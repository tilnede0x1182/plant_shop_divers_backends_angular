{-# LANGUAGE DeriveGeneric     #-}
{-# LANGUAGE OverloadedStrings #-}

module Models.User where

import           Data.Aeson
import           Data.Aeson.Casing (aesonDrop, camelCase)
import           Data.Text         (Text)
import           Data.Time         (UTCTime)
import           GHC.Generics      (Generic)
import           Database.PostgreSQL.Simple.FromRow
import           Data.Maybe        (fromMaybe)

-- | Représentation complète de l'utilisateur, incluant le hash du mot de passe.
data User = User
  { userId          :: Int
  , userName        :: Text
  , userEmail       :: Text
  , userPasswordHash :: Text
  , userIsAdmin     :: Bool
  }

instance FromRow User where
  fromRow = do
    uid      <- field  -- id
    email    <- field  -- email
    name     <- field  -- name
    pwdHash  <- field  -- password_hash
    isAdmin  <- field  -- is_admin
    _created <- field :: RowParser UTCTime  -- created_at (ignoré)
    return (User uid name email pwdHash isAdmin)

-- | Représentation publique envoyée par l’API.
data PublicUser = PublicUser
  { publicUserId      :: Int
  , publicUserName    :: Text
  , publicUserEmail   :: Text
  , publicUserIsAdmin :: Bool
  } deriving (Show, Generic)

toPublicUser :: User -> PublicUser
toPublicUser user = PublicUser
  { publicUserId      = userId user
  , publicUserName    = userName user
  , publicUserEmail   = userEmail user
  , publicUserIsAdmin = userIsAdmin user
  }

-- | Options Aeson pour conversion camelCase.
userOptions :: Options
userOptions = aesonDrop 10 camelCase

instance ToJSON PublicUser where
  toJSON = genericToJSON userOptions

-- | DTO pour la création d’un utilisateur.
data CreateUserPayload = CreateUserPayload
  { createUserName     :: Text
  , createUserEmail    :: Text
  , createUserPassword :: Text
  , createUserIsAdmin  :: Maybe Bool
  } deriving (Show, Generic)

instance FromJSON CreateUserPayload where
  parseJSON = genericParseJSON (aesonDrop 6 camelCase)

-- | DTO pour la mise à jour d’un utilisateur.
data UpdateUserPayload = UpdateUserPayload
  { updateUserName   :: Maybe Text
  , updateUserEmail  :: Maybe Text
  , updateUserIsAdmin :: Maybe Bool
  } deriving (Show, Generic)

instance FromJSON UpdateUserPayload where
  parseJSON = genericParseJSON (aesonDrop 6 camelCase)
