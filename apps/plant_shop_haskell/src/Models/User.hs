{-# LANGUAGE DeriveGeneric     #-}
{-# LANGUAGE OverloadedStrings #-}

module Models.User where

import           Data.Aeson
import           Data.Aeson.Casing (aesonDrop, camelCase)
import           Data.Text         (Text)
import           Data.Time         (UTCTime)
import           GHC.Generics      (Generic)

-- | Représentation complète de l'utilisateur, incluant le hash du mot de passe.
-- | Utilisé pour les opérations internes et la base de données.
data User = User
  { userId          :: Int
  , userName        :: Text
  , userEmail       :: Text
  , userPasswordHash :: Text
  , userIsAdmin     :: Bool
  , userCreatedAt   :: UTCTime
  } deriving (Show, Generic)

-- | Représentation publique de l'utilisateur, sans le mot de passe.
-- | C'est cette structure qui sera envoyée via l'API.
data PublicUser = PublicUser
  { publicUserId        :: Int
  , publicUserName      :: Text
  , publicUserEmail     :: Text
  , publicUserIsAdmin   :: Bool
  , publicUserCreatedAt :: UTCTime
  } deriving (Show, Generic)

-- | Convertit un User complet en PublicUser pour la sérialisation JSON.
toPublicUser :: User -> PublicUser
toPublicUser user = PublicUser
  { publicUserId        = userId user
  , publicUserName      = userName user
  , publicUserEmail     = userEmail user
  , publicUserIsAdmin   = userIsAdmin user
  , publicUserCreatedAt = userCreatedAt user
  }

-- | Options Aeson pour convertir les noms de champs en camelCase.
-- | Exemple : `publicUserName` devient `name` dans le JSON.
userOptions :: Options
userOptions = defaultOptions { fieldLabelModifier = camelCase . aesonDrop 10 }

instance ToJSON PublicUser where
  toJSON = genericToJSON userOptions

-- | DTO (Data Transfer Object) pour la création d'un utilisateur.
data CreateUserPayload = CreateUserPayload
  { createUserName     :: Text
  , createUserEmail    :: Text
  , createUserPassword :: Text
  , createUserIsAdmin  :: Maybe Bool -- Optionnel, pour la création par un admin
  } deriving (Show, Generic)

instance FromJSON CreateUserPayload where
  parseJSON = genericParseJSON defaultOptions { fieldLabelModifier = camelCase . aesonDrop 6 }

-- | DTO pour la mise à jour d'un utilisateur. Tous les champs sont optionnels.
data UpdateUserPayload = UpdateUserPayload
  { updateUserName  :: Maybe Text
  , updateUserEmail :: Maybe Text
  , updateUserIsAdmin :: Maybe Bool
  } deriving (Show, Generic)

instance FromJSON UpdateUserPayload where
  parseJSON = genericParseJSON defaultOptions { fieldLabelModifier = camelCase . aesonDrop 6 }
