module Controllers.OrderItemController (routes) where

import Web.Scotty
import Database.PostgreSQL.Simple

-- | Routes REST pour les items de commande (non implémenté).
-- La logique est gérée dans OrderController.
-- @param conn Connexion à la base de données PostgreSQL
routes :: Connection -> ScottyM ()
routes _ = return ()
