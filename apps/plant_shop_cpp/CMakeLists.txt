# ───────────────────────────────
#   Configuration de base
# ───────────────────────────────
cmake_minimum_required(VERSION 3.16)
project(plant_shop_cpp LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR})

# ───────────────────────────────
#   Détection et configuration manuelle des dépendances
# ───────────────────────────────
# Désactivation complète de MySQL et SQLite pour Drogon
set(BUILD_MYSQL OFF CACHE BOOL "Disable MySQL support")
set(BUILD_SQLITE OFF CACHE BOOL "Disable SQLite3 support")

# Activation explicite de PostgreSQL
set(BUILD_POSTGRESQL ON CACHE BOOL "Enable PostgreSQL support")

# Autoriser CMake à chercher MySQL/SQLite si jamais (mais ils sont désactivés)
set(CMAKE_DISABLE_FIND_PACKAGE_MySQL FALSE)
set(CMAKE_DISABLE_FIND_PACKAGE_SQLite3 FALSE)

# Répertoire d’installation de Drogon
set(CMAKE_PREFIX_PATH "/usr/lib/x86_64-linux-gnu/cmake/Drogon")

# Accélération de détection des bibliothèques partagées (.so)
set(OPENSSL_ROOT_DIR "/usr")
set(OPENSSL_USE_STATIC_LIBS FALSE)
set(PostgreSQL_LIBRARY "/usr/lib/x86_64-linux-gnu/libpq.so")
set(SQLITE3_LIBRARY "/usr/lib/x86_64-linux-gnu/libsqlite3.so")
set(ZLIB_LIBRARY "/usr/lib/x86_64-linux-gnu/libz.so")
set(Threads_LIBRARY "/usr/lib/x86_64-linux-gnu/libpthread.so")
set(UUID_LIBRARY "/usr/lib/x86_64-linux-gnu/libuuid.so")

# Correction MySQL : indiquer explicitement les répertoires
set(MySQL_INCLUDE_DIR "/usr/include/mysql")
set(MySQL_LIBRARY_DIR "/usr/lib/x86_64-linux-gnu")

# Éviter l’échec si MySQL non trouvé (DrogonConfig requiert MySQL_FOUND)
set(MySQL_FOUND TRUE CACHE BOOL "Force MySQL found to bypass Drogon check")

# Recherche des paquets
find_package(Drogon REQUIRED)
find_package(OpenSSL REQUIRED)
find_package(PostgreSQL REQUIRED)
find_package(ZLIB REQUIRED)
find_package(Threads REQUIRED)
include_directories(/usr/include/jsoncpp)

# ───────────────────────────────
#   Fichiers controllers
# ───────────────────────────────
set(SRC_CONTROLLERS
	controllers/AuthController.cpp
	controllers/UserController.cpp
	controllers/PlantController.cpp
	controllers/OrderController.cpp
)

# ───────────────────────────────
#   Fichiers modèles
# ───────────────────────────────
set(SRC_MODELS
	# Modèles ORM générés par drogon_ctl
	models/Users.cc
	models/Plants.cc
	models/Orders.cc
	models/OrderItems.cc
)

# ───────────────────────────────
#   Répertoires d’inclusion
# ───────────────────────────────
include_directories(
	${CMAKE_CURRENT_SOURCE_DIR}/models
)

# ───────────────────────────────
#   Exécutable principal (serveur)
# ───────────────────────────────
add_executable(plant_shop_cpp
	main.cpp
	routes.h
	${SRC_CONTROLLERS}
	${SRC_MODELS}
)

find_library(ARGON2_LIB argon2 REQUIRED)

target_link_libraries(plant_shop_cpp
	Drogon::Drogon
	OpenSSL::SSL
	OpenSSL::Crypto
	PostgreSQL::PostgreSQL
	ZLIB::ZLIB
	Threads::Threads
	${ARGON2_LIB}
)

# ───────────────────────────────
#   Exécutable de seed (initialisation DB)
# ───────────────────────────────
add_executable(seed_cpp
	db/Seed.cpp
	${SRC_MODELS}
)

target_link_libraries(seed_cpp
	Drogon::Drogon
	OpenSSL::SSL
	OpenSSL::Crypto
	PostgreSQL::PostgreSQL
	ZLIB::ZLIB
	Threads::Threads
	argon2
)

# ───────────────────────────────
#   Exécutable de test (E2E natif)
# ───────────────────────────────
add_executable(test_e2e_cpp
	test/test_e2e.cpp
)

target_link_libraries(test_e2e_cpp
	Drogon::Drogon
	OpenSSL::SSL
	OpenSSL::Crypto
	PostgreSQL::PostgreSQL
	ZLIB::ZLIB
	Threads::Threads
)

# ───────────────────────────────
#   Messages de confort
# ───────────────────────────────
message(STATUS "C++ standard: ${CMAKE_CXX_STANDARD}")
message(STATUS "CMake générera 3 exécutables :")
message(STATUS "  • plant_shop_cpp  (serveur principal)")
message(STATUS "  • seed_cpp        (seed DB)")
message(STATUS "  • test_e2e_cpp    (test end-to-end)")
