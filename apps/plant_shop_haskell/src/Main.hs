{-# LANGUAGE OverloadedStrings #-}

import           Web.Scotty
import           Network.Wai.Middleware.RequestLogger (logStdoutDev)
import           Network.Wai.Middleware.Cors          (simpleCors)
import           Database.PostgreSQL.Simple           (connectPostgreSQL, Connection)
import           Control.Monad.IO.Class               (liftIO)

import qualified Config.Config as C
import qualified Controllers.AuthController as Auth
import qualified Controllers.PlantController as Plant
import qualified Controllers.UserController as User
import qualified Controllers.OrderController as Order

main :: IO ()
main = do
  -- Charger la configuration et se connecter à la base de données
  connString <- C.loadDbConnectionString
  conn <- connectPostgreSQL connString
  putStrLn "🚀 Connexion à la base de données réussie."

  -- Démarrer le serveur Scotty
  scotty 4100 $ do
    -- Middlewares
    middleware logStdoutDev -- Pour le logging des requêtes
    middleware simpleCors   -- Pour la politique CORS (très permissif pour le dev)

    -- Montage des routes de chaque contrôleur
    Auth.routes conn
    Plant.routes conn
    User.routes conn
    Order.routes conn

    -- Route par défaut pour les chemins non trouvés
    notFound $ json ("error" :: String, "Route non trouvée" :: String)
