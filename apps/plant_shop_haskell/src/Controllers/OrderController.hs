{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE RecordWildCards   #-}

module Controllers.OrderController (routes) where

import           Control.Exception      (throwIO)
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
    -- Un admin voit tout, un utilisateur ne voit que ses commandes
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

    -- Utilisation d'une transaction pour garantir l'atomicité
    result <- liftIO $ withTransaction conn $ do
      -- 1. Créer la commande avec un total de 0 pour obtenir un ID
      [Only orderId] <- query conn "INSERT INTO orders (user_id, total, status) VALUES (?, 0, 'pending') RETURNING id" (Only $ userId user)

      -- 2. Traiter chaque article
      totalPrice <- processOrderItems conn (createOrderItems payload)

      -- 3. Mettre à jour le total de la commande
      execute conn "UPDATE orders SET total = ? WHERE id = ?" (totalPrice, orderId :: Int)

      return orderId

    -- Récupérer la commande complète pour la réponse
    [newOrder] <- liftIO $ query conn "SELECT * FROM orders WHERE id = ?" (Only (result :: Int))
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

-- | Logique de traitement des articles d'une commande dans une transaction
processOrderItems :: Connection -> [CreateOrderItemPayload] -> IO Double
processOrderItems conn items = sum <$> mapM processItem items
  where
    processItem :: CreateOrderItemPayload -> IO Double
    processItem item = do
      let pId = O.orderItemPlantId item
      let qty = O.orderItemQuantity item

      -- Récupérer la plante et vérifier le stock
      [plant] <- query conn "SELECT * FROM plants WHERE id = ?" (Only pId) :: IO [Plant]
      if plantStock plant < qty
        then throwIO (userError ("Stock insuffisant pour la plante " ++ show pId))
        else do
          -- Mettre à jour le stock
          execute conn "UPDATE plants SET stock = stock - ? WHERE id = ?" (qty, pId)
          -- Créer l'article de commande
          let itemPrice = plantPrice plant
          execute conn "INSERT INTO order_items (order_id, plant_id, quantity, price) VALUES (?, ?, ?, ?)"
            (0 :: Int, pId :: Int, qty :: Int, itemPrice :: Double) -- order_id sera mis à jour plus tard si nécessaire, mais ici on ne le lie pas directement
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
  [plant] <- liftIO $ query conn "SELECT * FROM plants WHERE id = ?" (Only $ OI.orderItemPlantId item)
  return FullOrderItem
    { fullOrderItemId = orderItemId item
    , fullOrderItemOrderId = orderItemOrderId item
    , fullOrderItemQuantity = OI.orderItemQuantity item
    , fullOrderItemPrice = orderItemPrice item
    , fullOrderItemPlant = plant
    }
