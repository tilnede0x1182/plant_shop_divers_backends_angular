{-# LANGUAGE DeriveGeneric     #-}
{-# LANGUAGE OverloadedStrings #-}

module Models.Plant where

import           Data.Aeson
import           Data.Aeson.Casing (aesonDrop, camelCase)
import           Data.Text         (Text)
import           Data.Time         (UTCTime)
import           GHC.Generics      (Generic)
import           Database.PostgreSQL.Simple.FromRow (FromRow(..), field)

data Plant = Plant
  { plantId          :: Int
  , plantName        :: Text
  , plantDescription :: Maybe Text
  , plantPrice       :: Double
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
