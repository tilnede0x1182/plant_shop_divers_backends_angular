module Config.Config (loadDbConnectionString) where

import           Data.ByteString (ByteString)
import qualified Data.ByteString.Char8 as BS
import           System.Environment (lookupEnv)
import           Configuration.Dotenv (loadFile, defaultConfig, Config(..))

-- | Charge la chaîne de connexion à la base de données depuis .env ou l'environnement.
-- | Charge d'abord le fichier .env deux niveaux au-dessus du dossier src.
loadDbConnectionString :: IO ByteString
loadDbConnectionString = do
	loadFile defaultConfig { configPath = ["../../.env"] }
	maybeUrl <- lookupEnv "DATABASE_URL"
	case maybeUrl of
		Just url -> return (BS.pack url)
		Nothing -> error "DATABASE_URL non défini ni dans .env ni dans l'environnement"
