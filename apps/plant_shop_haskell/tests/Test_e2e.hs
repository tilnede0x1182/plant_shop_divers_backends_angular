{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE RecordWildCards   #-}

-- Traduction du test end-to-end Java en Haskell.
-- Ce script exécute une série de tests sur une API de boutique de plantes.
--
-- Dépendances nécessaires (pour le client de test) :
-- base, aeson, bytestring, containers, directory, http-types, network,
-- random, text, time, transformers, unliftio, wreq.

module Main where

import           Control.Concurrent      (threadDelay)
import           Control.Lens            ((&), (.~), (^.))
import           Control.Monad           (forM_, unless, void, when)
import           Control.Monad.IO.Class  (MonadIO (liftIO))
import           Data.Aeson              (FromJSON, ToJSON, Value (..), object,
                                          (.=))
import qualified Data.Aeson.Key          as Key
import qualified Data.Aeson.KeyMap       as KeyMap
import qualified Data.Aeson.Types        as Aeson
import qualified Data.ByteString.Char8   as BC
import qualified Data.ByteString.Lazy    as BL
import           Data.IORef              (IORef, atomicModifyIORef', newIORef,
                                          readIORef)
import qualified Data.Map.Strict         as Map
import           Data.Maybe              (fromMaybe, isJust)
import           Data.Scientific         (fromFloatDigits)
import           Data.Text               (Text)
import qualified Data.Text               as T
import qualified Data.Text.Encoding      as TE
import           Data.Time               (defaultTimeLocale, formatTime,
                                          getCurrentTime)
import           Network.HTTP.Client     (HttpException)
import           Network.HTTP.Types.Header (hContentType, hCookie)
import           Network.HTTP.Types.Status (statusCode)
import qualified Network.Socket          as Net
import qualified Network.Wreq            as Wreq
import           System.Directory        (doesFileExist)
import           System.Exit             (ExitCode (..), exitWith)
import           System.IO               (hPutStrLn, stderr)
import           System.Random           (randomRIO)
import           Text.Printf             (printf)
import           UnliftIO.Exception      (SomeException, catch, throwIO, try)

--------------------------------------------------------------------------------
-- État des Tests
--------------------------------------------------------------------------------

-- L'état mutable partagé entre les tests, similaire à l'objet `Test` en Java.
data TestState = TestState
  { stCookies   :: IORef (Map.Map Text BC.ByteString) -- Map des cookies par utilisateur ("admin", "user")
  , stTimestamp :: IORef Text                         -- Timestamp de la session de test
  }

--------------------------------------------------------------------------------
-- Configuration
--------------------------------------------------------------------------------

-- Charge les variables d'environnement depuis un fichier .env.
-- Gère gracieusement l'absence du fichier.
loadEnv :: MonadIO m => FilePath -> m (Map.Map String String)
loadEnv path = liftIO $ do
  exists <- doesFileExist path
  if not exists
    then pure Map.empty
    else do
      content <- readFile path
      let ls = lines content
      let parseLine l = case break (== '=') l of
            (key, '=' : val) -> Just (trim key, trim val)
            _                -> Nothing
      pure $ Map.fromList [p | Just p <- map parseLine ls]
  where
    trim = filter (`notElem` " \t\r\n")

-- Configuration globale chargée une seule fois.
-- Note: 'unsafePerformIO' est généralement à éviter, mais acceptable ici pour
-- une configuration globale et immuable au démarrage de l'application.
-- Cependant, une approche plus idiomatique est de le passer explicitement.
-- Nous allons le charger dans 'main' pour rester propre.

type Config = Map.Map String String

getBaseUrl :: Config -> String
getBaseUrl cfg = "http://localhost:" ++ port ++ "/api"
  where
    port = fromMaybe "4100" $ Map.lookup "SERVER_ADDRESS" cfg

adminEmail, adminPwd :: Text
adminEmail = "admin1@planteshop.com"
adminPwd = "password"

--------------------------------------------------------------------------------
-- Utilitaires
--------------------------------------------------------------------------------

-- Génère un timestamp au format YYYYMMDDHHMMSS.
getTimestamp :: IO Text
getTimestamp = T.pack . formatTime defaultTimeLocale "%Y%m%d%H%M%S" <$> getCurrentTime

-- Génère une chaîne de caractères aléatoire de longueur n.
randomString :: Int -> IO Text
randomString n = T.pack <$> sequence (replicate n randomChar)
  where
    alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
    randomChar = (alphabet !!) <$> randomRIO (0, length alphabet - 1)

-- Attend que le serveur soit disponible sur un port donné.
waitForServer :: String -> Int -> Int -> IO Bool
waitForServer host port timeoutMs = do
  startTime <- getCurrentTime
  go startTime
  where
    go startTime = do
      now <- getCurrentTime
      let elapsed = round $ (* 1000) $ toRational (now `diffUTCTime` startTime)
      if elapsed > timeoutMs
        then pure False
        else do
          result <- try (connectAndClose host port) :: IO (Either SomeException ())
          case result of
            Right () -> pure True
            Left _ -> do
              threadDelay 100000 -- Attend 100ms
              go startTime

    connectAndClose :: String -> Int -> IO ()
    connectAndClose host port = Net.withSocketsDo $ do
      addr <- head <$> Net.getAddrInfo (Just $ Net.defaultHints { Net.addrSocketType = Net.Stream }) (Just host) (Just $ show port)
      sock <- Net.socket (Net.addrFamily addr) (Net.addrSocketType addr) (Net.addrProtocol addr)
      Net.setSocketOption sock Net.NoDelay 1
      -- Le timeout de connexion est géré par la boucle externe
      Net.connect sock (Net.addrAddress addr)
      Net.close sock

--------------------------------------------------------------------------------
-- Client HTTP et Gestion des Cookies
--------------------------------------------------------------------------------

-- Fonction principale pour appeler l'API.
-- Gère la méthode, le chemin, le corps, les cookies, et le statut attendu.
call :: TestState -> Config -> String -> String -> Int -> Maybe Value -> Text -> IO Value
call st cfg method path expectedStatus body who = do
  cookies <- readIORef (stCookies st)
  let baseUrl = getBaseUrl cfg
      fullUrl = baseUrl ++ path
      opts = Wreq.defaults
           & Wreq.header hContentType .~ ["application/json"]
           & Wreq.checkResponse .~ Just (\_ _ -> pure ()) -- On gère le statut manuellement

  -- Ajoute le cookie si il existe pour 'who'
  let finalOpts = case Map.lookup who cookies of
        Just cookieVal -> opts & Wreq.header hCookie .~ [cookieVal]
        Nothing        -> opts

  -- Exécute la requête
  resp <- case (method, body) of
    ("GET", _) -> Wreq.getWith finalOpts fullUrl
    ("POST", Just payload) -> Wreq.postWith finalOpts fullUrl payload
    ("PATCH", Just payload) -> Wreq.patchWith finalOpts fullUrl payload
    ("DELETE", _) -> Wreq.deleteWith finalOpts fullUrl
    _ -> fail $ "Méthode HTTP non supportée ou corps manquant: " ++ method

  let status = resp ^. Wreq.responseStatus . Wreq.statusCode
      statusOK = status == expectedStatus

  -- Affiche le résultat de l'appel
  printf "%s %-7s %s [%d]\n" (if statusOK then "✅" else "❌" :: String) method path status

  -- Gestion des cookies de la réponse
  let setCookieHeaders = resp ^. Wreq.responseHeader "Set-Cookie"
  unless (BC.null setCookieHeaders) $ do
    let newCookie = fst $ BC.break (== ';') setCookieHeaders
    atomicModifyIORef' (stCookies st) $ \m ->
      let updatedCookies = Map.insertWith (\new old -> old <> "; " <> new) who newCookie m
      in (updatedCookies, ())

  -- Gestion de l'échec
  unless statusOK $ do
    let responseBody = resp ^. Wreq.responseBody
    fail $ printf "API %s %s -> %d (attendu %d)\n%s" method path status expectedStatus (BL.unpack responseBody)

  -- Parse le corps de la réponse en JSON
  let respBody = resp ^. Wreq.responseBody
  if BL.null respBody
    then pure Null
    else do
      decodedBody <- Wreq.asJSON resp
      pure (decodedBody ^. Wreq.responseBody)

-- Wrapper pour les appels qui retournent un tableau JSON.
-- En Haskell, `call` retourne déjà une `Value`, qui peut être un Array.
-- Cette fonction est donc un alias pour la clarté.
callArray :: TestState -> Config -> String -> String -> Int -> Maybe Value -> Text -> IO Value
callArray = call

--------------------------------------------------------------------------------
-- Helpers d'Authentification
--------------------------------------------------------------------------------

login :: TestState -> Config -> Text -> Text -> Text -> IO ()
login st cfg email pwd who = do
  let credentials = object ["email" .= email, "password" .= pwd]
  void $ call st cfg "POST" "/auth/login" 201 (Just credentials) who

register :: TestState -> Config -> Text -> Text -> Text -> Text -> IO ()
register st cfg name email pwd who = do
  let userData = object ["name" .= name, "email" .= email, "password" .= pwd]
  void $ call st cfg "POST" "/auth/register" 201 (Just userData) who

--------------------------------------------------------------------------------
-- Helpers d'Assertion
--------------------------------------------------------------------------------

-- Extrait une valeur d'un objet JSON par sa clé.
getKey :: Value -> Key.Key -> IO Value
getKey (Object o) k = maybe (fail $ "Clé manquante: " ++ show k) pure (KeyMap.lookup k o)
getKey _ k = fail $ "N'est pas un objet JSON, impossible de trouver la clé " ++ show k

-- Extrait un entier d'un objet JSON.
getInt :: Value -> Key.Key -> IO Int
getInt v k = do
    val <- getKey v k
    case val of
        Number s -> pure $ round s
        _        -> fail $ "La valeur pour la clé '" ++ show k ++ "' n'est pas un nombre."

-- Extrait un tableau JSON.
getArray :: Value -> IO [Value]
getArray (Array a) = pure $ Aeson.toList a
getArray _         = fail "La valeur n'est pas un tableau JSON."

-- Vérifie l'égalité d'une valeur dans un objet JSON.
assert_eq :: (Eq a, Show a, ToJSON a) => Value -> Key.Key -> a -> IO ()
assert_eq obj key expected = do
  actualVal <- getKey obj key
  let expectedVal = Aeson.toJSON expected
  let ok = actualVal == expectedVal
  -- L'encodage en JSON assure un affichage correct (ex: strings avec des guillemets)
  let actualStr = BC.unpack $ Aeson.encode actualVal
  let expectedStr = BC.unpack $ Aeson.encode expected
  printf "%s   ↳ %s=%s (attendu %s)\n"
    (if ok then "✅" else "❌" :: String)
    (Key.toString key)
    actualStr
    expectedStr
  unless ok $ fail $ "Assertion échouée pour la clé '" ++ Key.toString key ++ "'"

-- Vérifie qu'une clé contient une valeur numérique.
assert_num :: Value -> Key.Key -> IO ()
assert_num obj key = do
  val <- getKey obj key
  case val of
    Number _ -> pure ()
    _ -> fail $ "La clé '" ++ Key.toString key ++ "' n'est pas numérique."

--------------------------------------------------------------------------------
-- Modules de Test
--------------------------------------------------------------------------------

test_plants :: TestState -> Config -> IO ()
test_plants st cfg = do
    putStrLn "\n📌 TEST MODULE: PLANTS (admin)"
    let plantData = object ["name" .= ("Test Plant" :: Text), "price" .= (10 :: Int), "stock" .= (5 :: Int)]
    plant <- call st cfg "POST" "/admin/plants" 201 (Just plantData) "admin"
    assert_num plant "id"
    plantId <- getInt plant "id"

    get <- call st cfg "GET" ("/plants/" ++ show plantId) 200 Nothing "admin"
    assert_eq get "name" ("Test Plant" :: Text)

    let priceUpdate = object ["price" .= (15 :: Int)]
    void $ call st cfg "PATCH" ("/admin/plants/" ++ show plantId) 200 (Just priceUpdate) "admin"

    check <- call st cfg "GET" ("/plants/" ++ show plantId) 200 Nothing "admin"
    assert_eq check "price" (15 :: Int)
    name <- getKey check "name"
    printf "   ↳ name=%s\n" (show name)

    void $ call st cfg "DELETE" ("/admin/plants/" ++ show plantId) 200 Nothing "admin"

test_users :: TestState -> Config -> IO ()
test_users st cfg = do
    putStrLn "\n📌 TEST MODULE: USERS (admin)"
    ts <- readIORef (stTimestamp st)
    let email = "utilisateur_test_" <> ts <> "@example.com"
        userData = object [
            "email" .= email,
            "name" .= ("Utilisateur de test" :: Text),
            "password" .= ("pass123" :: Text)
          ]

    user <- call st cfg "POST" "/users" 201 (Just userData) "admin"
    userId <- getInt user "id"

    let nameUpdate = object ["name" .= ("Tester Update" :: Text)]
    void $ call st cfg "PATCH" ("/users/" ++ show userId) 200 (Just nameUpdate) "admin"

    get <- call st cfg "GET" ("/users/" ++ show userId) 200 Nothing "admin"
    assert_eq get "name" ("Tester Update" :: Text)

    void $ call st cfg "DELETE" ("/users/" ++ show userId) 200 Nothing "admin"

test_orders :: TestState -> Config -> IO ()
test_orders st cfg = do
    putStrLn "\n📌 TEST MODULE: ORDERS & ORDER ITEMS"
    ts <- readIORef (stTimestamp st)
    let plantName = "Plante_de_test_" <> ts
        plantData = object ["name" .= plantName, "price" .= (10 :: Int), "stock" .= (5 :: Int)]

    plant <- call st cfg "POST" "/admin/plants" 201 (Just plantData) "admin"
    assert_num plant "id"
    pid <- getInt plant "id"

    let item = object ["plantId" .= pid, "quantity" .= (2 :: Int)]
        orderData = object ["items" .= [item]]

    order <- call st cfg "POST" "/orders" 201 (Just orderData) "user"
    assert_num order "id"
    oid <- getInt order "id"

    let statusUpdate = object ["status" .= ("shipped" :: Text)]
    void $ call st cfg "PATCH" ("/orders/" ++ show oid) 200 (Just statusUpdate) "admin"

    listVal <- callArray st cfg "GET" "/orders" 200 Nothing "user"
    list <- getArray listVal

    found <- findM (\o -> (== oid) <$> getInt o "id") list
    case found of
      Nothing -> fail "Commande absente"
      Just o -> do
        assert_eq o "status" ("shipped" :: Text)
        orderItemsVal <- getKey o "orderItems"
        orderItems <- getArray orderItemsVal
        when (null orderItems) $ fail "Items absents dans la commande"
        let firstItem = head orderItems
        nestedPlant <- getKey firstItem "plant"
        assert_eq nestedPlant "name" plantName

    void $ call st cfg "DELETE" ("/orders/" ++ show oid) 200 Nothing "admin"
    void $ call st cfg "DELETE" ("/admin/plants/" ++ show pid) 200 Nothing "admin"
  where
    findM :: Monad m => (a -> m Bool) -> [a] -> m (Maybe a)
    findM _ [] = pure Nothing
    findM p (x:xs) = do
      b <- p x
      if b then pure (Just x) else findM p xs

test_user_profile :: TestState -> Config -> Text -> IO ()
test_user_profile st cfg userEmail = do
    putStrLn "\n📌 TEST MODULE: USER PROFILE (user)"
    usersVal <- callArray st cfg "GET" "/users" 200 Nothing "admin"
    users <- getArray usersVal

    userObj <- findM (\u -> (== userEmail) . getText <$> getKey u "email") users
    uid <- case userObj of
      Nothing -> fail "Utilisateur de test non trouvé"
      Just u  -> getInt u "id"

    profile <- call st cfg "GET" ("/users/" ++ show uid) 200 Nothing "user"
    assert_eq profile "id" uid

    ts <- readIORef (stTimestamp st)
    let newName = "Utilisateur_de_test_" <> ts
        nameUpdate = object ["name" .= newName]
    void $ call st cfg "PATCH" ("/users/" ++ show uid) 200 (Just nameUpdate) "user"

    updated <- call st cfg "GET" ("/users/" ++ show uid) 200 Nothing "user"
    assert_eq updated "name" newName

    let adminUpdate = object ["admin" .= True]
    void $ call st cfg "PATCH" ("/users/" ++ show uid) 200 (Just adminUpdate) "user" -- L'API doit ignorer

    check <- call st cfg "GET" ("/users/" ++ show uid) 200 Nothing "admin"
    assert_eq check "admin" False -- Vérification que l'utilisateur n'est pas devenu admin
  where
    getText (String t) = t
    getText _          = ""
    findM :: Monad m => (a -> m Bool) -> [a] -> m (Maybe a)
    findM _ [] = pure Nothing
    findM p (x:xs) = do
      b <- p x
      if b then pure (Just x) else findM p xs

test_auth_roles :: TestState -> Config -> IO ()
test_auth_roles st cfg = do
    putStrLn "\n📌 TEST MODULE: ROLES"
    let badPlant = object ["name" .= ("Bad" :: Text), "price" .= (1 :: Int), "stock" .= (1 :: Int)]
    void $ call st cfg "POST" "/admin/plants" 403 (Just badPlant) "user"

    let goodPlant = object ["name" .= ("Good" :: Text), "price" .= (1 :: Int), "stock" .= (1 :: Int)]
    plant <- call st cfg "POST" "/admin/plants" 201 (Just goodPlant) "admin"
    pid <- getInt plant "id"
    void $ call st cfg "DELETE" ("/admin/plants/" ++ show pid) 200 Nothing "admin"

    void $ call st cfg "GET" "/users" 403 Nothing "user"

test_admin_plants :: TestState -> Config -> IO ()
test_admin_plants st cfg = do
    putStrLn "\n📌 TEST MODULE: ADMIN PLANTS"
    plantsVal <- callArray st cfg "GET" "/admin/plants" 200 Nothing "admin"
    plants <- getArray plantsVal
    printf "   ↳ %d plantes récupérées\n" (length plants)

    ts <- readIORef (stTimestamp st)
    let plantData = object ["name" .= ("Plante_admin_" <> ts), "price" .= (99 :: Int), "stock" .= (12 :: Int)]
    p <- call st cfg "POST" "/admin/plants" 201 (Just plantData) "admin"
    pid <- getInt p "id"

    let priceUpdate = object ["price" .= (123 :: Int)]
    void $ call st cfg "PATCH" ("/admin/plants/" ++ show pid) 200 (Just priceUpdate) "admin"
    void $ call st cfg "DELETE" ("/admin/plants/" ++ show pid) 200 Nothing "admin"

test_admin_users :: TestState -> Config -> IO ()
test_admin_users st cfg = do
    putStrLn "\n📌 TEST MODULE: ADMIN USERS"
    ts <- readIORef (stTimestamp st)
    let email = "admin_temp_" <> ts <> "@example.com"
        name = "Admin Temporaire " <> ts
        tempAdminData = object ["email" .= email, "name" .= name, "password" .= ("password" :: Text), "admin" .= True]

    temp <- call st cfg "POST" "/users" 201 (Just tempAdminData) "admin"
    uid <- getInt temp "id"

    listVal <- callArray st cfg "GET" "/admin/users" 200 Nothing "admin"
    list <- getArray listVal

    target <- findM (\u -> (== email) . getText <$> getKey u "email") list
    case target of
      Nothing -> fail "L'admin temporaire n'a pas été trouvé dans la liste !"
      Just t  -> assert_eq t "name" name

    let newName = "Admin_temp_modifié_" <> ts
        nameUpdate = object ["name" .= newName]
    void $ call st cfg "PATCH" ("/users/" ++ show uid) 200 (Just nameUpdate) "admin"

    userGet <- call st cfg "GET" ("/users/" ++ show uid) 200 Nothing "admin"
    assert_eq userGet "name" newName

    void $ call st cfg "DELETE" ("/users/" ++ show uid) 200 Nothing "admin"
  where
    getText (String t) = t
    getText _          = ""
    findM :: Monad m => (a -> m Bool) -> [a] -> m (Maybe a)
    findM _ [] = pure Nothing
    findM p (x:xs) = do
      b <- p x
      if b then pure (Just x) else findM p xs

test_auth_me :: TestState -> Config -> IO ()
test_auth_me st cfg = do
    putStrLn "\n📌 TEST MODULE: AUTH /me"
    me <- call st cfg "GET" "/auth/me" 200 Nothing "user"
    emailVal <- getKey me "email"
    nameVal <- getKey me "name"
    assert_eq me "email" emailVal
    assert_eq me "name" nameVal
    printf "   ↳ Utilisateur connecté: %s (%s)\n" (show emailVal) (show nameVal)

--------------------------------------------------------------------------------
-- Point d'Entrée Principal
--------------------------------------------------------------------------------

main :: IO ()
main = do
  -- Charger la configuration
  cfg <- loadEnv ".env"
  let portStr = fromMaybe "4100" $ Map.lookup "SERVER_ADDRESS" cfg
      port = read portStr :: Int

  -- Attendre que le serveur soit prêt
  serverReady <- waitForServer "127.0.0.1" port 5000
  unless serverReady $ do
    hPutStrLn stderr $ "❌ Serveur http://localhost:" ++ portStr ++ " injoignable"
    exitWith (ExitFailure 2)

  -- Initialiser l'état des tests
  cookieRef <- newIORef Map.empty
  tsRef <- newIORef =<< getTimestamp
  let testState = TestState { stCookies = cookieRef, stTimestamp = tsRef }

  -- Préparer les données utilisateur pour les tests
  ts <- readIORef (stTimestamp testState)
  randTag <- randomString 4
  let userEmail = "utilisateur_de_test_" <> ts <> "_" <> randTag <> "@example.com"
      userPassword = "pass123"

  putStrLn $ "🧪 Démarrage des tests: " ++ getBaseUrl cfg ++ "\n"

  -- Exécuter la suite de tests
  (do
    -- Connexion des utilisateurs de base
    login testState cfg adminEmail adminPwd "admin"
    register testState cfg "User" userEmail userPassword "user"
    login testState cfg userEmail userPassword "user"

    -- Exécution des modules de test
    test_plants testState cfg
    test_users testState cfg
    test_orders testState cfg
    test_user_profile testState cfg userEmail
    test_auth_roles testState cfg
    test_admin_plants testState cfg
    test_admin_users testState cfg
    test_auth_me testState cfg

    putStrLn "\n🎉 Tous les tests ont réussi!"
    exitWith ExitSuccess

    ) `catch` (\e -> do
      hPutStrLn stderr $ "\n❌ Tests interrompus: " ++ show (e :: SomeException)
      exitWith (ExitFailure 1)
    )
