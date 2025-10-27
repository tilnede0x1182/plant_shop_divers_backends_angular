{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE RecordWildCards #-}

module Main where

import           Control.Exception (bracket)
import           Control.Monad (forM, forM_, replicateM, foldM)
import           Control.Monad.IO.Class (liftIO)
import qualified Crypto.BCrypt as Bcrypt
import qualified Data.ByteString.Char8 as BS
import           Data.Char (toUpper, toLower)
import           Data.List (foldl')
import qualified Data.Map as Map
import           Data.Maybe (fromMaybe)
import           Data.Scientific (fromFloatDigits)
import qualified Data.Text as T
import qualified Data.Text.Encoding as TE
import qualified Data.Text.IO as TIO
import           Database.PostgreSQL.Simple
import           Database.PostgreSQL.Simple.Types (Only(..))
import           System.IO (hFlush, stdout)
import           System.Random (randomRIO)

-- | Configuration de la base de données lue depuis .env
data DbConfig = DbConfig
  { dbUrl  :: BS.ByteString
  , dbUser :: BS.ByteString
  , dbPass :: BS.ByteString
  } deriving (Show)

-- | Lit le fichier .env et le parse
readEnv :: IO DbConfig
readEnv = do
    content <- readFile ".env"
    let lines' = lines content
        envMap = foldl' parseLine Map.empty lines'
    return DbConfig
        { dbUrl  = BS.pack $ fromMaybe "" $ Map.lookup "DATABASE_URL" envMap
        , dbUser = BS.pack $ fromMaybe "" $ Map.lookup "DATABASE_USER" envMap
        , dbPass = BS.pack $ fromMaybe "" $ Map.lookup "DATABASE_PASS" envMap
        }

  where
    parseLine acc line =
        case break (== '=') line of
            (key, '=':val) -> Map.insert (trim key) (trim val) acc
            _              -> acc

    trim :: String -> String
    trim = filter (`notElem` (" \r\n" :: String))

-- | Informations sur une plante, utilisées pour la création des commandes
data PlantInfo = PlantInfo
  { plantId    :: Int
  , plantPrice :: Double
  , plantStock :: Int
  } deriving (Show)

-- | Met à jour le stock d'une plante dans une liste de PlantInfo
updatePlantStock :: Int -> Int -> [PlantInfo] -> [PlantInfo]
updatePlantStock pId quantity = map update
  where
    update p@PlantInfo{..}
      | plantId == pId = p { plantStock = plantStock - quantity }
      | otherwise      = p

--------------------------------------------------------------------------------
-- CONSTANTES (identiques à la version Java)
--------------------------------------------------------------------------------

nbAdmins :: Int
nbAdmins = 3

nbUsers :: Int
nbUsers = 20

nbPlants :: Int
nbPlants = 50

maxOrdersPerUser :: Int
maxOrdersPerUser = 7

plantNames :: [T.Text]
plantNames = [
    "Rose","Tulipe","Lavande","Orchidée","Basilic","Menthe","Pivoine","Tournesol",
    "Cactus (Echinopsis)","Bambou","Camomille (Matricaria recutita)","Sauge (Salvia officinalis)",
    "Romarin (Rosmarinus officinalis)","Thym (Thymus vulgaris)","Laurier-rose (Nerium oleander)",
    "Aloe vera","Jasmin (Jasminum officinale)","Hortensia (Hydrangea macrophylla)",
    "Marguerite (Leucanthemum vulgare)","Géranium (Pelargonium graveolens)",
    "Fuchsia (Fuchsia magellanica)","Anémone (Anemone coronaria)","Azalée (Rhododendron simsii)",
    "Chrysanthème (Chrysanthemum morifolium)","Digitale pourpre (Digitalis purpurea)",
    "Glaïeul (Gladiolus hortulanus)","Lys (Lilium candidum)","Violette (Viola odorata)",
    "Muguet (Convallaria majalis)","Iris (Iris germanica)","Lavandin (Lavandula intermedia)",
    "Érable du Japon (Acer palmatum)","Citronnelle (Cymbopogon citratus)","Pin parasol (Pinus pinea)",
    "Cyprès (Cupressus sempervirens)","Olivier (Olea europaea)","Papyrus (Cyperus papyrus)",
    "Figuier (Ficus carica)","Eucalyptus (Eucalyptus globulus)","Acacia (Acacia dealbata)",
    "Bégonia (Begonia semperflorens)","Calathea (Calathea ornata)","Dieffenbachia (Dieffenbachia seguine)",
    "Ficus elastica","Sansevieria (Sansevieria trifasciata)","Philodendron (Philodendron scandens)",
    "Yucca (Yucca elephantipes)","Zamioculcas zamiifolia","Monstera deliciosa",
    "Pothos (Epipremnum aureum)","Agave (Agave americana)","Cactus raquette (Opuntia ficus-indica)"
  ]

firstNames :: [T.Text]
firstNames = ["Alice","Bruno","Cathy","David","Emma","Franck", "Gwen","Hugo","Inès","Jules","Katia","Léo"]

lastNames :: [T.Text]
lastNames = ["Dupont","Martin","Bernard","Petit","Robert","Richard","Durand","Moreau","Roux","Fournier"]

emailDomains :: [T.Text]
emailDomains = ["gmail.com","yahoo.com","hotmail.com"]

--------------------------------------------------------------------------------
-- FONCTIONS UTILITAIRES (identiques à la version Java)
--------------------------------------------------------------------------------

-- | Sélectionne un élément aléatoire dans une liste
pick :: [a] -> IO a
pick xs = (xs !!) <$> randomRIO (0, length xs - 1)

-- | Génère un mot de passe aléatoire
randPwd :: IO T.Text
randPwd = T.pack . ("pw" ++) . show <$> (randomRIO (100000000, 999999999) :: IO Int)

-- | Hache un mot de passe avec bcrypt
hashPwd :: T.Text -> IO T.Text
hashPwd pwd = do
    let bsPwd = TE.encodeUtf8 pwd
    maybeHashed <- Bcrypt.hashPasswordUsingPolicy Bcrypt.fastBcryptHashingPolicy bsPwd
    case maybeHashed of
        Just hashed -> return (TE.decodeUtf8 hashed)
        Nothing     -> fail "Erreur lors du hachage du mot de passe"

-- | Génère une phrase "lorem ipsum"
loremSentence :: IO T.Text
loremSentence = do
    let words' = ["lorem","ipsum","dolor","sit","amet","consectetur","adipiscing","elit",
                  "sed","do","eiusmod","tempor","incididunt","ut","labore","et","dolore","magna","aliqua"]
    n <- randomRIO (10, 14)
    selectedWords <- replicateM n (pick words')
    let capitalized = case selectedWords of
                        (w:ws) -> T.cons (toUpper (T.head w)) (T.tail w) : ws
                        []     -> []
    return $ T.unwords capitalized <> "."

--------------------------------------------------------------------------------
-- LOGIQUE PRINCIPALE DU SEEDING
--------------------------------------------------------------------------------

main :: IO ()
main = do
    putStrLn "🔧 Lecture de la configuration .env..."
    cfg <- readEnv

    let connInfo = defaultConnectInfo
            { connectHost     = "localhost"
            , connectPort     = 5432
            , connectUser     = BS.unpack (dbUser cfg)
            , connectPassword = BS.unpack (dbPass cfg)
            , connectDatabase = "plant_shop_haskell"
            }

    bracket (connect connInfo) close $ \conn -> do
        -- Nettoyage
        putStrLn "🧹 Nettoyage de la base de données…"
        _ <- execute_ conn "TRUNCATE order_items,orders,plants,users RESTART IDENTITY CASCADE"
        putStrLn "✅ Base vidée."
        hFlush stdout

        -- Création des administrateurs
        putStrLn "👑 Création des administrateurs…"
        (adminIds, adminCreds) <- createAdmins conn
        putStrLn $ "✅ " ++ show (length adminIds) ++ " admins."
        hFlush stdout

        -- Création des utilisateurs
        putStrLn "👥 Création des utilisateurs…"
        (userIds, userCreds) <- createUsers conn
        putStrLn $ "✅ " ++ show (length userIds) ++ " utilisateurs."
        hFlush stdout

        -- Création des plantes
        putStrLn "🌱 Création des plantes…"
        plants <- createPlants conn
        putStrLn $ "✅ " ++ show (length plants) ++ " plantes."
        hFlush stdout

        -- Création des commandes
        putStrLn "🛒 Création des commandes…"
        totalOrders <- createOrders conn userIds plants
        putStrLn $ "✅ " ++ show totalOrders ++ " commandes."
        hFlush stdout

        -- Écriture du fichier users.txt
        let credsOut = ["Administrateurs :\n"] ++ adminCreds ++ ["", "Utilisateurs :\n"] ++ userCreds
        TIO.writeFile "users.txt" (T.unlines credsOut)
        putStrLn $ "✍️ Fichier users.txt généré (" ++ show (length credsOut) ++ " lignes)."

        putStrLn "🎉 Seed terminée !"

-- | Crée les administrateurs
createAdmins :: Connection -> IO ([Int], [T.Text])
createAdmins conn = do
    results <- forM [1..nbAdmins] $ \i -> do
        firstName <- pick firstNames
        lastName <- pick lastNames
        let name = firstName <> " " <> lastName
        let email = "admin" <> T.pack (show i) <> "@planteshop.com"
        let pwd = "password"
        hashed <- hashPwd pwd
        -- Insertion et récupération de l'ID
        [Only userId] <- query conn "INSERT INTO users(name,email,password_hash,is_admin) VALUES (?,?,?,?) RETURNING id"
                               (name, email, hashed, True)
        return (userId, email <> " " <> pwd)
    -- Sépare les IDs des credentials
    return (map fst results, map snd results)

-- | Crée les utilisateurs normaux
createUsers :: Connection -> IO ([Int], [T.Text])
createUsers conn = do
    results <- forM [1..nbUsers] $ \_ -> do
        firstName <- pick firstNames
        lastName <- pick lastNames
        randNum <- randomRIO (20, 99) :: IO Int
        domain <- pick emailDomains
        let name = firstName <> " " <> lastName
        let email = T.toLower firstName <> "_" <> T.toLower lastName <> T.pack (show randNum) <> "@" <> domain
        pwd <- randPwd
        hashed <- hashPwd pwd
        [Only userId] <- query conn "INSERT INTO users(name,email,password_hash,is_admin) VALUES (?,?,?,?) RETURNING id"
                               (name, email, hashed, False)
        return (userId, email <> " " <> pwd)
    return (map fst results, map snd results)

-- | Crée les plantes
createPlants :: Connection -> IO [PlantInfo]
createPlants conn =
    forM [0..nbPlants-1] $ \i -> do
        let baseName = plantNames !! (i `mod` length plantNames)
        -- Réplique la logique Java pour les noms si nbPlants > nombre de noms disponibles
        let plantName = if nbPlants > length plantNames
                        then baseName <> " " <> T.pack (show (i `div` length plantNames + 1))
                        else baseName
        description <- loremSentence
        price <- randomRIO (5.0, 50.0) :: IO Double
        stock <- randomRIO (5, 30) :: IO Int
        [Only plantId] <- query conn "INSERT INTO plants(name,description,price,stock) VALUES (?,?,?,?) RETURNING id"
                                (plantName, description, fromFloatDigits price, stock)
        return PlantInfo { plantId = plantId, plantPrice = price, plantStock = stock }

-- | Crée les commandes pour tous les utilisateurs
createOrders :: Connection -> [Int] -> [PlantInfo] -> IO Int
createOrders conn userIds initialPlants = do
    -- On utilise un foldM pour "enfiler" l'état de la liste des plantes à travers la création des commandes de chaque utilisateur
    (totalOrders, _) <- foldM (createOrdersForUser conn) (0, initialPlants) userIds
    return totalOrders

-- | Crée les commandes pour un utilisateur donné, en gérant l'état des stocks
createOrdersForUser :: Connection -> (Int, [PlantInfo]) -> Int -> IO (Int, [PlantInfo])
createOrdersForUser conn (totalOrders, currentPlants) userId = do
    numOrders <- randomRIO (0, maxOrdersPerUser)
    -- De même, on utilise un foldM pour gérer l'état des stocks à travers la création de chaque commande
    foldM (createSingleOrder conn userId) (totalOrders, currentPlants) [1..numOrders]

-- | Crée une seule commande et ses articles
createSingleOrder :: Connection -> Int -> (Int, [PlantInfo]) -> Int -> IO (Int, [PlantInfo])
createSingleOrder conn userId (totalOrders, currentPlants) _ = do
    let statuses = ["confirmed", "pending", "shipped", "delivered"] :: [T.Text]
    status <- pick statuses

    -- Insère une commande avec un total de 0 pour obtenir l'ID
    [Only orderId] <- query conn
        "INSERT INTO orders(user_id, total, status) VALUES (?, 0, ?) RETURNING id"
        (userId, T.unpack status)

    -- Crée 2 articles de commande
    (finalTotal, finalPlants) <- foldM (createOrderItem conn orderId) (0.0, currentPlants) [1..2]

    -- Met à jour le total de la commande
    _ <- execute conn "UPDATE orders SET total = ? WHERE id = ?" (fromFloatDigits finalTotal, orderId)

    return (totalOrders + 1, finalPlants)

-- | Crée un seul article de commande
createOrderItem :: Connection -> Int -> (Double, [PlantInfo]) -> Int -> IO (Double, [PlantInfo])
createOrderItem conn orderId (currentTotal, currentPlants) _ = do
    let availablePlants = filter (\p -> plantStock p > 0) currentPlants
    if null availablePlants
    then return (currentTotal, currentPlants) -- Pas de plantes en stock, on ne fait rien
    else do
        -- Sélectionne une plante avec du stock
        plant <- pick availablePlants
        let maxQty = min 5 (plantStock plant)
        quantity <- randomRIO (1, maxQty)

        -- Insère l'article de commande
        _ <- execute conn "INSERT INTO order_items(order_id, plant_id, quantity, price) VALUES (?, ?, ?, ?)"
                   (orderId, plantId plant, quantity, fromFloatDigits (plantPrice plant))

        -- Met à jour le stock dans la base de données
        _ <- execute conn "UPDATE plants SET stock = stock - ? WHERE id = ?" (quantity, plantId plant)

        let itemTotal = plantPrice plant * fromIntegral quantity
        let newTotal = currentTotal + itemTotal
        -- Met à jour l'état du stock dans notre liste en mémoire
        let newPlants = updatePlantStock (plantId plant) quantity currentPlants

        return (newTotal, newPlants)
