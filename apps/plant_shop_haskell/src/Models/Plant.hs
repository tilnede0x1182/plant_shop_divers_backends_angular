{-# LANGUAGE DeriveGeneric     #-}
{-# LANGUAGE OverloadedStrings #-}

module Models.Plant where

import           Data.Aeson
import           Data.Aeson.Casing                (aesonDrop, camelCase)
import           Data.Text                        (Text)
import           Data.Time                        (UTCTime)
import           GHC.Generics                     (Generic)
import           Database.PostgreSQL.Simple.FromRow (FromRow (..), field)

data Plant = Plant
  { plantId          :: Int
  , plantName        :: Text
  , plantDescription :: Maybe Text
  , plantPrice       :: Int
  , plantStock       :: Int
  , plantCreatedAt   :: UTCTime
  } deriving (Show, Generic)

plantOptions :: Options
plantOptions = aesonDrop 5 camelCase

instance ToJSON Plant where
  toJSON = genericToJSON plantOptions

instance FromJSON Plant where
  parseJSON = genericParseJSON plantOptions

instance FromRow Plant where
  fromRow = Plant <$> field <*> field <*> field <*> field <*> field <*> field

-- | Payload JSON pour la création d'une plante (depuis l'admin).
data CreatePlantPayload = CreatePlantPayload
  { createPlantName        :: Text
  , createPlantDescription :: Maybe Text
  , createPlantPrice       :: Int
  , createPlantStock       :: Maybe Int
  } deriving (Show, Generic)

instance FromJSON CreatePlantPayload where
  parseJSON = genericParseJSON (aesonDrop 11 camelCase)
