package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public final class DatabaseFactory {
    private DatabaseFactory(){}

    public static Map<String,String> loadEnv() {
        Map<String,String> out = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("config/.env"))) {
            String l;
            while ((l = br.readLine()) != null) {
                int i = l.indexOf('=');
                if (i > 0) out.put(l.substring(0, i).trim(), l.substring(i + 1).trim());
            }
        } catch (Exception ignored) {}
        return out;
    }

    public static String jdbcUrlOrDefault() {
        return loadEnv().getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost:5432/plant_shop_java_lite_active_web");
    }
    public static String dbUserOrDefault() {
        return loadEnv().getOrDefault("DATABASE_USER", "postgres");
    }
    public static String dbPassOrDefault() {
        return loadEnv().getOrDefault("DATABASE_PASS", "postgrespw");
    }
}
