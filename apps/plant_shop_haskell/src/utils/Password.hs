{-# LANGUAGE OverloadedStrings #-}

module Utils.Password (hashPassword, validatePassword) where

import qualified Crypto.BCrypt         as Bcrypt
import           Data.ByteString.Char8 (pack, unpack)
import           Data.Text             (Text)
import qualified Data.Text.Encoding    as TE

-- | Hache un mot de passe en utilisant bcrypt.
hashPassword :: Text -> IO Text
hashPassword pwd = do
  let bsPwd = TE.encodeUtf8 pwd
  -- Utilise une politique de hachage rapide pour les tests,
  -- en production, on pourrait utiliser `Bcrypt.defaultBcryptHashingPolicy`.
  maybeHashed <- Bcrypt.hashPasswordUsingPolicy Bcrypt.fastBcryptHashingPolicy bsPwd
  case maybeHashed of
    Just hashed -> return $ TE.decodeUtf8 hashed
    Nothing     -> fail "Erreur lors du hachage du mot de passe"

-- | Valide un mot de passe en clair contre un hash.
validatePassword :: Text -> Text -> Bool
validatePassword plain hashed =
  Bcrypt.validatePassword (TE.encodeUtf8 plain) (TE.encodeUtf8 hashed)
