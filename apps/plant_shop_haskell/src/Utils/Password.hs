{-# LANGUAGE PackageImports #-}
{-# LANGUAGE OverloadedStrings #-}

module Utils.Password (hashPassword, validatePassword) where

import qualified "cryptonite" Crypto.KDF.Argon2 as Argon2
import qualified Data.ByteArray.Encoding as BAE
import qualified Data.ByteString as BS
import qualified Data.Text as T
import qualified Data.Text.Encoding as TE
import           Data.Text (Text)
import           System.IO.Unsafe (unsafePerformIO)
import           Control.Monad (replicateM)
import           System.Random (randomIO)
import           Crypto.Error (CryptoFailable(..))

-- | Hache un mot de passe avec Argon2id, sel aléatoire 16 octets.
hashPassword :: Text -> IO Text
hashPassword pwd = do
    salt <- BS.pack <$> replicateM 16 randomIO
    let opts = Argon2.defaultOptions
            { Argon2.iterations = 2
            , Argon2.memory = 65536
            , Argon2.parallelism = 1
            , Argon2.variant = Argon2.Argon2id
            }
        pwdBs = TE.encodeUtf8 pwd
        CryptoPassed hash' = Argon2.hash opts pwdBs salt 32
        hash :: BS.ByteString
        hash = hash'
        encoded = BAE.convertToBase BAE.Base64 hash
        encodedSalt = BAE.convertToBase BAE.Base64 salt
    pure (TE.decodeUtf8 (encodedSalt <> ":" <> encoded))

-- | Vérifie un mot de passe en clair contre un hash Argon2id.
validatePassword :: Text -> Text -> Bool
validatePassword plain fullHash =
    unsafePerformIO $ do
        let parts = T.splitOn ":" fullHash
        if length parts /= 2
            then pure False
            else do
                let [saltB64, hashB64] = parts
                let decodedSalt = BAE.convertFromBase BAE.Base64 (TE.encodeUtf8 saltB64)
                        :: Either String BS.ByteString
                let decodedHash = BAE.convertFromBase BAE.Base64 (TE.encodeUtf8 hashB64)
                        :: Either String BS.ByteString
                case (decodedSalt, decodedHash) of
                    (Right salt, Right h) -> do
                        let opts = Argon2.defaultOptions
                                { Argon2.iterations = 2
                                , Argon2.memory = 65536
                                , Argon2.parallelism = 1
                                , Argon2.variant = Argon2.Argon2id
                                }
                            pwdBs = TE.encodeUtf8 plain
                            CryptoPassed computed' = Argon2.hash opts pwdBs salt 32
                            computed :: BS.ByteString
                            computed = computed'
                        pure (computed == h)
                    _ -> pure False
