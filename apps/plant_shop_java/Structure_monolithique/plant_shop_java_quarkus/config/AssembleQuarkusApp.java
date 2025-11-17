package config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Assemble un bundle exécutable (bin/quarkus-app) en copiant les classes compilées
 * et les dépendances installées via Coursier. Cela simule la sortie d'un build Maven/Gradle.
 */
public final class AssembleQuarkusApp {

    private static final Path BIN_DIR = Paths.get("bin");
    private static final Path LIB_DIR = Paths.get("lib");
    private static final Path OUTPUT_DIR = BIN_DIR.resolve("quarkus-app");

    public static void main(String[] args) {
        try {
            ensureCompiled();
            cleanOutput();
            Files.createDirectories(OUTPUT_DIR.resolve("lib"));

            List<String> classPath = copyLibs();
            Path appJar = OUTPUT_DIR.resolve("app.jar");
            createAppJar(appJar, classPath);

            System.out.println("✅ Bundle Quarkus prêt dans bin/quarkus-app (java -jar bin/quarkus-app/app.jar)");
        } catch (Exception e) {
            System.err.println("❌ Échec de l'assemblage Quarkus : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void ensureCompiled() throws IOException {
        if (!Files.exists(BIN_DIR) || Files.list(BIN_DIR).findAny().isEmpty()) {
            throw new IllegalStateException("Aucune classe compilée dans bin/. Lancez 'make compile' avant.");
        }
    }

    private static void cleanOutput() throws IOException {
        if (!Files.exists(OUTPUT_DIR)) {
            return;
        }
        try (var paths = Files.walk(OUTPUT_DIR)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new RuntimeException("Impossible de supprimer " + path + ": " + e.getMessage(), e);
                }
            });
        }
    }

    private static List<String> copyLibs() throws IOException {
        if (!Files.exists(LIB_DIR)) {
            throw new IllegalStateException("Le dossier lib/ est introuvable. Exécutez 'make install'.");
        }
        List<String> classPath = new ArrayList<>();
        try (var stream = Files.list(LIB_DIR)) {
            stream.filter(path -> path.toString().endsWith(".jar"))
                .forEach(path -> {
                    Path target = OUTPUT_DIR.resolve("lib").resolve(path.getFileName().toString());
                    try {
                        Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                        classPath.add("lib/" + target.getFileName());
                    } catch (IOException e) {
                        throw new RuntimeException("Copie impossible pour " + path + ": " + e.getMessage(), e);
                    }
                });
        }
        if (classPath.isEmpty()) {
            throw new IllegalStateException("Aucune dépendance copiée. Vérifiez lib/.");
        }
        return classPath;
    }

    private static void createAppJar(Path jarPath, List<String> classPathEntries) throws IOException {
        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.put(Attributes.Name.MAIN_CLASS, "Main");
        attrs.put(Attributes.Name.CLASS_PATH, String.join(" ", classPathEntries));

        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath), manifest)) {
            Path root = BIN_DIR;
            Files.walk(root)
                .filter(path -> Files.isRegularFile(path))
                .filter(path -> !path.startsWith(OUTPUT_DIR))
                .forEach(path -> addEntry(jar, root, path));
        }
    }

    private static void addEntry(JarOutputStream jar, Path root, Path file) {
        try {
            String entryName = root.relativize(file).toString().replace('\\', '/');
            JarEntry entry = new JarEntry(entryName);
            jar.putNextEntry(entry);
            try (InputStream in = Files.newInputStream(file)) {
                in.transferTo(jar);
            }
            jar.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ajouter " + file + " au jar : " + e.getMessage(), e);
        }
    }
}
