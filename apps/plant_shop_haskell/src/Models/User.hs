{-# LANGUAGE DeriveGeneric     #-}
{-# LANGUAGE OverloadedStrings #-}

module Models.User where

import           Control.Applicative            ((<|>))
import           Data.Aeson                     (FromJSON (..), ToJSON (..),
                                                 object, (.=), withObject, (.:),
                                                 (.:?))
import           Data.Text                      (Text)
import           Data.Time                      (UTCTime)
import           GHC.Generics                   (Generic)
import           Database.PostgreSQL.Simple.FromRow

-- | Représentation complète de l'utilisateur, incluant le hash du mot de passe.
data User = User
  { userId          :: Int
  , name        :: Text
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

-- | Convertit un User en PublicUser (sans le hash du mot de passe).
-- @param user L'utilisateur à convertir
toPublicUser :: User -> PublicUser
toPublicUser user = PublicUser
  { publicUserId      = userId user
  , publicUserName    = name user
  , publicUserEmail   = userEmail user
  , publicUserIsAdmin = userIsAdmin user
  }

instance ToJSON PublicUser where
  toJSON user =
    object
      [ "id"    .= publicUserId user
      , "name"  .= publicUserName user
      , "email" .= publicUserEmail user
      , "admin" .= publicUserIsAdmin user
      ]

-- | DTO pour la création d’un utilisateur.
data CreateUserPayload = CreateUserPayload
  { createUserName     :: Text
  , createUserEmail    :: Text
  , createUserPassword :: Text
  , createUserIsAdmin  :: Maybe Bool
  } deriving (Show, Generic)

instance FromJSON CreateUserPayload where
  parseJSON = withObject "CreateUserPayload" $ \obj ->
    CreateUserPayload
      <$> obj .: "name"
      <*> obj .: "email"
      <*> obj .: "password"
      <*> (obj .:? "admin" <|> obj .:? "isAdmin")

-- | DTO pour la mise à jour d’un utilisateur.
data UpdateUserPayload = UpdateUserPayload
  { updateUserName   :: Maybe Text
  , updateUserEmail  :: Maybe Text
  , updateUserIsAdmin :: Maybe Bool
  } deriving (Show, Generic)

instance FromJSON UpdateUserPayload where
  parseJSON = withObject "UpdateUserPayload" $ \obj ->
    UpdateUserPayload
      <$> obj .:? "name"
      <*> obj .:? "email"
      <*> (obj .:? "admin" <|> obj .:? "isAdmin")
