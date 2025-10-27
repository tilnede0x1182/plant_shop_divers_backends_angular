{-# LANGUAGE OverloadedStrings #-}

module Utils.JWT (createToken, getClaimsFromToken) where

import           Data.Aeson             (ToJSON (..))
import qualified Data.Map               as Map
import           Data.Text              (Text, pack)
import           Data.Time.Clock        (addUTCTime, getCurrentTime)
import           Data.Time.Clock.POSIX  (utcTimeToPOSIXSeconds)
import           Models.User            (User (..))
import           System.Environment     (lookupEnv)
import           System.IO.Unsafe       (unsafePerformIO)
import           Configuration.Dotenv   (loadFile, defaultConfig, Config(..))
import           Web.JWT                ( JWTClaimsSet (..)
                                       , ClaimsMap (ClaimsMap)
                                       , decodeAndVerifySignature
                                       , encodeSigned
                                       , hmacSecret
                                       , numericDate
                                       , toVerify
                                       , tokenIssuer
                                       )
import qualified Web.JWT                as JWT

{-|
Lecture robuste de JWT_SECRET.
Charge .env locaux si la variable n'est pas déjà présente.
Priorité : "./.env", puis "../../.env".
-}
{-# NOINLINE secretKey #-}
secretKey :: IO Text
secretKey = do
  _ <- loadFile defaultConfig { configPath = ["./.env"], configOverride = False }
  _ <- loadFile defaultConfig { configPath = ["../../.env"], configOverride = False }
  val <- lookupEnv "JWT_SECRET"
  case val of
    Just k  -> pure (pack k)
    Nothing -> fail "JWT_SECRET non défini dans .env"

-- Création d'un JWT HS256 valable 24h, compatible front + tests
createToken :: User -> IO Text
createToken user = do
  key <- secretKey
  putStrLn "🧩 [DEBUG] JWT_SECRET chargé :"
  print key
  now <- getCurrentTime
  let expAt  = addUTCTime (24 * 60 * 60) now
      claims = mempty
        { iss = tokenIssuer "plant-shop-haskell"
        , iat = numericDate (utcTimeToPOSIXSeconds now)
        , JWT.exp = numericDate (utcTimeToPOSIXSeconds expAt)
        , unregisteredClaims = ClaimsMap (Map.fromList
            [ ("id",    toJSON (userId user))
            , ("email", toJSON (userEmail user))
            , ("name",  toJSON (userName user))
            , ("admin", toJSON (userIsAdmin user))
            ])
        }
  pure $ encodeSigned (hmacSecret key) mempty claims

-- Vérification + extraction des claims
getClaimsFromToken :: Text -> Maybe JWTClaimsSet
getClaimsFromToken tok = do
  let key = unsafePerformIO secretKey
  jwt <- decodeAndVerifySignature (toVerify (hmacSecret key)) tok
  pure (JWT.claims jwt)
