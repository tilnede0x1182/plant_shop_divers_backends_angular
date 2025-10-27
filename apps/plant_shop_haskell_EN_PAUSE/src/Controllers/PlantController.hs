{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE RecordWildCards   #-}

module Controllers.PlantController (routes) where

import           Control.Monad          (join)
import           Control.Monad.IO.Class (liftIO)
import           Data.Aeson             (Value (..))
import           Data.Aeson.Types       (FromJSON, Object, parseMaybe, (.:?))
import           Data.Aeson.Key         (fromText)
import           Data.Maybe             (fromMaybe)
import           Database.PostgreSQL.Simple
import           Network.HTTP.Types.Status (status200)
import qualified Data.Text              as T
import           Web.Scotty

import           Middleware.Auth        (requireAdmin)
import           Models.Plant           (CreatePlantPayload (..), Plant (..))
import qualified Utils.Response         as R

plantSelectBase :: Query
plantSelectBase =
  "SELECT id, name, description, price::int AS price, stock, created_at FROM plants"

selectPlantRows :: Query
selectPlantRows = plantSelectBase <> " ORDER BY name ASC"

routes :: Connection -> ScottyM ()
routes conn = do
  -- GET /api/plants (Public)
  get "/api/plants" $ do
    plants <- liftIO $ query_ conn selectPlantRows
    R.ok (plants :: [Plant])

  -- GET /api/admin/plants (Admin)
  get "/api/admin/plants" $ do
    requireAdmin
    -- La route admin retourne toutes les plantes, y compris celles hors stock
    plants <- liftIO $ query_ conn selectPlantRows
    R.ok (plants :: [Plant])

  -- GET /api/plants/:id (Public)
  get "/api/plants/:id" $ do
    plantId <- captureParam "id"
    plants <- liftIO $ query conn (plantSelectBase <> " WHERE id = ?") (Only (plantId :: Int))
    case plants of
      [plant] -> R.ok (plant :: Plant)
      _       -> R.notFound "Plante non trouvée"

  -- POST /api/admin/plants (Admin)
  post "/api/admin/plants" $ do
    requireAdmin
    payload <- jsonData :: ActionM CreatePlantPayload
    let newStock = fromMaybe 0 (createPlantStock payload)
        newPrice = createPlantPrice payload
    [Only newId] <- liftIO $ query conn "INSERT INTO plants (name, description, price, stock) VALUES (?, ?, ?, ?) RETURNING id"
      ( createPlantName payload
      , createPlantDescription payload
      , newPrice
      , newStock
      )
    newPlants <- liftIO $ query conn (plantSelectBase <> " WHERE id = ?") (Only (newId :: Int))
    case newPlants of
        [newPlant] -> R.created (newPlant :: Plant)
        _ -> R.serverError "Impossible de récupérer la plante après création."

  -- PATCH /api/admin/plants/:id (Admin)
  patch "/api/admin/plants/:id" $ do
    requireAdmin
    plantId <- captureParam "id"
    payload <- jsonData :: ActionM Value -- Utilise Value pour gérer les champs partiels

    -- Récupérer la plante existante
    existingPlants <- liftIO $ query conn (plantSelectBase <> " WHERE id = ?") (Only (plantId :: Int))
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
    plantId <- captureParam "id"
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
