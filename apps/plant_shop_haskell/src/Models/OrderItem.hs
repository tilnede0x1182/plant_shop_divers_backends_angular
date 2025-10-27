{-# LANGUAGE DeriveGeneric     #-}
{-# LANGUAGE OverloadedStrings #-}

module Models.OrderItem where

import           Data.Aeson
import           Data.Aeson.Casing (aesonDrop, camelCase)
import           GHC.Generics      (Generic)
import           Models.Plant      (Plant)

-- | Modèle de base pour un article de commande.
data OrderItem = OrderItem
  { orderItemId        :: Int
  , orderItemOrderId   :: Int
  , orderItemPlantId   :: Int
  , orderItemQuantity  :: Int
  , orderItemPrice     :: Double -- Prix au moment de la commande
  } deriving (Show, Generic)

-- | Modèle complet incluant les détails de la plante.
data FullOrderItem = FullOrderItem
  { fullOrderItemId        :: Int
  , fullOrderItemOrderId   :: Int
  , fullOrderItemQuantity  :: Int
  , fullOrderItemPrice     :: Double
  , fullOrderItemPlant     :: Plant -- Objet Plant imbriqué
  } deriving (Show, Generic)

orderItemOptions :: Options
orderItemOptions = defaultOptions { fieldLabelModifier = camelCase . aesonDrop 9 }

fullOrderItemOptions :: Options
fullOrderItemOptions = defaultOptions { fieldLabelModifier = camelCase . aesonDrop 4 }

instance ToJSON FullOrderItem where
  toJSON = genericToJSON fullOrderItemOptions
