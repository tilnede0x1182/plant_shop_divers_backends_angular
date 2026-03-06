package db;

import java.io.*;
import java.sql.*;
import java.util.*;
import util.PasswordUtil;
import java.nio.file.*;

/** Seed pour User Service - Création des utilisateurs */
public final class Seed {
	private static final Path ENV_PATH = Paths.get("config", ".env");

	/* ---------- Lecture .env ---------- */
	/**
	 * Lit les variables d'environnement depuis le fichier .env.
	 * @return Map<String, String> Les paires clé-valeur du fichier
	 * @throws IOException En cas d'erreur de lecture
	 */
	private static Map<String, String> env() throws IOException {
			Map<String, String> out = new HashMap<>();
			try (BufferedReader br = Files.newBufferedReader(ENV_PATH)) {
					String line;
					while ((line = br.readLine()) != null) {
							int i = line.indexOf('=');
							if (i > 0) {
									out.put(line.substring(0, i).trim(), line.substring(i + 1).trim());
							}
					}
			}
			return out;
	}

	/* ---------- Constantes ---------- */
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

	/* ---------- Helpers ---------- */
	/**
	 * Génère un entier aléatoire entre min et max (inclus).
	 * @param min int Valeur minimale
	 * @param max int Valeur maximale
	 * @return int L'entier aléatoire généré
	 */
	private static int rnd(int min,int max){ return min + RNG.nextInt(max - min + 1); }
	/** Sélectionne un élément aléatoire dans un tableau. */
	private static <T> T pick(T[] arr){ return arr[rnd(0,arr.length-1)]; }
	/** Génère un mot de passe aléatoire. */
	private static String randPwd(){ return "pw" + rnd(100000000,999999999); }
	/**
	 * Hash un mot de passe.
	 * @param p String Le mot de passe en clair
	 * @return String Le hash du mot de passe
	 */
	private static String hash(String p){ return PasswordUtil.hashPassword(p); }

	/* ---------- Main ---------- */
	/**
	 * Point d'entrée du script de seed.
	 * @param args String[] Arguments CLI (non utilisés)
	 * @throws Exception En cas d'erreur
	 */
	public static void main(String[] args) throws Exception {

		Map<String,String> cfg = env();
		Connection db = DriverManager.getConnection(
				cfg.get("DATABASE_URL"), cfg.get("DATABASE_USER"), cfg.get("DATABASE_PASS")
		);

		/* Nettoyage */
		System.out.println("🧹 Nettoyage de la table users…");
		try(Statement st=db.createStatement()){
			st.execute("TRUNCATE users RESTART IDENTITY CASCADE");
		}
		System.out.println("✅ Table users vidée.");

		/* ---------- Users ---------- */
		PreparedStatement insUser = db.prepareStatement(
			"INSERT INTO users(name,email,password_hash,is_admin) VALUES (?,?,?,?)",
			Statement.RETURN_GENERATED_KEYS);

		List<Integer> adminIds = new ArrayList<>();
		List<Integer> userIds  = new ArrayList<>();
		List<String>  credsOut = new ArrayList<>();
		credsOut.add("Administrateurs :\n");

		// Admins
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
			try(ResultSet rs=insUser.getGeneratedKeys()){ rs.next(); adminIds.add(rs.getInt(1)); }
			credsOut.add(email+" "+pwd);
		}
		System.out.println("✅ "+adminIds.size()+" admins.");

		credsOut.add("");
		credsOut.add("Utilisateurs :\n");

		// Users
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
			try(ResultSet rs=insUser.getGeneratedKeys()){ rs.next(); userIds.add(rs.getInt(1)); }
			credsOut.add(email+" "+pwd);
		}
		System.out.println("✅ "+userIds.size()+" utilisateurs.");

		/* ---------- users.txt ---------- */
		try(PrintWriter pw=new PrintWriter("users.txt")){
			credsOut.forEach(pw::println);
		}
		System.out.println("✍️ Fichier users.txt généré ("+credsOut.size()+" lignes).");

		db.close();
		System.out.println("🎉 Seed user-service terminée !");
	}
}
