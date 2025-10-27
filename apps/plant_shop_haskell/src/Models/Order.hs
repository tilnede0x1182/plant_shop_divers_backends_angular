{-# LANGUAGE DeriveGeneric     #-}
{-# LANGUAGE OverloadedStrings #-}

module Models.Order where

import           Data.Aeson
import           Data.Aeson.Casing (aesonDrop, camelCase)
import           Data.Text         (Text)
import           Data.Time         (UTCTime)
import           GHC.Generics      (Generic)
import           Models.OrderItem  (FullOrderItem)

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

orderOptions :: Options
orderOptions = defaultOptions { fieldLabelModifier = camelCase . aesonDrop 4 }

fullOrderOptions :: Options
fullOrderOptions = defaultOptions { fieldLabelModifier = camelCase . aesonDrop 4 }

instance ToJSON FullOrder where
  toJSON = genericToJSON fullOrderOptions

-- | DTO pour la création d'une commande.
data CreateOrderPayload = CreateOrderPayload
  { createOrderItems :: [CreateOrderItemPayload]
  } deriving (Show, Generic)

data CreateOrderItemPayload = CreateOrderItemPayload
  { orderItemPlantId  :: Int
  , orderItemQuantity :: Int
  } deriving (Show, Generic)

instance FromJSON CreateOrderItemPayload where
  parseJSON = genericParseJSON defaultOptions { fieldLabelModifier = camelCase . aesonDrop 9 }

instance FromJSON CreateOrderPayload where
  parseJSON = genericParseJSON defaultOptions { fieldLabelModifier = camelCase . aesonDrop 11 }

-- | DTO pour la mise à jour du statut d'une commande.
newtype UpdateOrderStatusPayload = UpdateOrderStatusPayload
  { updateOrderStatus :: Text
  } deriving (Show, Generic)

instance FromJSON UpdateOrderStatusPayload where
  parseJSON = genericParseJSON defaultOptions { fieldLabelModifier = camelCase . aesonDrop 6 }
