{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE RecordWildCards   #-}

module Controllers.UserController (routes) where

import           Control.Monad.IO.Class (liftIO)
import           Data.Aeson             (Value (..), fromJSON, object, (.=))
import qualified Data.Aeson.Types       as Aeson
import           Data.Maybe             (fromMaybe)
import           Database.PostgreSQL.Simple
import           Web.Scotty

import           Middleware.Auth        (requireAdmin, requireUser)
import           Models.User
import qualified Utils.Response         as R
import           Utils.Password         (hashPassword)

routes :: Connection -> ScottyM ()
routes conn = do
  -- GET /api/users et /api/admin/users (Admin)
  get "/api/users" $ requireAdmin >> listUsers
  get "/api/admin/users" $ requireAdmin >> listUsers

  -- POST /api/users (Admin)
  post "/api/users" $ do
    requireAdmin
    payload <- jsonData :: ActionM CreateUserPayload
    hashedPassword <- liftIO $ hashPassword (createUserPassword payload)
    [Only newId] <- liftIO $ query conn "INSERT INTO users (name, email, password_hash, is_admin) VALUES (?, ?, ?, ?) RETURNING id"
      (createUserName payload, createUserEmail payload, hashedPassword, fromMaybe False (createUserIsAdmin payload))
    users <- liftIO $ query conn "SELECT * FROM users WHERE id = ?" (Only (newId :: Int))
    case users of
      [user] -> R.created (toPublicUser user)
      _      -> R.serverError "Impossible de récupérer l'utilisateur après création."

  -- GET /api/users/:id
  get "/api/users/:id" $ do
    targetId <- param "id"
    user <- requireUser -- Récupère l'utilisateur authentifié
    -- Un admin peut voir n'importe qui, un utilisateur ne peut voir que lui-même.
    if userIsAdmin user || userId user == targetId
      then do
        targetUsers <- liftIO $ query conn "SELECT * FROM users WHERE id = ?" (Only (targetId :: Int))
        case targetUsers of
          [targetUser] -> R.ok (toPublicUser targetUser)
          _            -> R.notFound "Utilisateur non trouvé"
      else R.forbidden "Accès refusé"

  -- PATCH /api/users/:id
  patch "/api/users/:id" $ do
    targetId <- param "id"
    currentUser <- requireUser
    payload <- jsonData :: ActionM UpdateUserPayload

    targetUsers <- liftIO $ query conn "SELECT * FROM users WHERE id = ?" (Only (targetId :: Int))
    case targetUsers of
      [] -> R.notFound "Utilisateur non trouvé"
      [targetUser] -> do
        -- Vérifier les permissions
        if not (userIsAdmin currentUser) && userId currentUser /= targetId
        then R.forbidden "Vous ne pouvez pas modifier cet utilisateur."
        else do
          let updatedUser = applyUserPatch targetUser payload (userIsAdmin currentUser)
          _ <- liftIO $ execute conn "UPDATE users SET name = ?, email = ?, is_admin = ? WHERE id = ?"
                (userName updatedUser, userEmail updatedUser, userIsAdmin updatedUser, targetId)
          R.ok (toPublicUser updatedUser)

  -- DELETE /api/admin/users/:id (Admin)
  delete "/api/admin/users/:id" $ do
    requireAdmin
    targetId <- param "id"
    rowsAffected <- liftIO $ execute conn "DELETE FROM users WHERE id = ?" (Only (targetId :: Int))
    if rowsAffected > 0
      then status status200
      else R.notFound "Utilisateur non trouvé"

  where
    listUsers = do
      users <- liftIO $ query_ conn "SELECT * FROM users ORDER BY is_admin DESC, name ASC"
      R.ok (map toPublicUser (users :: [User]))

applyUserPatch :: User -> UpdateUserPayload -> Bool -> User
applyUserPatch user payload isAdminCaller =
  user {
    userName = fromMaybe (userName user) (updateUserName payload),
    userEmail = fromMaybe (userEmail user) (updateUserEmail payload),
    -- Seul un admin peut changer le statut admin
    userIsAdmin = if isAdminCaller then fromMaybe (userIsAdmin user) (updateUserIsAdmin payload) else userIsAdmin user
  }
