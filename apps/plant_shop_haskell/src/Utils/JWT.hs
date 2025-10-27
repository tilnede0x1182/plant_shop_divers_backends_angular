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

-- Clé lue depuis l'environnement (.env -> JWT_SECRET), chargée une fois.
{-# NOINLINE secretKey #-}
secretKey :: Text
secretKey =
  case unsafePerformIO (lookupEnv "JWT_SECRET") of
    Just k  -> pack k
    Nothing -> error "JWT_SECRET non défini dans l'environnement"

-- Création d'un JWT HS256 valable 24h, compatible front + tests
createToken :: User -> IO Text
createToken user = do
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
  -- encodeSigned choisit HS256 automatiquement pour une clé HMAC, header = mempty
  pure $ encodeSigned (hmacSecret secretKey) mempty claims

-- Vérification + extraction des claims
getClaimsFromToken :: Text -> Maybe JWTClaimsSet
getClaimsFromToken tok = do
  jwt <- decodeAndVerifySignature (toVerify (hmacSecret secretKey)) tok
  pure (JWT.claims jwt)
