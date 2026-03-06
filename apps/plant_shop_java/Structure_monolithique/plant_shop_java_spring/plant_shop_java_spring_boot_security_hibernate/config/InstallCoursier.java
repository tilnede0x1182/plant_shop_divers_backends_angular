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

    /**
     * Point d'entrée du programme d'installation des dépendances.
     * @param args String[] Arguments de ligne de commande
     */
    public static void main(String[] args) {
        try {
            // System.out.println("📦 Installation des dépendances via Coursier...");
            ensureCs();
            Files.createDirectories(Paths.get(LIB_DIR));
            List<String> deps = Files.readAllLines(Paths.get(DEP_FILE)).stream().map(String::trim).toList();
						for (String raw : deps) {
								if (raw.isEmpty() || raw.startsWith("#")) continue;
								String[] p = raw.split(":");
								if (p.length >= 3) {
										Path libJar = Paths.get(LIB_DIR, p[1] + "-" + p[2] + ".jar");
										if (Files.exists(libJar)) { System.out.println("ℹ️  Existe, skip : " + libJar.getFileName()); continue; }
								}
								System.out.println("➡️  Téléchargement : " + raw);
								List<String> jars = fetchJarPathsFor(raw);
								if (jars.isEmpty()) System.out.println("⚠️  Aucun .jar trouvé pour : " + raw);
								for (String jar : jars) copyJarImmediate(jar);
						}
						cleanObsoleteJars(deps);
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
            // System.out.println("✔️  Coursier trouvé.");
            return;
        } catch (IOException ignored) {
            System.out.println("➡️  Coursier non trouvé. Installation locale...");
        }
        runAndCollect("bash", "-c", "curl -fLo cs " + CS_URL + " && chmod +x cs && sudo mv cs /usr/local/bin/");
        runAndCollect("cs", "--version");
        System.out.println("✔️  Coursier installé.");
    }

		/**
		 * Récupère les chemins .jar pour une dépendance.
		 * Si l'artifact principal (artifact-version.jar) existe déjà dans ./lib,
		 * évite d'appeler `cs fetch` et retourne une liste vide.
		 */
		private static List<String> fetchJarPathsFor(String dep) throws IOException, InterruptedException {
				String[] parts = dep.split(":");
				if (parts.length >= 3) {
						Path p = Paths.get(LIB_DIR, parts[1] + "-" + parts[2] + ".jar");
						if (Files.exists(p)) { System.out.println("ℹ️  Existe localement, skip fetch : " + p.getFileName()); return java.util.Collections.emptyList(); }
				}
				List<String> out = runAndCollect("cs", "fetch", dep, "-p");
				String all = String.join("\n", out);
				List<String> jars = new ArrayList<>();
				for (String token : all.split("\\s+|:")) if (token.trim().endsWith(".jar")) jars.add(token.trim());
				return jars;
		}

    /**
	 * Copie immédiatement le jar depuis le cache vers ./lib si absent.
	 * Affiche une ligne une fois la copie réalisée ou si le fichier est ignoré.
	 * @param jarPath String Chemin vers le fichier JAR
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

		/**
	 * Supprime les .jar obsolètes dans ./lib qui ne sont plus référencés
	 * par les dépendances actives ni leurs dépendances transitives.
	 * @param deps List<String> Liste des dépendances actives
	 */
		private static void cleanObsoleteJars(List<String> deps) throws IOException, InterruptedException {
				boolean didDelete = false;

				// 1. Obtenir le classpath complet via coursier
				List<String> lines = new ArrayList<>();
				lines.addAll(runAndCollect("bash", "-c",
						"cs fetch -p $(grep -v '^#' " + DEP_FILE + " | grep -v '^$' | tr '\n' ' ')"));
				String joined = String.join(" ", lines);
				String[] parts = joined.split(":");
				// 2. Construire un set des noms de fichiers valides
				var valid = new java.util.HashSet<String>();
				for (String s : parts) {
						if (s.trim().endsWith(".jar")) {
								valid.add(Paths.get(s.trim()).getFileName().toString());
						}
				}

				// 2.1 Vérifier s'il existe au moins un .jar à supprimer
				try (var stream = Files.list(Paths.get(LIB_DIR))) {
						didDelete = stream.filter(p -> p.toString().endsWith(".jar"))
								.anyMatch(jar -> !valid.contains(jar.getFileName().toString()));
				}

				if (didDelete) System.out.println("🧹 Nettoyage des dépendances obsolètes...");

				// 3. Parcourir ./lib et supprimer ce qui n’est plus valide
				try (var stream = Files.list(Paths.get(LIB_DIR))) {
						stream.filter(p -> p.toString().endsWith(".jar"))
									.forEach(jar -> {
											String name = jar.getFileName().toString();
											if (!valid.contains(name)) {
													try {
															Files.delete(jar);
															System.out.println("🗑️  Supprimé : " + name);
													} catch (IOException e) {
															System.err.println("⚠️  Impossible de supprimer " + name + " : " + e.getMessage());
													}
											}
									});
				}
				if (didDelete) System.out.println("✅ Nettoyage terminé.");
		}
}
