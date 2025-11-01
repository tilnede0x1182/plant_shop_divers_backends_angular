{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE RecordWildCards   #-}
{-# LANGUAGE TypeApplications  #-}

module Controllers.OrderController (routes) where

import           Control.Exception                 (SomeException, try)
import           Control.Monad.IO.Class            (liftIO)
import           Data.Maybe                        (catMaybes)
import           Database.PostgreSQL.Simple
import           Network.HTTP.Types.Status         (status200)
import           Web.Scotty
import           Models.User                      (User (..))
import qualified Utils.Response                  as R
import qualified Models.Order                     as O
import qualified Models.OrderItem                 as OI
import           Models.Order
import           Models.OrderItem
import           Models.Plant
import           Middleware.Auth                  (requireAdmin, requireUser)

orderSelectBase :: Query
orderSelectBase =
  "SELECT id, user_id, total::int AS total, status, created_at FROM orders"

orderSelectAdmin :: Query
orderSelectAdmin = orderSelectBase <> " ORDER BY created_at DESC"

orderSelectByUser :: Query
orderSelectByUser = orderSelectBase <> " WHERE user_id = ? ORDER BY created_at DESC"

orderSelectById :: Query
orderSelectById = orderSelectBase <> " WHERE id = ?"

orderItemsSelectByOrder :: Query
orderItemsSelectByOrder =
  "SELECT oi.id, oi.order_id, oi.plant_id, oi.quantity, oi.price::int AS price \
  \FROM order_items oi \
  \JOIN plants p ON p.id = oi.plant_id \
  \WHERE oi.order_id = ?"

routes :: Connection -> ScottyM ()
routes conn = do
  -- GET /api/orders
  get "/api/orders" $ do
    user <- requireUser
    -- Montrer uniquement les commandes appartenant à l'utilisateur courant.
    orders <- liftIO $ query conn orderSelectByUser (Only $ userId user)

    fullOrders <- liftIO $ mapM (fetchFullOrder conn) (orders :: [Order])
    R.ok fullOrders

  -- POST /api/orders
  post "/api/orders" $ do
    user <- requireUser
    payload <- jsonData :: ActionM CreateOrderPayload

    -- Utilisation de 'try' sur l'action IO, puis gestion du résultat dans ActionM
    orderIdResult <- liftIO $ try @SomeException $
      withTransaction conn $ do
        -- 1. Créer la commande avec un total de 0 pour obtenir un ID
        [Only orderId] <- query conn "INSERT INTO orders (user_id, total, status) VALUES (?, 0, 'pending') RETURNING id" (Only $ userId user)

        -- 2. Traiter chaque article et calculer le total
        totalPrice <- processOrderItems conn orderId (createOrderItems payload)

        -- 3. Mettre à jour le total de la commande
        execute conn "UPDATE orders SET total = ? WHERE id = ?" (totalPrice, orderId :: Int)

        return orderId

    case orderIdResult of
      Left e -> R.badRequest (show e) -- L'exception est maintenant gérée et renvoie un 400
      Right orderIdVal -> do
        orders <- liftIO $ query conn orderSelectById (Only (orderIdVal :: Int))
        case orders of
          [newOrder] -> do
            fullOrder <- liftIO $ fetchFullOrder conn newOrder
            R.created fullOrder
          _ -> R.serverError "Impossible de charger la commande après création."

  -- PATCH /api/orders/:id (Admin)
  patch "/api/orders/:id" $ do
    requireAdmin
    orderId <- captureParam "id"
    payload <- jsonData :: ActionM UpdateOrderStatusPayload
    rowsAffected <- liftIO $ execute conn "UPDATE orders SET status = ? WHERE id = ?" (updateOrderStatus payload, orderId :: Int)
    if rowsAffected > 0
      then do
        orders <- liftIO $ query conn orderSelectById (Only orderId)
        case orders of
          [updatedOrder] -> do
            fullOrder <- liftIO $ fetchFullOrder conn updatedOrder
            R.ok fullOrder
          _ -> R.serverError "Impossible de charger la commande après mise à jour."
      else R.notFound "Commande non trouvée"

  -- DELETE /api/orders/:id (Admin)
  delete "/api/orders/:id" $ do
    requireAdmin
    orderId <- captureParam "id"
    -- La suppression en cascade est gérée par la DB (ON DELETE CASCADE)
    rowsAffected <- liftIO $ execute conn "DELETE FROM orders WHERE id = ?" (Only (orderId :: Int))
    if rowsAffected > 0
      then status status200
      else R.notFound "Commande non trouvée"

-- Logique de traitement des articles, maintenant transactionnelle et correcte
processOrderItems :: Connection -> Int -> [CreateOrderItemPayload] -> IO Int
processOrderItems conn orderId items = sum <$> mapM (processItem conn orderId) items

processItem :: Connection -> Int -> CreateOrderItemPayload -> IO Int
processItem conn orderId item = do
  let pId = O.orderItemPlantId item
      qty = O.orderItemQuantity item

  -- Récupérer la plante et vérifier le stock
  mPlant <- listToMaybe <$> query conn plantSelectSql (Only pId)
  case mPlant of
    Nothing -> error ("Plante non trouvée: " ++ show pId)
    Just plant -> do
      if plantStock plant < qty
        then error ("Stock insuffisant pour la plante " ++ show pId)
        else do
          -- Mettre à jour le stock
          execute conn "UPDATE plants SET stock = stock - ? WHERE id = ?" (qty, pId)
          -- Créer l'article de commande
          let itemPrice = plantPrice plant
          execute conn "INSERT INTO order_items (order_id, plant_id, quantity, price) VALUES (?, ?, ?, ?)"
            (orderId, pId, qty, itemPrice)
          pure (itemPrice * qty)

-- | Récupère une commande et tous ses détails pour la sérialisation JSON
fetchFullOrder :: Connection -> Order -> IO FullOrder
fetchFullOrder conn order = do
  items <- liftIO $ query conn orderItemsSelectByOrder (Only $ orderId order)
  fullItems <- catMaybes <$> mapM (fetchFullOrderItem conn) (items :: [OrderItem])
  return FullOrder
    { fullOrderId = orderId order
    , fullOrderUserId = orderUserId order
    , fullOrderTotal = orderTotal order
    , fullOrderStatus = orderStatus order
    , fullOrderCreatedAt = orderCreatedAt order
    , fullOrderItems = fullItems
    }

fetchFullOrderItem :: Connection -> OrderItem -> IO (Maybe FullOrderItem)
fetchFullOrderItem conn item = do
  plants <- liftIO $ query conn plantSelectSql (Only $ OI.orderItemPlantId item)
  case plants of
    [plant] ->
      return $ Just FullOrderItem
        { fullOrderItemId = orderItemId item
        , fullOrderItemOrderId = orderItemOrderId item
        , fullOrderItemQuantity = OI.orderItemQuantity item
        , fullOrderItemPrice = orderItemPrice item
        , fullOrderItemPlant = plant
        }
    _ -> return Nothing

-- Helper pour extraire une valeur Maybe d'une liste (plus sûr que `head`)
listToMaybe :: [a] -> Maybe a
listToMaybe []    = Nothing
listToMaybe (x:_) = Just x

plantSelectSql :: Query
plantSelectSql =
  "SELECT id, name, description, price::int AS price, stock, created_at FROM plants WHERE id = ?"
