{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE RecordWildCards   #-}

module Controllers.AuthController (routes) where

import           Control.Monad.IO.Class (liftIO)
import           Data.Aeson             (object, (.=))
import qualified Data.Text.Lazy         as TL
import           Database.PostgreSQL.Simple
import           Network.Wai.Middleware.RequestLogger (logStdoutDev)
import           Web.Scotty

import           Models.User
import           Utils.JWT              (createToken)
import           Utils.Password         (hashPassword, validatePassword)
import qualified Utils.Response         as R

routes :: Connection -> ScottyM ()
routes conn = do
  -- POST /api/auth/register
  post "/api/auth/register" $ do
    payload <- jsonData :: ActionM CreateUserPayload
    -- Vérifier si l'utilisateur existe déjà
    existing <- liftIO $ query conn "SELECT * FROM users WHERE email = ?" (Only $ createUserEmail payload) :: ActionM [User]
    if not (null existing)
      then R.badRequest "Cet email est déjà utilisé."
      else do
        hashedPassword <- liftIO $ hashPassword (createUserPassword payload)
        -- Les admins ne peuvent être créés que par d'autres admins, pas à l'inscription.
        _ <- liftIO $ execute conn "INSERT INTO users (name, email, password_hash, is_admin) VALUES (?, ?, ?, ?)"
              (createUserName payload, createUserEmail payload, hashedPassword, False :: Bool)
        R.created (object ["message" .= ("Utilisateur créé avec succès." :: String)])

  -- POST /api/auth/login
  post "/api/auth/login" $ do
    payload <- jsonData :: ActionM (Map.Map String String)
    let mEmail = Map.lookup "email" payload
    let mPassword = Map.lookup "password" payload
    case (mEmail, mPassword) of
      (Just email, Just password) -> do
        -- Récupérer l'utilisateur avec son hash de mot de passe
        users <- liftIO $ query conn "SELECT * FROM users WHERE email = ?" (Only email) :: ActionM [User]
        case users of
          [user] ->
            if validatePassword (pack password) (userPasswordHash user)
              then do
                token <- liftIO $ createToken user
                setCookie $ defaultCookie { setCookieName = "jwt", setCookieValue = encodeUtf8 token }
                R.created (toPublicUser user)
              else R.unauthorized "Email ou mot de passe incorrect."
          _ -> R.unauthorized "Email ou mot de passe incorrect."
      _ -> R.badRequest "Les champs 'email' et 'password' sont requis."

  -- POST /api/auth/logout
  post "/api/auth/logout" $ do
    -- Efface le cookie en le faisant expirer immédiatement
    setCookie $ defaultCookie { setCookieName = "jwt", setCookieValue = "", setCookieExpires = Just (UTCTime (ModifiedJulianDay 0) 0) }
    R.ok (object ["message" .= ("Déconnecté" :: String)])

  -- GET /api/auth/me
  get "/api/auth/me" $ do
    user <- req `fmap` liftAndCatchIO
    case user of
      Just u  -> R.ok (toPublicUser u)
      Nothing -> R.unauthorized "Non authentifié"
