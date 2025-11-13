#!/bin/bash

################################################################################
# Script de création d'arborescence pour les architectures distribuées et microservices
# À exécuter depuis ~/code/tilnede0x1182/Personnel/2025/Entrainement/plant_shop/DiversBackends/apps/plant_shop_java
################################################################################

# Définir le répertoire de base
BASE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$BASE_DIR"

echo "Exécution depuis : $BASE_DIR"

################################################################################
# 1. Spring Boot avec Hibernate - Architecture Distribuée
################################################################################

echo ""
echo "=== 1/10 Spring Boot avec Hibernate - Architecture Distribuée ==="
cd "$BASE_DIR/Structure_en_application_distribuée/plant_shop_java_spring_distribuée/plant_shop_java_spring_boot_security_hibernate_distribuée" || exit 1

# Création de l'arborescence
mkdir -p auth-service/src/{controllers,models,repositories,security} auth-service/{bin,config}
mkdir -p catalog-service/src/{controllers,models,repositories} catalog-service/{bin,config}
mkdir -p order-service/src/{controllers,models,repositories} order-service/{bin,config}
mkdir -p user-service/src/{controllers,models,repositories} user-service/{bin,config}
mkdir -p gateway/{src,bin,config}
mkdir -p utils/{src,bin}
mkdir -p {db,config,lib,bin,test,build}

# Copie des fichiers depuis le monolithe Spring Boot Hibernate
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/controllers/AuthController.java" auth-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/models/User.java" auth-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/repositories/UserRepository.java" auth-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/security/SecurityConfig.java" auth-service/src/security/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/controllers/PlantController.java" catalog-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/models/Plant.java" catalog-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/repositories/PlantRepository.java" catalog-service/src/repositories/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/controllers/OrderController.java" order-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/controllers/OrderItemController.java" order-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/models/Order.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/models/OrderItem.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/repositories/OrderRepository.java" order-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/repositories/OrderItemRepository.java" order-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/repositories/PlantRepository.java" order-service/src/repositories/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/controllers/UserController.java" user-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/models/User.java" user-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/repositories/UserRepository.java" user-service/src/repositories/

# Copie depuis le projet distribuée existant pour le gateway et utils
cp "$BASE_DIR/Structure_en_application_distribuée/plant_shop_java_distribuée/gateway/Gateway.java" gateway/ 2>/dev/null || true
cp "$BASE_DIR/Structure_en_application_distribuée/plant_shop_java_distribuée/gateway/src/Main.java" gateway/src/ 2>/dev/null || true
cp "$BASE_DIR/Structure_en_application_distribuée/plant_shop_java_distribuée/utils/"*.java utils/src/ 2>/dev/null || true

# Copie des fichiers communs
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/db/schema.sql" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/db/Seed.java" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/config/dependencies.txt" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/config/InstallCoursier.java" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/Makefile" ./
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/test/Test.java" test/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/users.txt" ./ 2>/dev/null || true

touch backend_java_spring_hibernate_distribuée.txt
echo "✓ Terminé"


################################################################################
# 2. Spring Boot sans Hibernate - Architecture Distribuée
################################################################################

echo ""
echo "=== 2/10 Spring Boot sans Hibernate - Architecture Distribuée ==="
cd "$BASE_DIR/Structure_en_application_distribuée/plant_shop_java_spring_distribuée/plant_shop_java_spring_boot_security_distribuée" || exit 1

# Création de l'arborescence
mkdir -p auth-service/src/{controllers,models,repositories,security} auth-service/{bin,config}
mkdir -p catalog-service/src/{controllers,models,repositories} catalog-service/{bin,config}
mkdir -p order-service/src/{controllers,models,repositories} order-service/{bin,config}
mkdir -p user-service/src/{controllers,models,repositories} user-service/{bin,config}
mkdir -p gateway/{src,bin,config}
mkdir -p utils/{src,bin}
mkdir -p {db,config,lib,bin,test,build}

# Copie des fichiers depuis le monolithe Spring Boot sans Hibernate
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/controllers/AuthController.java" auth-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/models/User.java" auth-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/repositories/UserRepository.java" auth-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/security/SecurityConfig.java" auth-service/src/security/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/controllers/PlantController.java" catalog-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/models/Plant.java" catalog-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/repositories/PlantRepository.java" catalog-service/src/repositories/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/controllers/OrderController.java" order-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/controllers/OrderItemController.java" order-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/models/Order.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/models/OrderItem.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/repositories/OrderRepository.java" order-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/repositories/OrderItemRepository.java" order-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/repositories/PlantRepository.java" order-service/src/repositories/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/controllers/UserController.java" user-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/models/User.java" user-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/repositories/UserRepository.java" user-service/src/repositories/

# Copie du gateway et utils
cp -r "$BASE_DIR/Structure_en_application_distribuée/plant_shop_java_distribuée/gateway/"* gateway/ 2>/dev/null || true
cp -r "$BASE_DIR/Structure_en_application_distribuée/plant_shop_java_distribuée/utils/"* utils/ 2>/dev/null || true

# Copie des fichiers communs
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/db/schema.sql" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/db/Seed.java" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/config/dependencies.txt" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/config/InstallCoursier.java" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/Makefile" ./
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/test/Test.java" test/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/users.txt" ./ 2>/dev/null || true

touch backend_java_spring_distribuée.txt
echo "✓ Terminé"


################################################################################
# 3. Micronaut - Architecture Distribuée
################################################################################

echo ""
echo "=== 3/10 Micronaut - Architecture Distribuée ==="
cd "$BASE_DIR/Structure_en_application_distribuée/plant_shop_java_micronaut_distribuée" || exit 1

# Création de l'arborescence
mkdir -p auth-service/src/{controllers,models,repositories,security} auth-service/{bin,config}
mkdir -p catalog-service/src/{controllers,models,repositories} catalog-service/{bin,config}
mkdir -p order-service/src/{controllers,models,repositories} order-service/{bin,config}
mkdir -p user-service/src/{controllers,models,repositories} user-service/{bin,config}
mkdir -p gateway/{src,bin,config}
mkdir -p utils/{src,bin}
mkdir -p {db,config,lib,bin,test,build}

# Copie des fichiers depuis le monolithe Micronaut
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/controllers/AuthController.java" auth-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/models/User.java" auth-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/repositories/UserRepository.java" auth-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/security/"*.java auth-service/src/security/ 2>/dev/null || true

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/controllers/PlantController.java" catalog-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/models/Plant.java" catalog-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/repositories/PlantRepository.java" catalog-service/src/repositories/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/controllers/OrderController.java" order-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/models/Order.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/models/OrderItem.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/repositories/OrderRepository.java" order-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/repositories/OrderItemRepository.java" order-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/repositories/PlantRepository.java" order-service/src/repositories/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/controllers/UserController.java" user-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/models/User.java" user-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/repositories/UserRepository.java" user-service/src/repositories/

# Copie du gateway et utils
cp -r "$BASE_DIR/Structure_en_application_distribuée/plant_shop_java_micronaut_distribuée/gateway/"* gateway/ 2>/dev/null || \
cp -r "$BASE_DIR/Structure_en_application_distribuée/plant_shop_java_distribuée/gateway/"* gateway/ 2>/dev/null || true

cp -r "$BASE_DIR/Structure_en_application_distribuée/plant_shop_java_micronaut_distribuée/utils/"* utils/ 2>/dev/null || \
cp -r "$BASE_DIR/Structure_en_application_distribuée/plant_shop_java_distribuée/utils/"* utils/ 2>/dev/null || true

# Copie des utils spécifiques Micronaut
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/utils/"*.java utils/src/ 2>/dev/null || true

# Copie des fichiers communs
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/db/schema.sql" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/db/Seed.java" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/config/dependencies.txt" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/config/InstallCoursier.java" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/Makefile" ./
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/test/Test.java" test/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/users.txt" ./ 2>/dev/null || true

touch backend_java_micronaut_distribuée.txt
echo "✓ Terminé"


################################################################################
# 4. Quarkus - Architecture Distribuée
################################################################################

echo ""
echo "=== 4/10 Quarkus - Architecture Distribuée ==="
cd "$BASE_DIR/Structure_en_application_distribuée/plant_shop_java_quarkus_distribuée" || exit 1

# Création de l'arborescence
mkdir -p auth-service/src/{controllers,models,repositories,security,META-INF} auth-service/{bin,config}
mkdir -p catalog-service/src/{controllers,models,repositories,META-INF} catalog-service/{bin,config}
mkdir -p order-service/src/{controllers,models,repositories,META-INF} order-service/{bin,config}
mkdir -p user-service/src/{controllers,models,repositories,META-INF} user-service/{bin,config}
mkdir -p gateway/src/META-INF gateway/{bin,config}
mkdir -p utils/{src,bin}
mkdir -p {db,config,lib,bin,test,build,quarkus}

# Copie des fichiers depuis le monolithe Quarkus
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/controllers/AuthController.java" auth-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/models/User.java" auth-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/repositories/UserRepository.java" auth-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/security/"*.java auth-service/src/security/ 2>/dev/null || true
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/META-INF/beans.xml" auth-service/src/META-INF/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/controllers/PlantController.java" catalog-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/models/Plant.java" catalog-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/repositories/PlantRepository.java" catalog-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/META-INF/beans.xml" catalog-service/src/META-INF/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/controllers/OrderController.java" order-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/models/Order.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/models/OrderItem.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/repositories/OrderRepository.java" order-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/repositories/OrderItemRepository.java" order-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/repositories/PlantRepository.java" order-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/META-INF/beans.xml" order-service/src/META-INF/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/controllers/UserController.java" user-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/models/User.java" user-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/repositories/UserRepository.java" user-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/META-INF/beans.xml" user-service/src/META-INF/

# Copie du gateway
cp -r "$BASE_DIR/Structure_en_application_distribuée/plant_shop_java_quarkus_distribuée/gateway/"* gateway/ 2>/dev/null || \
cp -r "$BASE_DIR/Structure_en_application_distribuée/plant_shop_java_distribuée/gateway/"* gateway/ 2>/dev/null || true
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/META-INF/beans.xml" gateway/src/META-INF/ 2>/dev/null || true

# Copie des utils
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/utils/"*.java utils/src/ 2>/dev/null || true

# Copie des fichiers communs
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/db/schema.sql" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/db/Seed.java" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/config/dependencies.txt" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/config/InstallCoursier.java" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/Makefile" ./
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/test/Test.java" test/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/users.txt" ./ 2>/dev/null || true
cp -r "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/quarkus/"* quarkus/ 2>/dev/null || true

touch backend_java_quarkus_distribuée.txt
echo "✓ Terminé"


################################################################################
# 5. Javalin - Architecture Distribuée
################################################################################

echo ""
echo "=== 5/10 Javalin - Architecture Distribuée ==="
cd "$BASE_DIR/Structure_en_application_distribuée/plant_shop_java_javalin_distribuée" || exit 1

# Création de l'arborescence
mkdir -p auth-service/src/{controllers,models,repositories} auth-service/{bin,config}
mkdir -p catalog-service/src/{controllers,models,repositories} catalog-service/{bin,config}
mkdir -p order-service/src/{controllers,models,repositories} order-service/{bin,config}
mkdir -p user-service/src/{controllers,models,repositories} user-service/{bin,config}
mkdir -p gateway/{src,bin,config}
mkdir -p utils/{src,bin}
mkdir -p {db,config,lib,bin,test,build}

# Copie des fichiers depuis le monolithe Javalin
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/controllers/AuthController.java" auth-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/controllers/ApplicationController.java" auth-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/models/User.java" auth-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/repositories/UserRepository.java" auth-service/src/repositories/

cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/controllers/PlantController.java" catalog-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/controllers/ApplicationController.java" catalog-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/models/Plant.java" catalog-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/repositories/PlantRepository.java" catalog-service/src/repositories/

cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/controllers/OrderController.java" order-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/controllers/ApplicationController.java" order-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/models/Order.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/models/OrderItem.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/repositories/OrderRepository.java" order-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/repositories/OrderItemRepository.java" order-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/repositories/PlantRepository.java" order-service/src/repositories/

cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/controllers/UserController.java" user-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/controllers/ApplicationController.java" user-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/models/User.java" user-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/repositories/UserRepository.java" user-service/src/repositories/

# Copie du gateway et utils
cp -r "$BASE_DIR/Structure_en_application_distribuée/plant_shop_java_javalin_distribuée/gateway/"* gateway/ 2>/dev/null || \
cp -r "$BASE_DIR/Structure_en_application_distribuée/plant_shop_java_distribuée/gateway/"* gateway/ 2>/dev/null || true
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/utils/"*.java utils/src/ 2>/dev/null || true

# Copie des fichiers communs
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/db/schema.sql" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/db/Seed.java" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/config/libs.yaml" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/config/InstallLibs.java" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/Makefile" ./
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/test/Test.java" test/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/users.txt" ./ 2>/dev/null || true

touch backend_java_javalin_distribuée.txt
echo "✓ Terminé"


################################################################################
# 6. Spring Boot avec Hibernate - Architecture Microservices
################################################################################

echo ""
echo "=== 6/10 Spring Boot avec Hibernate - Architecture Microservices ==="
cd "$BASE_DIR/Structure_en_micro_services/plant_shop_java_spring_microservices/plant_shop_java_spring_boot_security_hibernate_microservices" || exit 1

# Création de l'arborescence
mkdir -p auth-service/src/{controllers,models,repositories,security} auth-service/{db,bin,config}
mkdir -p catalog-service/src/{controllers,models,repositories} catalog-service/{db,bin,config}
mkdir -p order-service/src/{controllers,models,repositories} order-service/{db,bin,config}
mkdir -p user-service/src/{controllers,models,repositories} user-service/{db,bin,config}
mkdir -p gateway/{src,bin,config}
mkdir -p utils/{src,bin}
mkdir -p {db,config,lib,bin,test,build}

# Copie des fichiers depuis le monolithe Spring Boot Hibernate
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/controllers/AuthController.java" auth-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/models/User.java" auth-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/repositories/UserRepository.java" auth-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/security/SecurityConfig.java" auth-service/src/security/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/controllers/PlantController.java" catalog-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/models/Plant.java" catalog-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/repositories/PlantRepository.java" catalog-service/src/repositories/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/controllers/OrderController.java" order-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/controllers/OrderItemController.java" order-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/models/Order.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/models/OrderItem.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/repositories/OrderRepository.java" order-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/repositories/OrderItemRepository.java" order-service/src/repositories/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/controllers/UserController.java" user-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/models/User.java" user-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/repositories/UserRepository.java" user-service/src/repositories/

# Copie du gateway et utils
cp -r "$BASE_DIR/Structure_en_micro_services/plant_shop_java_microservices/gateway/"* gateway/ 2>/dev/null || true
cp -r "$BASE_DIR/Structure_en_micro_services/plant_shop_java_microservices/utils/"* utils/ 2>/dev/null || true

# Copie des fichiers de DB pour chaque service (microservices = DB par service)
for service in auth-service catalog-service order-service user-service; do
    cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/db/schema.sql" $service/db/
    cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/db/Seed.java" $service/db/
done

# Copie des fichiers communs (db global de référence)
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/db/schema.sql" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/db/Seed.java" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/config/dependencies.txt" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/config/InstallCoursier.java" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/Makefile" ./
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/test/Test.java" test/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/users.txt" ./ 2>/dev/null || true

# Création du modèle PlantStock pour order-service
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security_hibernate/src/models/Plant.java" order-service/src/models/PlantStock.java

touch backend_java_spring_hibernate_microservices.txt
echo "✓ Terminé"


################################################################################
# 7. Spring Boot sans Hibernate - Architecture Microservices
################################################################################

echo ""
echo "=== 7/10 Spring Boot sans Hibernate - Architecture Microservices ==="
cd "$BASE_DIR/Structure_en_micro_services/plant_shop_java_spring_microservices/plant_shop_java_spring_boot_security_microservices" || exit 1

# Création de l'arborescence
mkdir -p auth-service/src/{controllers,models,repositories,security} auth-service/{db,bin,config}
mkdir -p catalog-service/src/{controllers,models,repositories} catalog-service/{db,bin,config}
mkdir -p order-service/src/{controllers,models,repositories} order-service/{db,bin,config}
mkdir -p user-service/src/{controllers,models,repositories} user-service/{db,bin,config}
mkdir -p gateway/{src,bin,config}
mkdir -p utils/{src,bin}
mkdir -p {db,config,lib,bin,test,build}

# Copie des fichiers depuis le monolithe Spring Boot sans Hibernate
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/controllers/AuthController.java" auth-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/models/User.java" auth-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/repositories/UserRepository.java" auth-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/security/SecurityConfig.java" auth-service/src/security/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/controllers/PlantController.java" catalog-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/models/Plant.java" catalog-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/repositories/PlantRepository.java" catalog-service/src/repositories/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/controllers/OrderController.java" order-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/controllers/OrderItemController.java" order-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/models/Order.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/models/OrderItem.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/repositories/OrderRepository.java" order-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/repositories/OrderItemRepository.java" order-service/src/repositories/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/controllers/UserController.java" user-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/models/User.java" user-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/repositories/UserRepository.java" user-service/src/repositories/

# Copie du gateway et utils
cp -r "$BASE_DIR/Structure_en_micro_services/plant_shop_java_microservices/gateway/"* gateway/ 2>/dev/null || true
cp -r "$BASE_DIR/Structure_en_micro_services/plant_shop_java_microservices/utils/"* utils/ 2>/dev/null || true

# Copie des fichiers de DB pour chaque service
for service in auth-service catalog-service order-service user-service; do
    cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/db/schema.sql" $service/db/
    cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/db/Seed.java" $service/db/
done

# Copie des fichiers communs
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/db/schema.sql" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/db/Seed.java" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/config/dependencies.txt" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/config/InstallCoursier.java" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/Makefile" ./
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/test/Test.java" test/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/users.txt" ./ 2>/dev/null || true

# Création du modèle PlantStock pour order-service
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_spring/plant_shop_java_spring_boot_security/src/models/Plant.java" order-service/src/models/PlantStock.java

touch backend_java_spring_microservices.txt
echo "✓ Terminé"


################################################################################
# 8. Micronaut - Architecture Microservices
################################################################################

echo ""
echo "=== 8/10 Micronaut - Architecture Microservices ==="
cd "$BASE_DIR/Structure_en_micro_services/plant_shop_java_micronaut_microservices" || exit 1

# Création de l'arborescence
mkdir -p auth-service/src/{controllers,models,repositories,security} auth-service/{db,bin,config}
mkdir -p catalog-service/src/{controllers,models,repositories} catalog-service/{db,bin,config}
mkdir -p order-service/src/{controllers,models,repositories} order-service/{db,bin,config}
mkdir -p user-service/src/{controllers,models,repositories} user-service/{db,bin,config}
mkdir -p gateway/{src,bin,config}
mkdir -p utils/{src,bin}
mkdir -p {db,config,lib,bin,test,build}

# Copie des fichiers depuis le monolithe Micronaut
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/controllers/AuthController.java" auth-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/models/User.java" auth-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/repositories/UserRepository.java" auth-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/security/"*.java auth-service/src/security/ 2>/dev/null || true

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/controllers/PlantController.java" catalog-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/models/Plant.java" catalog-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/repositories/PlantRepository.java" catalog-service/src/repositories/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/controllers/OrderController.java" order-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/models/Order.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/models/OrderItem.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/repositories/OrderRepository.java" order-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/repositories/OrderItemRepository.java" order-service/src/repositories/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/controllers/UserController.java" user-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/models/User.java" user-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/repositories/UserRepository.java" user-service/src/repositories/

# Copie du gateway et utils
cp -r "$BASE_DIR/Structure_en_micro_services/plant_shop_java_micronaut_microservices/gateway/"* gateway/ 2>/dev/null || \
cp -r "$BASE_DIR/Structure_en_micro_services/plant_shop_java_microservices/gateway/"* gateway/ 2>/dev/null || true

cp -r "$BASE_DIR/Structure_en_micro_services/plant_shop_java_micronaut_microservices/utils/"* utils/ 2>/dev/null || \
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/utils/"*.java utils/src/ 2>/dev/null || true

# Copie des fichiers de DB pour chaque service
for service in auth-service catalog-service order-service user-service; do
    cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/db/schema.sql" $service/db/
    cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/db/Seed.java" $service/db/
done

# Copie des fichiers communs
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/db/schema.sql" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/db/Seed.java" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/config/dependencies.txt" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/config/InstallCoursier.java" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/Makefile" ./
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/test/Test.java" test/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/users.txt" ./ 2>/dev/null || true

# Création du modèle PlantStock pour order-service
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_micronaut/src/models/Plant.java" order-service/src/models/PlantStock.java

touch backend_java_micronaut_microservices.txt
echo "✓ Terminé"


################################################################################
# 9. Quarkus - Architecture Microservices
################################################################################

echo ""
echo "=== 9/10 Quarkus - Architecture Microservices ==="
cd "$BASE_DIR/Structure_en_micro_services/plant_shop_java_quarkus_microservices" || exit 1

# Création de l'arborescence
mkdir -p auth-service/src/{controllers,models,repositories,security,META-INF} auth-service/{db,bin,config}
mkdir -p catalog-service/src/{controllers,models,repositories,META-INF} catalog-service/{db,bin,config}
mkdir -p order-service/src/{controllers,models,repositories,META-INF} order-service/{db,bin,config}
mkdir -p user-service/src/{controllers,models,repositories,META-INF} user-service/{db,bin,config}
mkdir -p gateway/src/META-INF gateway/{bin,config}
mkdir -p utils/{src,bin}
mkdir -p {db,config,lib,bin,test,build,quarkus}

# Copie des fichiers depuis le monolithe Quarkus
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/controllers/AuthController.java" auth-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/models/User.java" auth-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/repositories/UserRepository.java" auth-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/security/"*.java auth-service/src/security/ 2>/dev/null || true
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/META-INF/beans.xml" auth-service/src/META-INF/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/controllers/PlantController.java" catalog-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/models/Plant.java" catalog-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/repositories/PlantRepository.java" catalog-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/META-INF/beans.xml" catalog-service/src/META-INF/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/controllers/OrderController.java" order-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/models/Order.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/models/OrderItem.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/repositories/OrderRepository.java" order-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/repositories/OrderItemRepository.java" order-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/META-INF/beans.xml" order-service/src/META-INF/

cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/controllers/UserController.java" user-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/models/User.java" user-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/repositories/UserRepository.java" user-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/META-INF/beans.xml" user-service/src/META-INF/

# Copie du gateway
cp -r "$BASE_DIR/Structure_en_micro_services/plant_shop_java_quarkus_microservices/gateway/"* gateway/ 2>/dev/null || \
cp -r "$BASE_DIR/Structure_en_micro_services/plant_shop_java_microservices/gateway/"* gateway/ 2>/dev/null || true
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/META-INF/beans.xml" gateway/src/META-INF/ 2>/dev/null || true

# Copie des utils
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/utils/"*.java utils/src/ 2>/dev/null || true

# Copie des fichiers de DB pour chaque service
for service in auth-service catalog-service order-service user-service; do
    cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/db/schema.sql" $service/db/
    cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/db/Seed.java" $service/db/
done

# Copie des fichiers communs
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/db/schema.sql" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/db/Seed.java" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/config/dependencies.txt" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/config/InstallCoursier.java" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/Makefile" ./
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/test/Test.java" test/
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/users.txt" ./ 2>/dev/null || true
cp -r "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/quarkus/"* quarkus/ 2>/dev/null || true

# Création du modèle PlantStock pour order-service
cp "$BASE_DIR/Structure_monolithique/plant_shop_java_quarkus/src/models/Plant.java" order-service/src/models/PlantStock.java

touch backend_java_quarkus_microservices.txt
echo "✓ Terminé"


################################################################################
# 10. Javalin - Architecture Microservices
################################################################################

echo ""
echo "=== 10/10 Javalin - Architecture Microservices ==="
cd "$BASE_DIR/Structure_en_micro_services/plant_shop_java_javalin_microservices" || exit 1

# Création de l'arborescence
mkdir -p auth-service/src/{controllers,models,repositories} auth-service/{db,bin,config}
mkdir -p catalog-service/src/{controllers,models,repositories} catalog-service/{db,bin,config}
mkdir -p order-service/src/{controllers,models,repositories} order-service/{db,bin,config}
mkdir -p user-service/src/{controllers,models,repositories} user-service/{db,bin,config}
mkdir -p gateway/{src,bin,config}
mkdir -p utils/{src,bin}
mkdir -p {db,config,lib,bin,test,build}

# Copie des fichiers depuis le monolithe Javalin
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/controllers/AuthController.java" auth-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/controllers/ApplicationController.java" auth-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/models/User.java" auth-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/repositories/UserRepository.java" auth-service/src/repositories/

cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/controllers/PlantController.java" catalog-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/controllers/ApplicationController.java" catalog-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/models/Plant.java" catalog-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/repositories/PlantRepository.java" catalog-service/src/repositories/

cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/controllers/OrderController.java" order-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/controllers/ApplicationController.java" order-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/models/Order.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/models/OrderItem.java" order-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/repositories/OrderRepository.java" order-service/src/repositories/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/repositories/OrderItemRepository.java" order-service/src/repositories/

cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/controllers/UserController.java" user-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/controllers/ApplicationController.java" user-service/src/controllers/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/models/User.java" user-service/src/models/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/repositories/UserRepository.java" user-service/src/repositories/

# Copie du gateway et utils
cp -r "$BASE_DIR/Structure_en_micro_services/plant_shop_java_jalavin_microservices/gateway/"* gateway/ 2>/dev/null || \
cp -r "$BASE_DIR/Structure_en_micro_services/plant_shop_java_microservices/gateway/"* gateway/ 2>/dev/null || true
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/utils/"*.java utils/src/ 2>/dev/null || true

# Copie des fichiers de DB pour chaque service
for service in auth-service catalog-service order-service user-service; do
    cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/db/schema.sql" $service/db/
    cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/db/Seed.java" $service/db/
done

# Copie des fichiers communs
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/db/schema.sql" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/db/Seed.java" db/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/config/libs.yaml" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/config/InstallLibs.java" config/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/Makefile" ./
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/test/Test.java" test/
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/users.txt" ./ 2>/dev/null || true

# Création du modèle PlantStock pour order-service
cp "$BASE_DIR/Structure_monolithique/plant_shop_javalin/src/models/Plant.java" order-service/src/models/PlantStock.java

touch backend_java_javalin_microservices.txt
echo "✓ Terminé"


################################################################################
# Résumé final
################################################################################

echo ""
echo "========================================================================"
echo "✓ Toutes les arborescences ont été créées avec succès!"
echo "========================================================================"
echo ""
echo "10 projets créés :"
echo "  • 5 architectures distribuées"
echo "  • 5 architectures microservices"
echo ""
echo "Exécuté depuis : $BASE_DIR"
echo "========================================================================"
