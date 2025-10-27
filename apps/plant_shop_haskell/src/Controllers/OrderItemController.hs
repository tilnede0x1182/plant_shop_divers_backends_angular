module Controllers.OrderItemController (routes) where

import Web.Scotty
import Database.PostgreSQL.Simple

-- Ce contrôleur n'est pas requis par les tests e2e,
-- la logique de création des items est dans OrderController.
routes :: Connection -> ScottyM ()
routes _ = return ()
