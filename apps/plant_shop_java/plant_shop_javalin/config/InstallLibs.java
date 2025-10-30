import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class InstallLibs {
    public static void main(String[] args) throws Exception {
        Path yaml = Path.of("libs.yaml");
        if (!Files.exists(yaml)) {
            System.err.println("Fichier libs.yaml introuvable.");
            return;
        }

        List<String> deps = new ArrayList<>();
        for (String line : Files.readAllLines(yaml)) {
            line = line.trim();
            if (line.startsWith("- ")) deps.add(line.substring(2));
        }

        if (deps.isEmpty()) {
            System.out.println("Aucune dépendance trouvée.");
            return;
        }

        Files.createDirectories(Path.of("lib"));
        Pattern p = Pattern.compile("([^:]+):([^:]+):([^:]+)");

        for (String dep : deps) {
            Matcher m = p.matcher(dep);
            if (!m.matches()) continue;

            String group = m.group(1).replace('.', '/');
            String artifact = m.group(2);
            String version = m.group(3);
            String url = String.format(
                "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar",
                group, artifact, version, artifact, version
            );

            Path out = Path.of("lib", artifact + "-" + version + ".jar");
            if (Files.exists(out)) {
                System.out.println(out.getFileName() + " déjà présent.");
                continue;
            }

            System.out.println("Téléchargement : " + out.getFileName());
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, out);
            }
        }

        System.out.println("Toutes les dépendances sont dans ./lib");
    }
}
