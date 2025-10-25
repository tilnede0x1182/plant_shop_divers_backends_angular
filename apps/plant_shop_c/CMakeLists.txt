cmake_minimum_required(VERSION 3.20)
project(plant_shop_c C)

# ----------------------------------
# Options globales
set(CMAKE_C_STANDARD 11)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# ----------------------------------
# Dépendances système (pkg-config)
find_package(PkgConfig REQUIRED)
pkg_check_modules(LIBPQ REQUIRED libpq)
pkg_check_modules(CJSON REQUIRED libcjson)
pkg_check_modules(ARGON2 REQUIRED libargon2)
pkg_check_modules(CURL REQUIRED libcurl)

# Inclure Kore et dépendances système depuis l'installation globale
include_directories(/usr/local/include/kore
	${LIBPQ_INCLUDE_DIRS} ${CJSON_INCLUDE_DIRS}
	${ARGON2_INCLUDE_DIRS} ${CURL_INCLUDE_DIRS})

link_directories(/usr/local/lib
	${LIBPQ_LIBRARY_DIRS} ${CJSON_LIBRARY_DIRS}
	${ARGON2_LIBRARY_DIRS} ${CURL_LIBRARY_DIRS})

# ----------------------------------
# Définition des bibliothèques communes (Kore global)
set(COMMON_LIBS
	kore
	${LIBPQ_LIBRARIES}
	${CJSON_LIBRARIES}
	${ARGON2_LIBRARIES}
	pthread m ssl crypto z)

# ----------------------------------
# Sources du serveur principal
file(GLOB_RECURSE SERVER_SOURCES
	"src/main.c"
	"routes.c"
	"src/controllers/*.c"
	"src/repository/*.c"
	"src/utils/utils.c")

add_executable(server ${SERVER_SOURCES})
target_link_libraries(server ${COMMON_LIBS})

# Flags Kore recommandés
target_compile_definitions(server PRIVATE
	KORE_NO_HTTP_GZIP
	KORE_SINGLE_BINARY
)

install(TARGETS server DESTINATION bin)

# ----------------------------------
# Exécutable pour le seeding
add_executable(seed_c db/seed.c)
target_link_libraries(seed_c ${LIBPQ_LIBRARIES} ${ARGON2_LIBRARIES})

# ----------------------------------
# Exécutable pour les tests E2E
add_executable(test_e2e_c test/test_e2e.c)
target_link_libraries(test_e2e_c ${CURL_LIBRARIES} ${CJSON_LIBRARIES})
