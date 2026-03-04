{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE RecordWildCards   #-}

module Controllers.AuthController (routes) where

import           Control.Monad.IO.Class (liftIO)
import qualified Data.ByteString.Lazy          as BL
import           Data.ByteString.Builder       (toLazyByteString)
import           Data.Maybe                    (fromMaybe)
import           Data.Aeson             (object, (.=))
import qualified Data.Map               as Map
import qualified Data.Text              as T
import qualified Data.Text.Encoding     as TE
import qualified Data.Text.Lazy         as TL
import           Data.Time.Calendar     (Day(..))
import           Data.Time.Clock        (UTCTime(..))
import           Database.PostgreSQL.Simple
import           Web.Cookie             (defaultSetCookie, renderSetCookie,
                                         SetCookie (..))
import           Network.Wai.Middleware.RequestLogger (logStdoutDev)
import           Web.Scotty
import           Web.Scotty.Cookie      (getCookie)
import           Data.Aeson.Types       (parseJSON, parseMaybe)
import           Web.JWT                (unregisteredClaims, ClaimsMap(..))

import           Models.User
import qualified Utils.JWT              as JWT
import           Utils.JWT              (createToken)
import           Utils.Password         (hashPassword, validatePassword)
import qualified Utils.Response         as R

-- | Routes REST pour l'authentification.
-- @param conn Connexion à la base de données PostgreSQL
routes :: Connection -> ScottyM ()
routes conn = do
  -- POST /api/auth/register
  post "/api/auth/register" $ do
    payload <- jsonData :: ActionM CreateUserPayload
    existing <- liftIO $
      query conn "SELECT * FROM users WHERE email = ?" (Only $ createUserEmail payload)
    if not (null (existing :: [User]))
      then R.badRequest "Cet email est déjà utilisé."
      else do
        hashedPassword <- liftIO $ hashPassword (createUserPassword payload)
        _ <- liftIO $
          execute conn "INSERT INTO users (name, email, password_hash, is_admin) VALUES (?, ?, ?, ?)"
            (createUserName payload, createUserEmail payload, hashedPassword, False :: Bool)
        R.created (object ["message" .= ("Utilisateur créé avec succès." :: String)])

  -- POST /api/auth/login
  post "/api/auth/login" $ do
    payload <- jsonData :: ActionM (Map.Map String String)
    let mEmail    = Map.lookup "email" payload
        mPassword = Map.lookup "password" payload
    case (mEmail, mPassword) of
      (Just email, Just password) -> do
        users <- liftIO $
          query conn "SELECT * FROM users WHERE email = ?" (Only email)
        case users of
          [user] -> do
            -- CHANGÉ : Appel à la version IO de validatePassword
            isValid <- liftIO $ validatePassword (T.pack password) (userPasswordHash user)
            if isValid
              then do
                token <- liftIO $ createToken user
                let sc = defaultSetCookie
                      { setCookieName     = "jwt"
                      , setCookieValue    = TE.encodeUtf8 token
                      , setCookiePath     = Just "/"
                      , setCookieHttpOnly = True
                      }
                setHeader "Set-Cookie"
                  (TL.fromStrict (TE.decodeUtf8 (BL.toStrict (toLazyByteString (renderSetCookie sc)))))
                R.created (toPublicUser user)
              else R.unauthorized "Email ou mot de passe incorrect."
          _ -> R.unauthorized "Email ou mot de passe incorrect."
      _ -> R.badRequest "Les champs 'email' et 'password' sont requis."

  -- POST /api/auth/logout
  post "/api/auth/logout" $ do
    let sc = defaultSetCookie
          { setCookieName     = "jwt"
          , setCookieValue    = ""
          , setCookiePath     = Just "/"
          , setCookieHttpOnly = True
          , setCookieMaxAge   = Just 0
          }
    setHeader "Set-Cookie"
      (TL.fromStrict (TE.decodeUtf8 (BL.toStrict (toLazyByteString (renderSetCookie sc)))))
    R.ok (object ["message" .= ("Déconnecté" :: String)])

  -- GET /api/auth/me
  get "/api/auth/me" $ do
    maybeToken <- getCookie "jwt"
    case maybeToken >>= (JWT.getClaimsFromToken . TL.toStrict . TL.fromStrict) of
      Just claims -> do
        let (ClaimsMap claimsMap) = unregisteredClaims claims
            mId    = Map.lookup "id" claimsMap    >>= parseMaybe parseJSON
            mEmail = Map.lookup "email" claimsMap >>= parseMaybe parseJSON
            mName  = Map.lookup "name" claimsMap  >>= parseMaybe parseJSON
            mAdmin = Map.lookup "admin" claimsMap >>= parseMaybe parseJSON
            user =
              User
                { userId           = fromMaybe 0 mId
                , name         = fromMaybe "" mName
                , userEmail        = fromMaybe "" mEmail
                , userPasswordHash = ""
                , userIsAdmin      = fromMaybe False mAdmin
                }
        R.ok (toPublicUser user)
      Nothing -> R.unauthorized "Non authentifié"
