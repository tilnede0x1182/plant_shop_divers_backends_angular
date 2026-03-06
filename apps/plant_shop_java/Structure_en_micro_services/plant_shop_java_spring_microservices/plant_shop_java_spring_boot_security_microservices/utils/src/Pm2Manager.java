package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Gestionnaire de services via PM2 */
public final class Pm2Manager {

    private static final Map<String, Service> SERVICES = new LinkedHashMap<>();

    static {
        SERVICES.put("auth-service", new Service("auth-service", "auth-service", "app.AuthApplication"));
        SERVICES.put("catalog-service", new Service("catalog-service", "catalog-service", "app.CatalogApplication"));
        SERVICES.put("order-service", new Service("order-service", "order-service", "app.OrderApplication"));
        SERVICES.put("user-service", new Service("user-service", "user-service", "app.UserApplication"));
        SERVICES.put("gateway", new Service("gateway", "gateway", "Gateway"));
    }

    /** Point d'entree du gestionnaire PM2 */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }
        switch (args[0]) {
            case "start-all" -> startAll();
            case "stop-all" -> stopAll();
            case "stop-all-silent" -> stopAllSilent();
            case "start-all-with-logs" -> {
                requireArgs(args, 2);
                startAllWithLogs(Path.of(args[1]));
            }
            case "start" -> {
                requireArgs(args, 2);
                startOne(args[1]);
            }
            case "stop" -> {
                requireArgs(args, 2);
                stopOne(args[1]);
            }
            default -> {
                System.err.printf("Commande inconnue: %s%n", args[0]);
                printUsage();
            }
        }
    }

    /** Affiche l'aide d'utilisation */
    private static void printUsage() {
        System.out.println("""
            Usage: java util.Pm2Manager <commande>
              start-all       Démarre tous les services (via pm2)
              stop-all        Arrête tous les services gérés par pm2
              stop-all-silent Idem mais ignore les erreurs si un service est absent
              start-all-with-logs <dir>  Démarre tous les services avec sortie vers un dossier
              start <nom>     Démarre un service
              stop <nom>      Arrête un service

            Services disponibles: auth-service, catalog-service, order-service, user-service, gateway
            """);
    }

    /** Verifie le nombre d'arguments requis */
    private static void requireArgs(String[] args, int expected) {
        if (args.length < expected) {
            throw new IllegalArgumentException("Arguments insuffisants.");
        }
    }

    /** Demarre tous les services */
    private static void startAll() throws Exception {
        for (String name : SERVICES.keySet()) {
            startOne(name);
        }
    }

    /** Arrete tous les services */
    private static void stopAll() throws Exception {
        for (String name : SERVICES.keySet()) {
            stopOne(name);
        }
    }

    /** Demarre tous les services avec logs */
    private static void startAllWithLogs(Path dir) throws Exception {
        Path logDir = dir.toAbsolutePath();
        Files.createDirectories(logDir);
        System.out.printf("🗂️  Logs dans %s%n", logDir);
        for (String name : SERVICES.keySet()) {
            startOne(name, logDir);
        }
    }

    /** Arrete tous les services silencieusement */
    private static void stopAllSilent() {
        for (String name : SERVICES.keySet()) {
            try {
                stopOne(name, true);
            } catch (Exception e) {
                System.out.printf("⚠️  Impossible d'arrêter %s proprement (%s)%n", name, e.getMessage());
            }
        }
    }

    /** Demarre un service par son nom */
    private static void startOne(String name) throws Exception {
        startOne(name, null);
    }

    /** Demarre un service avec option de logs */
    private static void startOne(String name, Path logDir) throws Exception {
        Service service = SERVICES.get(name);
        if (service == null) {
            throw new IllegalArgumentException("Service inconnu: " + name);
        }
        deleteIfExists(service.name());
        List<String> command = new ArrayList<>(List.of(
            "pm2", "start", "java",
            "--name", service.name(),
            "--cwd", service.directory().toString()
        ));
        Path outLog = null;
        if (logDir != null) {
            outLog = logDir.resolve(service.name() + ".log");
            Path errLog = logDir.resolve(service.name() + ".err.log");
            command.add("--output");
            command.add(outLog.toString());
            command.add("--error");
            command.add(errLog.toString());
        }
        command.add("--");
        command.add("-cp");
        command.add(service.classpath());
        command.add(service.mainClass());
        runCommand(command);
        if (outLog != null) {
            System.out.printf("   ↳ %s → %s%n", service.name(), outLog);
        }
    }

    /** Arrete un service par son nom */
    private static void stopOne(String name) throws Exception {
        stopOne(name, false);
    }

    /** Arrete un service avec option silencieuse */
    private static void stopOne(String name, boolean quiet) throws Exception {
        Service service = SERVICES.get(name);
        if (service == null) {
            throw new IllegalArgumentException("Service inconnu: " + name);
        }
        if (!processExists(service.name())) {
            if (!quiet) {
                System.out.printf("ℹ️  pm2 ne gérait pas le service %s%n", name);
            }
            return;
        }
        runCommand(List.of("pm2", "delete", service.name()));
    }

    /** Supprime un processus PM2 s'il existe */
    private static void deleteIfExists(String name) throws Exception {
        if (!processExists(name)) {
            return;
        }
        runCommand(List.of("pm2", "delete", name));
    }

    /** Execute une commande systeme */
    private static void runCommand(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process p = pb.start();
        int exit = p.waitFor();
        if (exit != 0) {
            throw new RuntimeException("Commande échouée (" + String.join(" ", command) + "), code " + exit);
        }
    }

    /** Verifie si un processus PM2 existe */
    private static boolean processExists(String name) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("pm2", "pid", name);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process p = pb.start();
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            output = reader.readLine();
        }
        int exit = p.waitFor();
        if (exit != 0) {
            return false;
        }
        if (output == null) {
            return false;
        }
        output = output.trim();
        return !output.isEmpty() && !"0".equals(output);
    }

    /** Record definissant un service */
    private record Service(String name, String relativeDir, String mainClass) {
        /** Retourne le chemin absolu du service */
        Path directory() {
            return Path.of(relativeDir).toAbsolutePath().normalize();
        }

        /** Retourne le classpath du service */
        String classpath() {
            return "bin:../utils/bin:../lib/*";
        }
    }
}
