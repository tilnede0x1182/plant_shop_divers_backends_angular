package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Pm2Manager {

    private static final Map<String, Service> SERVICES = new LinkedHashMap<>();

    static {
        SERVICES.put("auth-service", new Service("auth-service", "auth-service", "AuthService"));
        SERVICES.put("catalog-service", new Service("catalog-service", "catalog-service", "CatalogService"));
        SERVICES.put("order-service", new Service("order-service", "order-service", "OrderService"));
        SERVICES.put("user-service", new Service("user-service", "user-service", "UserService"));
        SERVICES.put("gateway", new Service("gateway", "gateway", "Gateway"));
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }
        switch (args[0]) {
            case "start-all" -> startAll();
            case "stop-all" -> stopAll();
            case "stop-all-safe" -> stopAllSafe();
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

    private static void printUsage() {
        System.out.println("""
            Usage: java util.Pm2Manager <commande>
              start-all       Démarre tous les services (via pm2)
              stop-all        Arrête tous les services gérés par pm2
              stop-all-safe   Idem mais ignore les erreurs si un service est absent
              start <nom>     Démarre un service
              stop <nom>      Arrête un service

            Services disponibles: auth-service, catalog-service, order-service, user-service, gateway
            """);
    }

    private static void requireArgs(String[] args, int expected) {
        if (args.length < expected) {
            throw new IllegalArgumentException("Arguments insuffisants.");
        }
    }

    private static void startAll() throws Exception {
        for (String name : SERVICES.keySet()) {
            startOne(name);
        }
    }

    private static void stopAll() throws Exception {
        for (String name : SERVICES.keySet()) {
            stopOne(name);
        }
    }

    private static void stopAllSafe() {
        for (String name : SERVICES.keySet()) {
            try {
                stopOne(name);
            } catch (Exception e) {
                System.out.printf("⚠️  Impossible d'arrêter %s proprement (%s)%n", name, e.getMessage());
            }
        }
    }

    private static void startOne(String name) throws Exception {
        Service service = SERVICES.get(name);
        if (service == null) {
            throw new IllegalArgumentException("Service inconnu: " + name);
        }
        deleteIfExists(service.name());
        runCommand(List.of(
            "pm2", "start", "java",
            "--name", service.name(),
            "--cwd", service.directory().toString(),
            "--",
            "-cp", service.classpath(),
            service.mainClass()
        ));
    }

    private static void stopOne(String name) throws Exception {
        Service service = SERVICES.get(name);
        if (service == null) {
            throw new IllegalArgumentException("Service inconnu: " + name);
        }
        if (!processExists(service.name())) {
            System.out.printf("ℹ️  pm2 ne gérait pas le service %s%n", name);
            return;
        }
        runCommand(List.of("pm2", "delete", service.name()));
    }

    private static void deleteIfExists(String name) throws Exception {
        if (!processExists(name)) {
            return;
        }
        runCommand(List.of("pm2", "delete", name));
    }

    private static void runCommand(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process p = pb.start();
        int exit = p.waitFor();
        if (exit != 0) {
            throw new RuntimeException("Commande échouée (" + String.join(" ", command) + "), code " + exit);
        }
    }

    private static boolean processExists(String name) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("pm2", "pid", name);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process p = pb.start();
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            output = reader.readLine();
        }
        int exit = p.waitFor();
        if (exit != 0 || output == null) {
            return false;
        }
        output = output.trim();
        return !output.isEmpty() && !"0".equals(output);
    }

    private record Service(String name, String relativeDir, String mainClass) {
        Path directory() {
            return Path.of(relativeDir).toAbsolutePath().normalize();
        }

        String classpath() {
            return "bin:../utils/bin:../lib/*";
        }
    }
}
