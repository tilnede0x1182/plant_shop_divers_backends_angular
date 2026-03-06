package db;

import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.nio.file.*;

/** Seed pour Order Service - Création de plant_stock, orders et order_items */
public final class Seed {
	private static final Path ENV_PATH = Paths.get("config", ".env");

	/* ---------- Lecture .env ---------- */
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

	private static final int MAX_ORDERS_PER_USER = 7;
	private static final Random RNG = new Random();

	/* ---------- Helpers ---------- */
	/**
	 * Génère un entier aléatoire entre min et max (inclus).
	 * @param min Valeur minimale
	 * @param max Valeur maximale
	 * @return Entier aléatoire
	 */
	private static int rnd(int min,int max){ return min + RNG.nextInt(max - min + 1); }

	/* ---------- Main ---------- */
	public static void main(String[] args) throws Exception {

		Map<String,String> cfg = env();
		Connection db = DriverManager.getConnection(
				cfg.get("DATABASE_URL"), cfg.get("DATABASE_USER"), cfg.get("DATABASE_PASS")
		);

		/* Nettoyage */
		System.out.println("🧹 Nettoyage des tables order…");
		try(Statement st=db.createStatement()){
			st.execute("TRUNCATE order_items, orders, plant_stock RESTART IDENTITY CASCADE");
		}
		System.out.println("✅ Tables vidées.");

		/* ---------- Plant Stock ---------- */
		System.out.println("🌿 Création plant_stock...");
		PreparedStatement insStock = db.prepareStatement(
			"INSERT INTO plant_stock(id, name, price) VALUES (?,?,?)");
		for(int i=1; i<=50; i++){
			insStock.setInt(1, i);
			insStock.setString(2, "Plant #"+i);
			insStock.setBigDecimal(3, new BigDecimal(rnd(5,50)));
			insStock.executeUpdate();
		}
		System.out.println("✅ 50 entrées plant_stock créées.");

		/* ---------- User IDs ---------- */
		List<Integer> userIds = new ArrayList<>();
		for(int i=1; i<=23; i++) userIds.add(i);

		/* ---------- Orders & items ---------- */
		PreparedStatement insOrder = db.prepareStatement(
				"INSERT INTO orders(user_id,total,status) VALUES (?,?,?)",
				Statement.RETURN_GENERATED_KEYS);
		PreparedStatement insItem = db.prepareStatement(
			"INSERT INTO order_items(order_id,plant_id,quantity,price) VALUES (?,?,?,?)");

		String[] statusArr = {"confirmed","pending","shipped","delivered"};
		int totalOrders = 0;

		System.out.println("🛒 Création des commandes…");
		for(Integer uid : userIds){
			int nb = rnd(0,MAX_ORDERS_PER_USER);
			for(int k=0;k<nb;k++){
				insOrder.setInt(1,uid);
				insOrder.setBigDecimal(2,BigDecimal.ZERO);
				insOrder.setString(3,statusArr[rnd(0,3)]);
				insOrder.executeUpdate();
				int orderId;
				try(ResultSet rs=insOrder.getGeneratedKeys()){ rs.next(); orderId = rs.getInt(1); }

				BigDecimal total = BigDecimal.ZERO;
				for(int it=0;it<2;it++){
					int plantId = rnd(1,50);
					int qty = rnd(1,5);
					int price = rnd(5,50);
					insItem.setInt(1,orderId);
					insItem.setInt(2,plantId);
					insItem.setInt(3,qty);
					insItem.setBigDecimal(4,new BigDecimal(price));
					insItem.executeUpdate();
					total = total.add(new BigDecimal(price*qty));
				}
				try(PreparedStatement up=db.prepareStatement("UPDATE orders SET total=? WHERE id=?")){
					up.setBigDecimal(1,total);
					up.setInt(2,orderId);
					up.executeUpdate();
				}
				totalOrders++;
			}
		}
		System.out.println("✅ "+totalOrders+" commandes créées.");

		db.close();
		System.out.println("🎉 Seed order-service terminée !");
	}
}
