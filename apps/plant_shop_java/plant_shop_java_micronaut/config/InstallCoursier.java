package config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Installe (si nécessaire) l'outil Coursier et récupère toutes les dépendances
 * listées dans config/dependencies.txt vers ./lib.
 *
 * Comportement :
 *  - affiche des messages clairs et emoji.
 *  - pour chaque dépendance, lance `cs fetch <dep> -p`, attend la fin,
 *    extrait les chemins vers les .jar, copie immédiatement chaque .jar
 *    vers ./lib (skip si déjà présent) et affiche une ligne quand la copie
 *    est terminée.
 */
public class InstallCoursier {

    private static final String DEP_FILE = "config/dependencies.txt";
    private static final String LIB_DIR = "lib";
    private static final String CS_URL = "https://git.io/coursier-cli";

    public static void main(String[] args) {
        try {
            System.out.println("📦 Installation des dépendances via Coursier...");
            ensureCs();
            Files.createDirectories(Paths.get(LIB_DIR));
            List<String> deps = Files.readAllLines(Paths.get(DEP_FILE)).stream().map(String::trim).toList();
            for (String raw : deps) {
                if (raw.isEmpty() || raw.startsWith("#")) continue;
                System.out.println("➡️  Téléchargement : " + raw);
                List<String> jars = fetchJarPathsFor(raw);
                if (jars.isEmpty()) {
                    System.out.println("⚠️  Aucun .jar trouvé pour : " + raw);
                }
                for (String jar : jars) {
                    copyJarImmediate(jar);
                }
            }
            System.out.println("✅ Dépendances installées dans ./lib");
        } catch (Exception e) {
            System.err.println("❌ Échec : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Vérifie la présence de 'cs'. Si absent, tente l'installation système.
     */
    private static void ensureCs() throws IOException, InterruptedException {
        try {
            runAndCollect("cs", "--version");
            System.out.println("✔️  Coursier trouvé.");
            return;
        } catch (IOException ignored) {
            System.out.println("➡️  Coursier non trouvé. Installation locale...");
        }
        runAndCollect("bash", "-c", "curl -fLo cs " + CS_URL + " && chmod +x cs && sudo mv cs /usr/local/bin/");
        runAndCollect("cs", "--version");
        System.out.println("✔️  Coursier installé.");
    }

    /**
     * Exécute cs fetch <dep> -p, attend la fin puis extrait tous les tokens
     * qui se terminent par .jar (séparateurs : whitespace et ':').
     */
    private static List<String> fetchJarPathsFor(String dep) throws IOException, InterruptedException {
        List<String> out = runAndCollect("cs", "fetch", dep, "-p");
        StringBuilder sb = new StringBuilder();
        for (String s : out) sb.append(s).append('\n');
        String all = sb.toString();
        String[] tokens = all.split("\\s+|:"); // couvre les cas "Downloading ..." et classpath
        List<String> jars = new ArrayList<>();
        for (String t : tokens) {
            String s = t.trim();
            if (s.isEmpty()) continue;
            if (s.endsWith(".jar")) {
                jars.add(s);
            }
        }
        return jars;
    }

    /**
     * Copie immédiatement le jar depuis le cache vers ./lib si absent.
     * Affiche une ligne une fois la copie réalisée ou si le fichier est ignoré.
     */
    private static void copyJarImmediate(String jarPath) {
        try {
            Path src = Paths.get(jarPath);
            if (!Files.exists(src)) {
                System.out.println("⚠️  Chemin introuvable (skip) : " + jarPath);
                return;
            }
            Path dest = Paths.get(LIB_DIR, src.getFileName().toString());
            if (Files.exists(dest)) {
                System.out.println("ℹ️  Existe, skip : " + dest.getFileName());
                return;
            }
            Files.copy(src, dest, StandardCopyOption.COPY_ATTRIBUTES);
            System.out.println("✅ Copié : " + dest.getFileName());
        } catch (IOException e) {
            System.err.println("❌ Erreur copie : " + jarPath + " -> " + e.getMessage());
        }
    }

    /**
     * Lance une commande et renvoie sa sortie (stdout+stderr) sous forme de liste de lignes.
     * Lève IOException si le processus termine avec un code non nul.
     */
    private static List<String> runAndCollect(String... cmd) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        List<String> out = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) out.add(line);
        }
        int rc = p.waitFor();
        if (rc != 0) throw new IOException("Commande échouée (" + String.join(" ", cmd) + "), rc=" + rc
                + ". Sortie:\n" + String.join("\n", out));
        return out;
    }
}
