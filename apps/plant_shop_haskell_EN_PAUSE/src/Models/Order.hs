{-# LANGUAGE DeriveGeneric     #-}
{-# LANGUAGE OverloadedStrings #-}

module Models.Order where

import           Control.Applicative            ((<|>))
import           Data.Aeson                     (FromJSON (..), ToJSON (..),
                                                 genericParseJSON, object, (.=),
                                                 withObject, (.:?))
import           Data.Aeson.Casing              (aesonDrop, camelCase)
import           Data.Text                      (Text)
import           Data.Time                      (UTCTime)
import           GHC.Generics                   (Generic)
import           Models.OrderItem               (FullOrderItem)
import           Database.PostgreSQL.Simple.FromRow (FromRow (..), field)

-- | Modèle de base pour une commande, tel que stocké en DB.
data Order = Order
  { orderId        :: Int
  , orderUserId    :: Int
  , orderTotal     :: Double
  , orderStatus    :: Text
  , orderCreatedAt :: UTCTime
  } deriving (Show, Generic)

-- | Modèle complet pour une commande, incluant les articles.
-- | C'est cette structure qui est envoyée via l'API.
data FullOrder = FullOrder
  { fullOrderId        :: Int
  , fullOrderUserId    :: Int
  , fullOrderTotal     :: Double
  , fullOrderStatus    :: Text
  , fullOrderCreatedAt :: UTCTime
  , fullOrderItems     :: [FullOrderItem]
  } deriving (Show, Generic)

instance ToJSON FullOrder where
  toJSON fullOrder =
    object
      [ "id"         .= fullOrderId fullOrder
      , "userId"     .= fullOrderUserId fullOrder
      , "total"      .= fullOrderTotal fullOrder
      , "status"     .= fullOrderStatus fullOrder
      , "createdAt"  .= fullOrderCreatedAt fullOrder
      , "orderItems" .= fullOrderItems fullOrder
      ]

-- | DTO pour la création d'une commande.
data CreateOrderPayload = CreateOrderPayload
  { createOrderItems :: [CreateOrderItemPayload]
  } deriving (Show, Generic)

data CreateOrderItemPayload = CreateOrderItemPayload
  { orderItemPlantId  :: Int
  , orderItemQuantity :: Int
  } deriving (Show, Generic)

instance FromJSON CreateOrderItemPayload where
  parseJSON = genericParseJSON (aesonDrop 9 camelCase)

instance FromJSON CreateOrderPayload where
  parseJSON = genericParseJSON (aesonDrop 11 camelCase)

-- | DTO pour la mise à jour du statut d'une commande.
newtype UpdateOrderStatusPayload = UpdateOrderStatusPayload
  { updateOrderStatus :: Text
  } deriving (Show, Generic)

instance FromJSON UpdateOrderStatusPayload where
  parseJSON = withObject "UpdateOrderStatusPayload" $ \obj -> do
    status <- obj .:? "status" <|> obj .:? "orderStatus"
    maybe (fail "Champ 'status' manquant") (pure . UpdateOrderStatusPayload) status

instance FromRow Order where
  fromRow = Order <$> field <*> field <*> field <*> field <*> field
