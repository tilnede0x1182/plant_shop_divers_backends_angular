package db;

import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.Date;

public final class Seed {

    /* ---------- Lecture .env ---------- */
    private static Map<String,String> env() throws IOException {
        Map<String,String> m = new HashMap<String,String>();
        BufferedReader br = new BufferedReader(new FileReader(".env"));
        String l; while ((l = br.readLine()) != null) {
            int i = l.indexOf('=');
            if (i > 0) m.put(l.substring(0,i).trim(), l.substring(i+1).trim());
        }
        br.close(); return m;
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

    private static final String[] FIRST = {"Alice","Bob","Cathy","David","Emma","Franck","Gwen","Hugo","Iris","Jack"};
    private static final String[] LAST  = {"Dupont","Martin","Bernard","Petit","Robert","Richard","Durand","Moreau"};
    private static final String[] DOMAINS = {"test.com","mail.test","example.com"};

    /* ---------- Helpers ---------- */
    private static int rnd(int min,int max){ return min + new Random().nextInt(max-min+1); }
    private static String pick(String[] arr){ return arr[rnd(0,arr.length-1)]; }
    private static String randPwd(){ return "pw"+rnd(10000000,99999999); }
    private static String hash(String p){ return "h$"+p; } // stub bcrypt

    /* ---------- Main ---------- */
    public static void main(String[] args) throws Exception {
        Map<String,String> cfg = env();
        String url = cfg.get("DATABASE_URL");          // jdbc:postgresql://…/plant_shop_java
        if(url==null) throw new IllegalStateException("DATABASE_URL manquant");

        System.out.println("🧹  Reset DB…");
        Connection db = DriverManager.getConnection(url);
        Statement st  = db.createStatement();
        st.execute("TRUNCATE order_items,orders,plants,users RESTART IDENTITY CASCADE");
        System.out.println("✅  Vidée.");

        /* ---------- Users ---------- */
        PreparedStatement insUser = db.prepareStatement(
            "INSERT INTO users(name,email,password_hash,role) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

        List<Integer> userIds   = new ArrayList<Integer>();
        List<Integer> adminIds  = new ArrayList<Integer>();

        System.out.println("👑  Création admins…");
        for(int i=1;i<=NB_ADMINS;i++){
            insUser.setString(1,"Admin "+i);
            insUser.setString(2,"admin"+i+"@planteshop.com");
            insUser.setString(3,hash("password"));
            insUser.setString(4,"ADMIN");
            insUser.executeUpdate();
            ResultSet rs = insUser.getGeneratedKeys(); rs.next();
            adminIds.add(rs.getInt(1));
        }
        System.out.println("✅  "+adminIds.size()+" admins.");

        System.out.println("👥  Création users…");
        for(int i=1;i<=NB_USERS;i++){
            String fn = pick(FIRST), ln = pick(LAST);
            String mail = fn.toLowerCase()+"."+ln.toLowerCase()+rnd(10,99)+"@"+pick(DOMAINS);
            insUser.setString(1,fn+" "+ln);
            insUser.setString(2,mail);
            insUser.setString(3,hash(randPwd()));
            insUser.setString(4,"USER");
            insUser.executeUpdate();
            ResultSet rs = insUser.getGeneratedKeys(); rs.next();
            userIds.add(rs.getInt(1));
        }
        System.out.println("✅  "+userIds.size()+" users.");

        /* ---------- Plants ---------- */
        PreparedStatement insPlant = db.prepareStatement(
            "INSERT INTO plants(name,price,stock) VALUES (?,?,?)",
            Statement.RETURN_GENERATED_KEYS
        );
        List<Integer> plantIds = new ArrayList<Integer>();

        System.out.println("🌱  Création plantes…");
        for(int i=0;i<NB_PLANTS;i++){
            String base = PLANT_NAMES[i % PLANT_NAMES.length];
            String name = NB_PLANTS>PLANT_NAMES.length ? base+" "+(i/PLANT_NAMES.length+1) : base;
            insPlant.setString(1,name);
            insPlant.setInt(2,rnd(5,50));
            insPlant.setInt(3,rnd(5,30));
            insPlant.executeUpdate();
            ResultSet rs = insPlant.getGeneratedKeys(); rs.next();
            plantIds.add(rs.getInt(1));
        }
        System.out.println("✅  "+plantIds.size()+" plantes.");

        /* ---------- Orders & items ---------- */
        PreparedStatement insOrder = db.prepareStatement(
            "INSERT INTO orders(user_id,total,status) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS);
        PreparedStatement insItem  = db.prepareStatement(
            "INSERT INTO order_items(order_id,plant_id,quantity,price) VALUES (?,?,?,?)");

        String[] status = {"confirmed","pending","shipped","delivered"};
        int totalOrders = 0;

        System.out.println("🛒  Création commandes…");
        for(Integer uid : userIds){
            int n = rnd(0,MAX_ORDERS_PER_USER);
            for(int k=0;k<n;k++){
                insOrder.setInt(1,uid);
                insOrder.setBigDecimal(2, BigDecimal.ZERO);
                insOrder.setString(3, status[rnd(0,3)]);
                insOrder.executeUpdate();
                ResultSet oRs = insOrder.getGeneratedKeys(); oRs.next();
                int oid = oRs.getInt(1);

                int orderTotal = 0;
                for(int it=0;it<2;it++){
                    int pid = plantIds.get(rnd(0,plantIds.size()-1));
                    int qty = rnd(1,5);
                    int price = rnd(5,50);

                    insItem.setInt(1,oid);
                    insItem.setInt(2,pid);
                    insItem.setInt(3,qty);
                    insItem.setInt(4,price);
                    insItem.executeUpdate();
                    orderTotal += price*qty;
                }
                st.executeUpdate("UPDATE orders SET total="+orderTotal+" WHERE id="+oid);
                totalOrders++;
            }
        }
        System.out.println("✅  "+totalOrders+" commandes.");

        db.close();
        System.out.println("\n🎉  Seed terminée avec succès !");
    }
}
