package db;

import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import util.PasswordUtil;

public final class Seed {

    /* ---------- Lecture .env ---------- */
    private static Map<String,String> env() throws IOException {
        Map<String,String> out = new HashMap<String,String>();
        BufferedReader br = new BufferedReader(new FileReader(".env"));
        String l;
        while ((l = br.readLine()) != null) {
            int i = l.indexOf('=');
            if (i > 0) out.put(l.substring(0, i).trim(), l.substring(i + 1).trim());
        }
        br.close();
        return out;
    }

    /* ---------- Constantes ---------- */
    private static final int NB_ADMINS = 3;
    private static final int NB_USERS  = 20;
    private static final int NB_PLANTS = 50;
    private static final int MAX_ORDERS_PER_USER = 7;

    private static final String[] PLANT_NAMES = {
        "Rose","Tulipe","Lavande","Orchidée","Basilic","Menthe","Pivoine","Tournesol",
        "Cactus","Bambou","Camomille","Sauge","Romarin","Thym","Laurier-rose","Aloe vera",
        "Jasmin","Hortensia","Marguerite","Géranium","Fuchsia","Anémone","Azalée",
        "Chrysanthème","Digitale","Glaïeul","Lys","Violette","Muguet","Iris",
        "Lavandin","Érable du Japon","Citronnelle","Pin parasol","Cyprès","Olivier",
        "Papyrus","Figuier","Eucalyptus","Acacia","Bégonia","Calathea","Dieffenbachia",
        "Ficus","Sansevieria","Philodendron","Yucca","Zamioculcas","Monstera"
    };

    private static final String[] FIRST   = {"Alice","Bob","Cathy","David","Emma","Franck","Gwen","Hugo","Iris","Jack"};
    private static final String[] LAST    = {"Dupont","Martin","Bernard","Petit","Robert","Richard","Durand","Moreau"};
    private static final String[] DOMAINS = {"test.com","mail.test","example.com"};

    private static int rnd(int min, int max) { return min + new Random().nextInt(max - min + 1); }
    private static String pick(String[] arr) { return arr[rnd(0, arr.length - 1)]; }
    private static String randPwd()          { return "pw" + rnd(10000000, 99999999); }
    private static String hash(String p)     { return PasswordUtil.hashPassword(p); }

    /* ---------- Main ---------- */
    public static void main(String[] args) throws Exception {

        Map<String,String> cfg = env();
        String url  = cfg.get("DATABASE_URL");
        String user = cfg.get("DATABASE_USER");
        String pass = cfg.get("DATABASE_PASS");
        if (url == null || user == null || pass == null)
            throw new IllegalStateException("DATABASE_URL / USER / PASS manquants");

        System.out.println("🧹 Nettoyage de la base de données…");
        Connection db = DriverManager.getConnection(url, user, pass);
        Statement  st = db.createStatement();
        st.execute("TRUNCATE order_items,orders,plants,users RESTART IDENTITY CASCADE");
      	System.out.println("✅ Base de données nettoyée.");

				/* ---------- Users ---------- */
				PreparedStatement insUser = db.prepareStatement(
						"INSERT INTO users(name,email,password_hash,is_admin) VALUES (?,?,?,?)",
						Statement.RETURN_GENERATED_KEYS);

				Set<String> usedEmails = new HashSet<String>();
				List<Integer> adminIds = new ArrayList<Integer>();
				List<Integer> userIds  = new ArrayList<Integer>();

				System.out.println("👑 Création des administrateurs…");
				for (int i = 1; i <= NB_ADMINS; i++) {
						insUser.setString(1, "Admin " + i);
						insUser.setString(2, "admin" + i + "@planteshop.com");
						insUser.setString(3, hash("password"));
						insUser.setBoolean(4, true);
						insUser.executeUpdate();
						ResultSet rs = insUser.getGeneratedKeys(); rs.next();
						adminIds.add(rs.getInt(1));
						usedEmails.add("admin" + i + "@planteshop.com");
				}
				System.out.println("✅ " + adminIds.size() + " administrateurs créés.");

				System.out.println("👥 Création des utilisateurs…");
				Random rng = new Random();
				for (int i = 1; i <= NB_USERS; i++) {
						String email;
						do {
								String fn = pick(FIRST), ln = pick(LAST);
								email = fn.toLowerCase() + "." + ln.toLowerCase()
											+ (10 + rng.nextInt(90)) + "@" + pick(DOMAINS);
						} while (!usedEmails.add(email));

						insUser.setString(1, "User " + i);
						insUser.setString(2, email);
						insUser.setString(3, hash(randPwd()));
						insUser.setBoolean(4, false);
						insUser.executeUpdate();
						ResultSet rs = insUser.getGeneratedKeys(); rs.next();
						userIds.add(rs.getInt(1));
				}
				System.out.println("✅ " + userIds.size() + " utilisateurs créés.");

        /* ---------- Plants ---------- */
        PreparedStatement insPlant = db.prepareStatement(
            "INSERT INTO plants(name,description,price,stock) VALUES (?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS);

        List<Integer> plantIds = new ArrayList<Integer>();

        System.out.println("🌱 Création des plantes…");
        for (int i = 0; i < NB_PLANTS; i++) {
            String base = PLANT_NAMES[i % PLANT_NAMES.length];
            String name = NB_PLANTS > PLANT_NAMES.length ? base + " " + (i / PLANT_NAMES.length + 1) : base;
            insPlant.setString(1, name);
            insPlant.setString(2, null);               // description NULL
            insPlant.setBigDecimal(3, new BigDecimal(rnd(5, 50)));
            insPlant.setInt(4, rnd(5, 30));
            insPlant.executeUpdate();
            ResultSet rs = insPlant.getGeneratedKeys(); rs.next();
            plantIds.add(rs.getInt(1));
        }
        System.out.println("✅ " + plantIds.size() + " plantes créées.");

        /* ---------- Orders & items ---------- */
        PreparedStatement insOrder = db.prepareStatement(
            "INSERT INTO orders(user_id,total,status) VALUES (?,?,?)",
            Statement.RETURN_GENERATED_KEYS);
        PreparedStatement insItem  = db.prepareStatement(
            "INSERT INTO order_items(order_id,plant_id,quantity,price) VALUES (?,?,?,?)");

        String[] statusArr = {"confirmed", "pending", "shipped", "delivered"};
        int totalOrders = 0;

        System.out.println("🛒 Création des commandes…");
        for (Integer uid : userIds) {
            int nb = rnd(0, MAX_ORDERS_PER_USER);
            for (int k = 0; k < nb; k++) {

                insOrder.setInt(1, uid);
                insOrder.setBigDecimal(2, BigDecimal.ZERO); // placeholder
                insOrder.setString(3, statusArr[rnd(0, 3)]);
                insOrder.executeUpdate();

                ResultSet oRs = insOrder.getGeneratedKeys(); oRs.next();
                int orderId = oRs.getInt(1);

                BigDecimal orderTotal = BigDecimal.ZERO;
                for (int it = 0; it < 2; it++) {
                    int pid = plantIds.get(rnd(0, plantIds.size() - 1));
                    int qty = rnd(1, 5);
                    BigDecimal price = new BigDecimal(rnd(5, 50));

                    insItem.setInt(1, orderId);
                    insItem.setInt(2, pid);
                    insItem.setInt(3, qty);
                    insItem.setBigDecimal(4, price);
                    insItem.executeUpdate();

                    orderTotal = orderTotal.add(price.multiply(new BigDecimal(qty)));
                }
                PreparedStatement up = db.prepareStatement("UPDATE orders SET total=? WHERE id=?");
                up.setBigDecimal(1, orderTotal);
                up.setInt(2, orderId);
                up.executeUpdate();
                up.close();

                totalOrders++;
            }
        }
        System.out.println("✅ " + totalOrders + " commandes créées.");

        db.close();
        System.out.println("🎉 Seed terminée avec succès !");
    }
}
