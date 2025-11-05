package db;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import util.PasswordUtil;

public final class Seed {

    private static final int NB_ADMINS = 3;
    private static final int NB_USERS  = 20;
    private static final String[] FIRST = {
        "Alice","Bruno","Cathy","David","Emma","Franck",
        "Gwen","Hugo","Inès","Jules","Katia","Léo"
    };
    private static final String[] LAST = {
        "Dupont","Martin","Bernard","Petit","Robert","Richard","Durand","Moreau","Roux","Fournier"
    };
    private static final String[] EMAIL_DOMAINS = {"gmail.com","yahoo.com","hotmail.com"};
    private static final Random RNG = new Random();

    private static int rnd(int min,int max){ return min + RNG.nextInt(max - min + 1); }
    private static <T> T pick(T[] arr){ return arr[rnd(0,arr.length-1)]; }
    private static String randPwd(){ return "pw" + rnd(100000000,999999999); }
    private static String hash(String p){ return PasswordUtil.hashPassword(p); }

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

        System.out.println("🧹 Nettoyage [UserService] tables…");
        try(Statement st=db.createStatement()){
            st.execute("TRUNCATE users RESTART IDENTITY CASCADE");
        }

        PreparedStatement insUser = db.prepareStatement(
            "INSERT INTO users(name,email,password_hash,is_admin) VALUES (?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS);

        List<String> credsOut = new ArrayList<>();
        credsOut.add("Administrateurs :\n");

        System.out.println("👑 Création des administrateurs…");
        for(int i=0;i<NB_ADMINS;i++){
            String name = pick(FIRST)+" "+pick(LAST);
            String email = "admin"+(i+1)+"@planteshop.com";
            String pwd = "password";
            insUser.setString(1,name);
            insUser.setString(2,email);
            insUser.setString(3,hash(pwd));
            insUser.setBoolean(4,true);
            insUser.executeUpdate();
            credsOut.add(email+" "+pwd);
        }
        System.out.println("✅ Admins créés.");

        credsOut.add("");
        credsOut.add("Utilisateurs :\n");

        System.out.println("👥 Création des utilisateurs…");
        for(int i=0;i<NB_USERS;i++){
            String first = pick(FIRST), last = pick(LAST);
            String email = first.toLowerCase()+"_"+last.toLowerCase()+rnd(20,99)+"@"+pick(EMAIL_DOMAINS);
            String pwd = randPwd();
            String name = first+" "+last;
            insUser.setString(1,name);
            insUser.setString(2,email);
            insUser.setString(3,hash(pwd));
            insUser.setBoolean(4,false);
            insUser.executeUpdate();
            credsOut.add(email+" "+pwd);
        }
        System.out.println("✅ Utilisateurs créés.");

        try(PrintWriter pw=new PrintWriter("users.txt")){
            credsOut.forEach(pw::println);
        }
        System.out.println("✍️  Fichier users.txt généré.");

        db.close();
        System.out.println("🎉 Seed [UserService] terminée !");
    }
}
