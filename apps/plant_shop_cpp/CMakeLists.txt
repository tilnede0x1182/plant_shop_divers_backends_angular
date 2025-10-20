# ───────────────────────────────
#   Configuration de base
# ───────────────────────────────
cmake_minimum_required(VERSION 3.16)
project(plant_shop_cpp LANGUAGES CXX)

# Utilisation du standard moderne
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Génération dans un dossier "build" sans polluer la racine
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR})

# ───────────────────────────────
#   Dépendances (trouvées sur le système)
# ───────────────────────────────
find_package(Drogon REQUIRED)
find_package(Jsoncpp REQUIRED)
find_package(OpenSSL REQUIRED)
find_package(PostgreSQL REQUIRED)
find_package(ZLIB REQUIRED)
find_package(Threads REQUIRED)
find_package(UUID REQUIRED)

# ───────────────────────────────
#   Fichiers sources communs
# ───────────────────────────────
set(SRC_CONTROLLERS
	controllers/AuthController.cpp
	controllers/UserController.cpp
	controllers/PlantController.cpp
	controllers/OrderController.cpp
)

set(SRC_MODELS
	models/User.h
	models/Plant.h
	models/Order.h
	models/OrderItem.h
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

target_link_libraries(plant_shop_cpp
	Drogon::Drogon
	Jsoncpp_lib
	OpenSSL::SSL
	OpenSSL::Crypto
	PostgreSQL::PostgreSQL
	ZLIB::ZLIB
	Threads::Threads
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
	Jsoncpp_lib
	OpenSSL::SSL
	OpenSSL::Crypto
	PostgreSQL::PostgreSQL
	ZLIB::ZLIB
	Threads::Threads
)

# ───────────────────────────────
#   Exécutable de test (E2E natif)
# ───────────────────────────────
add_executable(test_e2e_cpp
	test/test_e2e.cpp
)

target_link_libraries(test_e2e_cpp
	Drogon::Drogon
	Jsoncpp_lib
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
