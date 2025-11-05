package db;

import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Seed {

    private static final int MAX_ORDERS_PER_USER = 7;
    private static final Random RNG = new Random();

    private static int rnd(int min,int max){ return min + RNG.nextInt(max - min + 1); }

    private static Map<String,String> env() throws IOException {
        Map<String,String> out = new HashMap<>();
        Path envPath = Path.of("../config/.env");
        if (!Files.exists(envPath)) {
            envPath = Path.of("config/.env");
        }
        if (!Files.exists(envPath)) {
             envPath = Path.of(".env");
        }
        try (BufferedReader br = new BufferedReader(new FileReader(envPath.toFile()))) {
            String l;
            while ((l = br.readLine()) != null) {
                int i = l.indexOf('=');
                if (i > 0) out.put(l.substring(0, i).trim(), l.substring(i + 1).trim());
            }
        }
        return out;
    }

    public static void main(String[] args) throws Exception {
        Map<String,String> cfg = env();
        Connection db = DriverManager.getConnection(
            cfg.get("DATABASE_URL"), cfg.get("DATABASE_USER"), cfg.get("DATABASE_PASS")
        );

        System.out.println("🧹 Nettoyage [OrderService] tables…");
        try(Statement st=db.createStatement()){
            st.execute("TRUNCATE order_items,orders RESTART IDENTITY CASCADE");
        }

        // 1. Récupérer les IDs des utilisateurs (non-admin)
        List<Integer> userIds = new ArrayList<>();
        try (Statement st = db.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM users WHERE is_admin = FALSE")) {
            while (rs.next()) {
                userIds.add(rs.getInt(1));
            }
        }
        if (userIds.isEmpty()) {
            System.out.println("⚠️ Aucun utilisateur non-admin trouvé. Seed des commandes annulée.");
            db.close();
            return;
        }

        // 2. Récupérer les plantes
        class PlantInfo{ int id,price,stock; PlantInfo(int id,int p,int s){this.id=id;price=p;stock=s;} }
        List<PlantInfo> plants = new ArrayList<>();
        try (Statement st = db.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, price, stock FROM plants")) {
            while (rs.next()) {
                plants.add(new PlantInfo(rs.getInt(1), rs.getInt(2), rs.getInt(3)));
            }
        }
         if (plants.isEmpty()) {
            System.out.println("⚠️ Aucune plante trouvée. Seed des commandes annulée.");
            db.close();
            return;
        }

        System.out.println("🛒 Création des commandes…");

        PreparedStatement insOrder = db.prepareStatement(
                "INSERT INTO orders(user_id,total,status) VALUES (?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
        PreparedStatement insItem = db.prepareStatement(
            "INSERT INTO order_items(order_id,plant_id,quantity,price) VALUES (?,?,?,?)");
        PreparedStatement updPlantStock = db.prepareStatement(
            "UPDATE plants SET stock = ? WHERE id = ?");
        PreparedStatement updOrderTotal = db.prepareStatement("UPDATE orders SET total=? WHERE id=?");

        String[] statusArr = {"confirmed","pending","shipped","delivered"};
        int totalOrders = 0;

        for(Integer uid : userIds){
            int nb = rnd(0, MAX_ORDERS_PER_USER);
            for(int k=0; k<nb; k++){
                insOrder.setInt(1, uid);
                insOrder.setBigDecimal(2, BigDecimal.ZERO); // placeholder
                insOrder.setString(3, statusArr[rnd(0,3)]);
                insOrder.executeUpdate();
                int orderId;
                try(ResultSet rs = insOrder.getGeneratedKeys()){ rs.next(); orderId = rs.getInt(1); }

                BigDecimal total = BigDecimal.ZERO;
                int itemsInOrder = rnd(1, 3);

                for(int it=0; it < itemsInOrder; it++){
                    List<PlantInfo> avail = plants.stream().filter(p->p.stock > 0).toList();
                    if(avail.isEmpty()) break;

                    PlantInfo p = avail.get(rnd(0,avail.size()-1));
                    int qty = Math.min(rnd(1,3), p.stock);

                    insItem.setInt(1, orderId);
                    insItem.setInt(2, p.id);
                    insItem.setInt(3, qty);
                    insItem.setBigDecimal(4, new BigDecimal(p.price));
                    insItem.executeUpdate();

                    p.stock -= qty;
                    updPlantStock.setInt(1, p.stock);
                    updPlantStock.setInt(2, p.id);
                    updPlantStock.executeUpdate();

                    total = total.add(new BigDecimal(p.price * qty));
                }

                updOrderTotal.setBigDecimal(1, total);
                updOrderTotal.setInt(2, orderId);
                updOrderTotal.executeUpdate();

                totalOrders++;
            }
        }
        System.out.println("✅ "+totalOrders+" commandes créées.");

        db.close();
        System.out.println("🎉 Seed [OrderService] terminée !");
    }
}
