package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Prépare un workspace Maven complet dans bin/maven-workspace/,
 * lance quarkus:build et dépose le fast-jar dans .quarkus/quarkus-app.
 */
public final class QuarkusBundleBuilder {

    private static final String QUARKUS_VERSION = "3.10.0";
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();
    private static final Path BIN_DIR = PROJECT_ROOT.resolve("bin");
    private static final Path WORKSPACE_DIR = BIN_DIR.resolve("maven-workspace");
    private static final Path BUNDLE_DIR = PROJECT_ROOT.resolve(".quarkus");
    private static final Path QUARKUS_APP_DIR = BUNDLE_DIR.resolve("quarkus-app");
    private static final Path DEP_FILE = PROJECT_ROOT.resolve("config").resolve("dependencies.txt");
    private static final Path POM_FILE = WORKSPACE_DIR.resolve("pom.xml");

    /**
     * Record représentant une dépendance Maven.
     * @param groupId String Groupe Maven
     * @param artifactId String Artefact Maven
     * @param version String Version
     */
    private record Dependency(String groupId, String artifactId, String version) {}

    /**
     * Point d'entrée du builder.
     * @param args String[] Arguments
     */
    public static void main(String[] args) {
        try {
            buildBundle();
        } catch (Exception e) {
            System.err.println("❌ Impossible de générer le bundle Quarkus : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Construit le bundle Quarkus.
     * @throws IOException En cas d'erreur I/O
     * @throws InterruptedException En cas d'interruption
     */
    public static void buildBundle() throws IOException, InterruptedException {
        List<Dependency> dependencies = readDependencies();
        prepareWorkspace();
        writePom(dependencies);
        cleanDirectory(QUARKUS_APP_DIR);
        runMavenBuild();
        verifyBundle();
        System.out.println("✅ Bundle Quarkus régénéré dans .quarkus/quarkus-app");
    }

    /**
     * Lit les dépendances depuis le fichier de configuration.
     * @return List Liste des dépendances
     * @throws IOException En cas d'erreur de lecture
     */
    private static List<Dependency> readDependencies() throws IOException {
        if (!Files.exists(DEP_FILE)) {
            throw new IllegalStateException("config/dependencies.txt introuvable");
        }
        List<Dependency> deps = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(DEP_FILE, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split(":");
                if (parts.length < 3) {
                    throw new IllegalArgumentException("Entrée de dépendance invalide : " + trimmed);
                }
                deps.add(new Dependency(parts[0], parts[1], parts[2]));
            }
        }
        return deps;
    }

    /**
     * Prépare le workspace Maven.
     * @throws IOException En cas d'erreur I/O
     */
    /**
     * Prépare le workspace Maven.
     * @throws IOException En cas d'erreur I/O
     */
    private static void prepareWorkspace() throws IOException {
        cleanDirectory(WORKSPACE_DIR);
        Files.createDirectories(WORKSPACE_DIR);
        copyDirectory(PROJECT_ROOT.resolve("src"), WORKSPACE_DIR.resolve("src"));
    }

    /**
     * Copie un répertoire récursivement.
     * @param source Path Répertoire source
     * @param target Path Répertoire cible
     * @throws IOException En cas d'erreur I/O
     */
    /**
     * Copie un répertoire récursivement.
     * @param source Path Répertoire source
     * @param target Path Répertoire cible
     * @throws IOException En cas d'erreur I/O
     */
    private static void copyDirectory(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            return;
        }
        Files.walk(source).forEach(path -> {
            try {
                Path relative = source.relativize(path);
                Path dest = target.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException("Impossible de copier " + path + ": " + e.getMessage(), e);
            }
        });
    }

    /**
     * Écrit le fichier pom.xml.
     * @param deps List Dépendances à inclure
     * @throws IOException En cas d'erreur I/O
     */
    /**
     * Écrit le fichier pom.xml.
     * @param deps List Dépendances à inclure
     * @throws IOException En cas d'erreur I/O
     */
    private static void writePom(List<Dependency> deps) throws IOException {
        Files.createDirectories(WORKSPACE_DIR);
        try (BufferedWriter writer = Files.newBufferedWriter(POM_FILE,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(pomHeader());
            for (Dependency dep : deps) {
                writer.write("        <dependency>\n");
                writer.write("            <groupId>" + dep.groupId + "</groupId>\n");
                writer.write("            <artifactId>" + dep.artifactId + "</artifactId>\n");
                writer.write("            <version>" + dep.version + "</version>\n");
                writer.write("        </dependency>\n");
            }
            writer.write(pomFooter());
        }
    }

    /**
     * Génère l'en-tête du pom.xml.
     * @return String En-tête XML
     */
    /**
     * Génère l'en-tête du pom.xml.
     * @return String En-tête XML
     */
    private static String pomHeader() {
        return """
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>plant.shop</groupId>
    <artifactId>plant-shop-quarkus-temporary</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <quarkus.platform.version>%s</quarkus.platform.version>
        <quarkus.plugin.version>%s</quarkus.plugin.version>
    </properties>
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.quarkus.platform</groupId>
                <artifactId>quarkus-bom</artifactId>
                <version>${quarkus.platform.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    <dependencies>
""".formatted(QUARKUS_VERSION, QUARKUS_VERSION);
    }

    /**
     * Génère le pied du pom.xml.
     * @return String Pied XML
     */
    private static String pomFooter() {
        return """
    </dependencies>
    <build>
        <sourceDirectory>src</sourceDirectory>
        <resources>
            <resource>
                <directory>src/resources</directory>
            </resource>
            <resource>
                <directory>src/META-INF</directory>
            </resource>
        </resources>
        <plugins>
            <plugin>
                <groupId>io.quarkus.platform</groupId>
                <artifactId>quarkus-maven-plugin</artifactId>
                <version>${quarkus.plugin.version}</version>
                <extensions>true</extensions>
                <executions>
                    <execution>
                        <goals>
                            <goal>build</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
""";
    }

    /**
     * Supprime un répertoire et son contenu.
     * @param dir Path Répertoire à supprimer
     * @throws IOException En cas d'erreur I/O
     */
    private static void cleanDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new RuntimeException("Impossible de supprimer " + path + ": " + e.getMessage(), e);
                    }
                });
        }
    }

    /**
     * Lance le build Maven.
     * @throws IOException En cas d'erreur I/O
     * @throws InterruptedException En cas d'interruption
     */
    private static void runMavenBuild() throws IOException, InterruptedException {
        String outputDir = QUARKUS_APP_DIR.toAbsolutePath().toString();
        ProcessBuilder pb = new ProcessBuilder(
            "mvn",
            "-B",
            "-q",
            "-f",
            "pom.xml",
            "clean",
            "package",
            "-DskipTests",
            "-Dquarkus.package.type=fast-jar",
            "-Dquarkus.package.output-directory=" + outputDir
        );
        pb.directory(WORKSPACE_DIR.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (InputStream in = process.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("mvn a échoué (code=" + exit + ")");
        }
    }

    /**
     * Vérifie que le bundle a été généré.
     */
    private static void verifyBundle() {
        Path runner = QUARKUS_APP_DIR.resolve("quarkus-run.jar");
        if (!Files.exists(runner)) {
            throw new IllegalStateException("quarkus-run.jar introuvable dans " + runner);
        }
    }
}
