{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE RecordWildCards   #-}

module Controllers.PlantController (routes) where

import           Control.Monad.IO.Class (liftIO)
import           Data.Aeson             (Value (..), Object, FromJSON, object, (.=), (.:?), withObject)
import           Data.Aeson.Key         (fromText)
import           Data.Aeson.Types       (parseMaybe)
import qualified Data.Text              as T
import           Data.Maybe             (fromMaybe)
import           Network.HTTP.Types.Status (status200)
import           Database.PostgreSQL.Simple
import           Web.Scotty
import           Middleware.Auth        (requireAdmin, requireUser)
import           Models.Plant
import           Models.User            (User (..))
import qualified Utils.Response         as R
import           Data.Text (Text)
import           Data.Aeson (Result(..))
import           Models.Order
import					 Models.OrderItem
import 					 Models.Plant
import qualified Data.ByteString.Lazy          as BL
import           Data.ByteString.Builder       (toLazyByteString)
import           Data.Aeson.Key (Key, fromText)
import           Control.Monad (join)

routes :: Connection -> ScottyM ()
routes conn = do
  -- GET /api/plants (Public)
  get "/api/plants" $ do
    plants <- liftIO $ query_ conn "SELECT * FROM plants ORDER BY name ASC"
    R.ok (plants :: [Plant])

  -- GET /api/admin/plants (Admin)
  get "/api/admin/plants" $ do
    requireAdmin
    -- La route admin retourne toutes les plantes, y compris celles hors stock
    plants <- liftIO $ query_ conn "SELECT * FROM plants ORDER BY name ASC"
    R.ok (plants :: [Plant])

  -- GET /api/plants/:id (Public)
  get "/api/plants/:id" $ do
    plantId <- param "id"
    plants <- liftIO $ query conn "SELECT * FROM plants WHERE id = ?" (Only (plantId :: Int))
    case plants of
      [plant] -> R.ok (plant :: Plant)
      _       -> R.notFound "Plante non trouvée"

  -- POST /api/admin/plants (Admin)
  post "/api/admin/plants" $ do
    requireAdmin
    payload <- jsonData :: ActionM Plant
    -- L'ID et createdAt sont ignorés car gérés par la DB
    [Only newId] <- liftIO $ query conn "INSERT INTO plants (name, description, price, stock) VALUES (?, ?, ?, ?) RETURNING id"
      (plantName payload, plantDescription payload, plantPrice payload, plantStock payload)
    newPlants <- liftIO $ query conn "SELECT * FROM plants WHERE id = ?" (Only (newId :: Int))
    case newPlants of
        [newPlant] -> R.created (newPlant :: Plant)
        _ -> R.serverError "Impossible de récupérer la plante après création."

  -- PATCH /api/admin/plants/:id (Admin)
  patch "/api/admin/plants/:id" $ do
    requireAdmin
    plantId <- param "id"
    payload <- jsonData :: ActionM Value -- Utilise Value pour gérer les champs partiels

    -- Récupérer la plante existante
    existingPlants <- liftIO $ query conn "SELECT * FROM plants WHERE id = ?" (Only (plantId :: Int))
    case existingPlants of
      [] -> R.notFound "Plante non trouvée"
      [existingPlant] -> do
        let updatedPlant = applyPatch existingPlant payload
        _ <- liftIO $ execute conn "UPDATE plants SET name = ?, description = ?, price = ?, stock = ? WHERE id = ?"
              (plantName updatedPlant, plantDescription updatedPlant, plantPrice updatedPlant, plantStock updatedPlant, plantId)
        R.ok updatedPlant

  -- DELETE /api/admin/plants/:id (Admin)
  delete "/api/admin/plants/:id" $ do
    requireAdmin
    plantId <- param "id"
    rowsAffected <- liftIO $ execute conn "DELETE FROM plants WHERE id = ?" (Only (plantId :: Int))
    if rowsAffected > 0
      then status status200 -- Le test e2e attend 200, pas 204
      else R.notFound "Plante non trouvée"

objLookup :: FromJSON a => T.Text -> Object -> Maybe a
objLookup key o = join $ parseMaybe (.:? fromText key) o

-- | Applique les modifications d'un JSON partiel à une plante existante.
applyPatch :: Plant -> Value -> Plant
applyPatch plant (Object obj) =
  let newName        = fromMaybe (plantName plant) (objLookup "name" obj)
      newDescription = fromMaybe (plantDescription plant) (objLookup "description" obj)
      newPrice       = fromMaybe (plantPrice plant) (objLookup "price" obj)
      newStock       = fromMaybe (plantStock plant) (objLookup "stock" obj)
  in plant { plantName = newName
           , plantDescription = newDescription
           , plantPrice = newPrice
           , plantStock = newStock }
applyPatch plant _ = plant
