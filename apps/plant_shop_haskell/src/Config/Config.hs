module Config.Config (loadDbConnectionString) where

import           Data.ByteString (ByteString)
import qualified Data.ByteString.Char8 as BS
import           System.Environment (lookupEnv)

-- | Charge la chaîne de connexion à la base de données depuis la variable d'environnement DATABASE_URL.
-- | Fournit une valeur par défaut si la variable n'est pas définie.
loadDbConnectionString :: IO ByteString
loadDbConnectionString = do
	maybeUrl <- lookupEnv "DATABASE_URL"
	case maybeUrl of
		Just url -> return (BS.pack url)
		Nothing -> error "DATABASE_URL non défini dans l'environnement"
