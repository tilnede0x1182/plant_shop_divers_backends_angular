{-# LANGUAGE DeriveGeneric     #-}
{-# LANGUAGE OverloadedStrings #-}

module Models.Plant where

import           Data.Aeson
import           Data.Aeson.Casing (aesonDrop, camelCase)
import           Data.Text         (Text)
import           Data.Time         (UTCTime)
import           GHC.Generics      (Generic)

data Plant = Plant
  { plantId          :: Int
  , plantName        :: Text
  , plantDescription :: Maybe Text
  , plantPrice       :: Double
  , plantStock       :: Int
  , plantCreatedAt   :: UTCTime
  } deriving (Show, Generic)

plantOptions :: Options
plantOptions = defaultOptions { fieldLabelModifier = camelCase . aesonDrop 5 }

instance ToJSON Plant where
  toJSON = genericToJSON plantOptions

instance FromJSON Plant where
  parseJSON = genericParseJSON plantOptions
