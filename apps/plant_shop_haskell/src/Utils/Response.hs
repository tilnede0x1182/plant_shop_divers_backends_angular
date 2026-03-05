{-# LANGUAGE OverloadedStrings #-}

module Utils.Response where

import           Data.Aeson         (ToJSON, object, (.=))
import           Web.Scotty         (ActionM, json, status)
import           Network.HTTP.Types (Status, status200, status201, status400,
                                     status401, status403, status404, status500)

-- | Envoie une réponse JSON avec un statut donné.
jsonResponse :: ToJSON a => Status -> a -> ActionM ()
jsonResponse s body = status s >> json body

-- | Envoie un message d'erreur JSON avec un statut.
jsonError :: Status -> String -> ActionM ()
jsonError s msg = status s >> json (object ["error" .= msg])

-- | Helpers pour les statuts HTTP courants.
ok :: ToJSON a => a -> ActionM ()
ok = jsonResponse status200

-- | Envoie une réponse 201 Created avec le body JSON.
created :: ToJSON a => a -> ActionM ()
created = jsonResponse status201

-- | Envoie une erreur 400 Bad Request.
badRequest :: String -> ActionM ()
badRequest = jsonError status400

-- | Envoie une erreur 401 Unauthorized.
unauthorized :: String -> ActionM ()
unauthorized = jsonError status401

forbidden :: String -> ActionM ()
forbidden = jsonError status403

notFound :: String -> ActionM ()
notFound = jsonError status404

serverError :: String -> ActionM ()
serverError = jsonError status500
