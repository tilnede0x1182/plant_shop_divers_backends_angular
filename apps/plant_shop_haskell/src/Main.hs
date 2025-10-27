{-# LANGUAGE OverloadedStrings #-}

import           Web.Scotty
import           Network.Wai.Middleware.RequestLogger (logStdoutDev)
import           Network.Wai.Middleware.Cors          (simpleCors)
import           Database.PostgreSQL.Simple           (connectPostgreSQL, Connection)
import           Control.Monad.IO.Class               (liftIO)
import           System.Environment                   (lookupEnv)
import           Text.Read                             (readMaybe)
import           Control.Exception                     (try, IOException)
import           Configuration.Dotenv                  (Config (..), loadFile, defaultConfig)

import qualified Config.Config as C
import qualified Controllers.AuthController as Auth
import qualified Controllers.PlantController as Plant
import qualified Controllers.UserController as User
import qualified Controllers.OrderController as Order

main :: IO ()
main = do
  -- Charger .env local d'abord, puis fallback monorepo
  do
    _ <- loadFile defaultConfig { configPath = ["./.env"],    configOverride = False }
    _ <- loadFile defaultConfig { configPath = ["../../.env"], configOverride = False }
    pure ()

  -- Connexion à la base
  connString <- C.loadDbConnectionString
  conn <- connectPostgreSQL connString
  putStrLn "🔧Connexion à la base de données réussie."

  -- Lecture du port
  maybePortStr <- lookupEnv "SERVER_ADDRESS"
  let port = maybe 4100 id (maybePortStr >>= readMaybe)

  putStrLn $ "🚀  Démarrage du serveur sur le port " ++ show port ++ "..."

  -- Gestion d'erreur si port occupé
  result <- try (scotty port $ app conn) :: IO (Either IOException ())
  case result of
    Left _  -> putStrLn $ "❌ Erreur : le port " ++ show port ++ " est déjà utilisé."
    Right _ -> return ()

-- Application Scotty
app :: Connection -> ScottyM ()
app conn = do
  middleware logStdoutDev
  middleware simpleCors
  Auth.routes conn
  Plant.routes conn
  User.routes conn
  Order.routes conn
  notFound $ json ("error" :: String, "Route non trouvée" :: String)
