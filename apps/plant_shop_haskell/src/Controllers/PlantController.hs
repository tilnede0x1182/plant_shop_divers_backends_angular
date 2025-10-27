{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE RecordWildCards   #-}

module Controllers.PlantController (routes) where

import           Control.Monad.IO.Class (liftIO)
import           Data.Aeson             (Value (..), object, (.=))
import           Database.PostgreSQL.Simple
import           Web.Scotty

import           Middleware.Auth        (requireAdmin, requireUser)
import           Models.Plant
import           Models.User            (User (..))
import qualified Utils.Response         as R

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

-- | Applique les modifications d'un JSON partiel à une plante existante.
applyPatch :: Plant -> Value -> Plant
applyPatch plant (Object obj) =
  plant {
    plantName = fromMaybe (plantName plant) (obj .:? "name" >>= fromJSON' :: Maybe Text),
    plantDescription = fromMaybe (plantDescription plant) (obj .:? "description" >>= fromJSON' :: Maybe (Maybe Text)),
    plantPrice = fromMaybe (plantPrice plant) (obj .:? "price" >>= fromJSON' :: Maybe Double),
    plantStock = fromMaybe (plantStock plant) (obj .:? "stock" >>= fromJSON' :: Maybe Int)
  }
  where
    fromJSON' v = case fromJSON v of Success a -> Just a; _ -> Nothing
applyPatch plant _ = plant -- Ne fait rien si le payload n'est pas un objet
