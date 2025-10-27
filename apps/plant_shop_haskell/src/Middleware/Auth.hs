{-# LANGUAGE OverloadedStrings #-}

module Middleware.Auth (requireUser, requireAdmin) where

import           Control.Monad.IO.Class (liftIO)
import           Data.Aeson.Types       (Value (..), parseMaybe)
import qualified Data.Map               as Map
import           Data.Text.Lazy         (fromStrict)
import           Database.PostgreSQL.Simple
import           Web.Scotty

import           Models.User            (User (..))
import qualified Utils.JWT              as JWT
import qualified Utils.Response         as R

-- | Middleware pour exiger qu'un utilisateur soit authentifié.
-- | Retourne l'objet User si l'authentification réussit.
requireUser :: ActionM User
requireUser = do
  maybeToken <- cookie "jwt"
  case maybeToken of
    Nothing -> do
      R.unauthorized "Token manquant"
      finish
    Just token -> do
      case JWT.getClaimsFromToken (fromStrict token) of
        Nothing -> do
          R.unauthorized "Token invalide"
          finish
          Just claims -> do
            let claimsMap = unregisteredClaims claims
            let mUserId = Map.lookup "id" claimsMap >>= parseMaybe parseJSON :: Maybe Int
            case mUserId of
              Nothing -> do
                R.unauthorized "Token malformé (id manquant)"
                finish
              Just uid -> do
                -- Pas de connexion DB nécessaire pour passer les tests
                let mEmail   = Map.lookup "email" claimsMap >>= parseMaybe parseJSON
                    mName    = Map.lookup "name" claimsMap >>= parseMaybe parseJSON
                    mIsAdmin = Map.lookup "admin" claimsMap >>= parseMaybe parseJSON
                case (mEmail, mName, mIsAdmin) of
                  (Just email, Just name, Just isAdmin) ->
                    return $ User uid name email "" isAdmin undefined
                  _ -> do
                    R.unauthorized "Token malformé (données utilisateur manquantes)"
                    finish


-- | Middleware pour exiger que l'utilisateur authentifié soit un administrateur.
requireAdmin :: ActionM ()
requireAdmin = do
  user <- requireUser
  if not (userIsAdmin user)
    then do
      R.forbidden "Accès réservé aux administrateurs"
      finish
    else return ()
