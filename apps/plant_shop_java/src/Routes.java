package src;   // ← ajustez le package racine

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.io.*;

/**
 * Point d'entrée de l'application.
 *  - charge .env
 *  - ouvre la connexion JDBC
 *  - installe les routes
 *  - lance le serveur HTTP
 */
public final class Main {

    private static Map<String,String> env() throws IOException {
        Map<String,String> m = new HashMap<String,String>();
        BufferedReader br = new BufferedReader(new FileReader(".env"));
        String l;
        while ((l = br.readLine()) != null) {
            int i = l.indexOf('=');
            if (i > 0) m.put(l.substring(0,i).trim(), l.substring(i+1).trim());
        }
        br.close(); return m;
    }

    public static void main(String[] args) throws Exception {

        /* ---------- config ---------- */
        Map<String,String> cfg = env();
        String url  = cfg.get("DATABASE_URL");
        String user = cfg.get("DATABASE_USER");
        String pass = cfg.get("DATABASE_PASS");
        String portStr = cfg.get("SERVER_ADDRESS");       // ex: 4100
        int port = portStr != null ? Integer.parseInt(portStr) : 4100;

        if (url==null||user==null||pass==null)
            throw new IllegalStateException("DATABASE_* manquants dans .env");

        /* ---------- JDBC ---------- */
        Connection db = DriverManager.getConnection(url, user, pass);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() { try { db.close(); } catch (Exception ignore) {} }
        });

        /* ---------- HTTP ---------- */
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        Routes.mount(server, db);
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("🚀  Server up on http://localhost:"+port);
    }
}
