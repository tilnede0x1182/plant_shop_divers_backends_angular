{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE RecordWildCards   #-}
{-# LANGUAGE TypeApplications  #-}

module Controllers.OrderController (routes) where

import           Control.Exception      (SomeException, try)
import           Control.Monad.IO.Class     (liftIO)
import qualified Data.Aeson                 as Aeson
import           Data.Maybe                 (fromMaybe)
import           Database.PostgreSQL.Simple
import           Database.PostgreSQL.Simple.FromRow (FromRow)
import           Network.HTTP.Types.Status (status200)
import           Web.Scotty
import           Models.User                 (User(..))
import qualified Utils.Response             as R
import qualified Models.Order as O
import qualified Models.OrderItem as OI
import qualified Models.Plant as P
import           Models.Order
import           Models.OrderItem
import           Models.Plant
import           Middleware.Auth        (requireUser, requireAdmin)

routes :: Connection -> ScottyM ()
routes conn = do
  -- GET /api/orders
  get "/api/orders" $ do
    user <- requireUser
    let queryStr = if userIsAdmin user
          then "SELECT * FROM orders ORDER BY created_at DESC"
          else "SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC"

    orders <- if userIsAdmin user
      then liftIO $ query_ conn queryStr
      else liftIO $ query conn queryStr (Only $ userId user)

    fullOrders <- liftIO $ mapM (fetchFullOrder conn) (orders :: [Order])
    R.ok fullOrders

  -- POST /api/orders
  post "/api/orders" $ do
    user <- requireUser
    payload <- jsonData :: ActionM CreateOrderPayload

    -- Utilisation de 'try' sur l'action IO, puis gestion du résultat dans ActionM
    orderIdResult <- liftIO $ Control.Exception.try @SomeException $
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
        [newOrder] <- liftIO $ query conn "SELECT * FROM orders WHERE id = ?" (Only (orderIdVal :: Int))
        fullOrder <- liftIO $ fetchFullOrder conn newOrder
        R.created fullOrder

  -- PATCH /api/orders/:id (Admin)
  patch "/api/orders/:id" $ do
    requireAdmin
    orderId <- param "id"
    payload <- jsonData :: ActionM UpdateOrderStatusPayload
    rowsAffected <- liftIO $ execute conn "UPDATE orders SET status = ? WHERE id = ?" (updateOrderStatus payload, orderId :: Int)
    if rowsAffected > 0
      then do
        [updatedOrder] <- liftIO $ query conn "SELECT * FROM orders WHERE id = ?" (Only orderId)
        fullOrder <- liftIO $ fetchFullOrder conn updatedOrder
        R.ok fullOrder
      else R.notFound "Commande non trouvée"

  -- DELETE /api/orders/:id (Admin)
  delete "/api/orders/:id" $ do
    requireAdmin
    orderId <- param "id"
    -- La suppression en cascade est gérée par la DB (ON DELETE CASCADE)
    rowsAffected <- liftIO $ execute conn "DELETE FROM orders WHERE id = ?" (Only (orderId :: Int))
    if rowsAffected > 0
      then status status200
      else R.notFound "Commande non trouvée"

-- Logique de traitement des articles, maintenant transactionnelle et correcte
processOrderItems :: Connection -> Int -> [CreateOrderItemPayload] -> IO Double
processOrderItems conn orderId items = sum <$> mapM (processItem conn orderId) items

processItem :: Connection -> Int -> CreateOrderItemPayload -> IO Double
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
          return (itemPrice * fromIntegral qty)

-- | Récupère une commande et tous ses détails pour la sérialisation JSON
fetchFullOrder :: Connection -> Order -> IO FullOrder
fetchFullOrder conn order = do
  items <- liftIO $ query conn "SELECT * FROM order_items WHERE order_id = ?" (Only $ orderId order)
  fullItems <- mapM (fetchFullOrderItem conn) (items :: [OrderItem])
  return FullOrder
    { fullOrderId = orderId order
    , fullOrderUserId = orderUserId order
    , fullOrderTotal = orderTotal order
    , fullOrderStatus = orderStatus order
    , fullOrderCreatedAt = orderCreatedAt order
    , fullOrderItems = fullItems
    }

fetchFullOrderItem :: Connection -> OrderItem -> IO FullOrderItem
fetchFullOrderItem conn item = do
  [plant] <- liftIO $ query conn plantSelectSql (Only $ OI.orderItemPlantId item)
  return FullOrderItem
    { fullOrderItemId = orderItemId item
    , fullOrderItemOrderId = orderItemOrderId item
    , fullOrderItemQuantity = OI.orderItemQuantity item
    , fullOrderItemPrice = orderItemPrice item
    , fullOrderItemPlant = plant
    }

-- Helper pour extraire une valeur Maybe d'une liste (plus sûr que `head`)
listToMaybe :: [a] -> Maybe a
listToMaybe []    = Nothing
listToMaybe (x:_) = Just x

plantSelectSql :: Query
plantSelectSql =
  "SELECT id, name, description, price::float8 AS price, stock, created_at FROM plants WHERE id = ?"
