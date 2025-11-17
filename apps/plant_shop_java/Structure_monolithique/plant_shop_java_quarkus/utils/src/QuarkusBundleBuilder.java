package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Génère un pom temporaire dans bin/, lance mvn quarkus:build et copie le bundle
 * dans bin/quarkus-app, sans dépendre d'une structure Maven permanente.
 */
public final class QuarkusBundleBuilder {

    private static final String QUARKUS_VERSION = "3.10.0";
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();
    private static final Path BUNDLE_DIR = PROJECT_ROOT.resolve(".quarkus");
    private static final Path BIN_DIR = PROJECT_ROOT.resolve("bin");
    private static final Path BUILD_DIR = BUNDLE_DIR.resolve("maven-build");
    private static final Path QUARKUS_APP_DIR = BUNDLE_DIR.resolve("quarkus-app");
    private static final Path DEP_FILE = PROJECT_ROOT.resolve("config").resolve("dependencies.txt");
    private static final Path POM_FILE = BUILD_DIR.resolve("pom.generated.xml");

    private record Dependency(String groupId, String artifactId, String version) {}

    public static void main(String[] args) {
        try {
            List<Dependency> dependencies = readDependencies();
            Files.createDirectories(BUILD_DIR);
            Files.createDirectories(QUARKUS_APP_DIR.getParent());
            writePom(dependencies);
            cleanDirectory(QUARKUS_APP_DIR);
            runMavenBuild();
            verifyBundle();
            System.out.println("✅ Bundle Quarkus régénéré dans .quarkus/quarkus-app");
        } catch (Exception e) {
            System.err.println("❌ Impossible de générer le bundle Quarkus : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

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

    private static void writePom(List<Dependency> deps) throws IOException {
        Files.createDirectories(BUILD_DIR);
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

    private static String pomHeader() {
        return """
<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<project xmlns=\"http://maven.apache.org/POM/4.0.0\"
         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"
         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">
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

    private static String pomFooter() {
        return """
    </dependencies>
    <build>
        <sourceDirectory>../../src</sourceDirectory>
        <resources>
            <resource>
                <directory>../../src/resources</directory>
            </resource>
            <resource>
                <directory>../../src/META-INF</directory>
                <includes>
                    <include>**/*</include>
                </includes>
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

    private static void cleanDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            stream.sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new RuntimeException("Impossible de supprimer " + path + ": " + e.getMessage(), e);
                    }
                });
        }
    }

    private static void runMavenBuild() throws IOException, InterruptedException {
        String outputDir = QUARKUS_APP_DIR.toAbsolutePath().toString();
        ProcessBuilder pb = new ProcessBuilder(
            "mvn",
            "-B",
            "-q",
            "-f",
            POM_FILE.getFileName().toString(),
            "clean",
            "package",
            "-DskipTests",
            "-Dquarkus.package.type=fast-jar",
            "-Dquarkus.package.output-directory=" + outputDir
        );
        pb.directory(BUILD_DIR.toFile());
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

    private static void verifyBundle() {
        Path runner = QUARKUS_APP_DIR.resolve("quarkus-run.jar");
        if (!Files.exists(runner)) {
            throw new IllegalStateException("quarkus-run.jar introuvable dans " + runner);
        }
    }
}
