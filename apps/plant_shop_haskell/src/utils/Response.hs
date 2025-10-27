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

created :: ToJSON a => a -> ActionM ()
created = jsonResponse status201

badRequest :: String -> ActionM ()
badRequest = jsonError status400

unauthorized :: String -> ActionM ()
unauthorized = jsonError status401

forbidden :: String -> ActionM ()
forbidden = jsonError status403

notFound :: String -> ActionM ()
notFound = jsonError status404

serverError :: String -> ActionM ()
serverError = jsonError status500
