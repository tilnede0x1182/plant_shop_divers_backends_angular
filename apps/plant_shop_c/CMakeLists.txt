cmake_minimum_required(VERSION 3.20)
project(plant_shop_c C)

# ----------------------------------
# Options globales
set(CMAKE_C_STANDARD 11)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# ----------------------------------
# Dépendances système (pkg-config)
find_package(PkgConfig REQUIRED)
pkg_check_modules(LIBPQ   REQUIRED libpq)
pkg_check_modules(CJSON   REQUIRED libcjson)
pkg_check_modules(ARGON2  REQUIRED libargon2)

# Kore n’a pas de pc : supposer install “/usr/local/include/kore”
include_directories(/usr/local/include ${LIBPQ_INCLUDE_DIRS}
                    ${CJSON_INCLUDE_DIRS} ${ARGON2_INCLUDE_DIRS})
link_directories(/usr/local/lib ${LIBPQ_LIBRARY_DIRS}
                 ${CJSON_LIBRARY_DIRS} ${ARGON2_LIBRARY_DIRS})

# ----------------------------------
# Sources
file(GLOB_RECURSE SRC
     "src/*.c"
     "db/seed.c")

add_executable(server ${SRC})

# ----------------------------------
# Lien
target_link_libraries(server
	/usr/local/lib/libkore.a     # Kore compilé via “make install”
	${LIBPQ_LIBRARIES}
	${CJSON_LIBRARIES}
	${ARGON2_LIBRARIES}
	pthread m ssl crypto z)

# ----------------------------------
# Flags Kore recommandés
target_compile_definitions(server PRIVATE
	KORE_NO_HTTP_GZIP
	KORE_SINGLE_BINARY
)

# ----------------------------------
# Installation simple
install(TARGETS server DESTINATION bin)
