{-# LANGUAGE DeriveGeneric     #-}
{-# LANGUAGE OverloadedStrings #-}

module Models.OrderItem where

import           Data.Aeson
import           Data.Aeson.Casing (aesonDrop, camelCase)
import           GHC.Generics      (Generic)
import           Models.Plant      (Plant)
import           Database.PostgreSQL.Simple.FromRow (FromRow(..), field)

-- | Modèle de base pour un article de commande.
data OrderItem = OrderItem
  { orderItemId        :: Int
  , orderItemOrderId   :: Int
  , orderItemPlantId   :: Int
  , orderItemQuantity  :: Int
  , orderItemPrice     :: Int -- Prix au moment de la commande
  } deriving (Show, Generic)

-- | Modèle complet incluant les détails de la plante.
data FullOrderItem = FullOrderItem
  { fullOrderItemId        :: Int
  , fullOrderItemOrderId   :: Int
  , fullOrderItemQuantity  :: Int
  , fullOrderItemPrice     :: Int
  , fullOrderItemPlant     :: Plant -- Objet Plant imbriqué
  } deriving (Show, Generic)

-- | Options Aeson pour le parsing JSON des OrderItem.
orderItemOptions :: Options
orderItemOptions = aesonDrop 9 camelCase -- FIX: Le préfixe est "orderItem" (9 lettres)

-- | Options Aeson pour le parsing JSON des FullOrderItem.
fullOrderItemOptions :: Options
fullOrderItemOptions = aesonDrop 13 camelCase -- FIX: Le préfixe est "fullOrderItem" (13 lettres)

instance ToJSON FullOrderItem where
  toJSON = genericToJSON fullOrderItemOptions

instance FromRow OrderItem where
  fromRow = OrderItem <$> field <*> field <*> field <*> field <*> field
