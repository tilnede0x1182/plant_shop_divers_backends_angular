{-# LANGUAGE OverloadedStrings #-}

module Utils.JWT (createToken, getClaimsFromToken) where

import           Control.Monad.IO.Class (liftIO)
import           Data.Aeson             (ToJSON (..), object, (.=))
import qualified Data.Map               as Map
import           Data.Text              (Text, pack)
import           Data.Time.Clock        (addUTCTime, getCurrentTime)
import           Web.JWT                (JWTClaimsSet (..), NumericDate (..),
                                         StringOrURI, decodeAndVerifySignature,
                                         encodeSigned, hmacSecret, jwtAlg,
                                         numericDate)

import           Models.User            (User (..))

-- Clé secrète. En production, elle devrait provenir d'une variable d'environnement.
secretKey :: Text
secretKey = "your-dev-secret-key"

-- | Crée un token JWT pour un utilisateur.
createToken :: User -> IO Text
createToken user = do
  now <- liftIO getCurrentTime
  let claims = mempty -- Commence avec un ensemble de revendications vide
        { iss = stringOrURI "plant-shop-haskell"
        , iat = numericDate now
        , exp = numericDate (addUTCTime (24 * 60 * 60) now) -- Expire dans 24h
        , unregisteredClaims = Map.fromList
            [ ("id", toJSON (userId user))
            , ("email", toJSON (userEmail user))
            , ("name", toJSON (userName user))
            , ("admin", toJSON (userIsAdmin user))
            ]
        }
  return $ encodeSigned (hmacSecret secretKey) mempty claims

-- | Extrait les revendications (claims) d'un token.
getClaimsFromToken :: Text -> Maybe JWTClaimsSet
getClaimsFromToken token =
  decodeAndVerifySignature (hmacSecret secretKey) token

-- | Helper pour convertir une String en StringOrURI.
stringOrURI :: String -> Maybe StringOrURI
stringOrURI = Just . pack
