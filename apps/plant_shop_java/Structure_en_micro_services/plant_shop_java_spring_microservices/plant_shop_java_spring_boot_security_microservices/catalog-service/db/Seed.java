package db;

import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.nio.file.*;

/** Seed pour Catalog Service - Création des plantes uniquement */
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

	/* ---------- Constantes ---------- */
	private static final int NB_PLANTS = 50;

	private static final String[] PLANT_NAMES = {
		"Rose","Tulipe","Lavande","Orchidée","Basilic","Menthe","Pivoine","Tournesol",
		"Cactus (Echinopsis)","Bambou","Camomille (Matricaria recutita)","Sauge (Salvia officinalis)",
		"Romarin (Rosmarinus officinalis)","Thym (Thymus vulgaris)","Laurier-rose (Nerium oleander)",
		"Aloe vera","Jasmin (Jasminum officinale)","Hortensia (Hydrangea macrophylla)",
		"Marguerite (Leucanthemum vulgare)","Géranium (Pelargonium graveolens)",
		"Fuchsia (Fuchsia magellanica)","Anémone (Anemone coronaria)","Azalée (Rhododendron simsii)",
		"Chrysanthème (Chrysanthemum morifolium)","Digitale pourpre (Digitalis purpurea)",
		"Glaïeul (Gladiolus hortulanus)","Lys (Lilium candidum)","Violette (Viola odorata)",
		"Muguet (Convallaria majalis)","Iris (Iris germanica)","Lavandin (Lavandula intermedia)",
		"Érable du Japon (Acer palmatum)","Citronnelle (Cymbopogon citratus)","Pin parasol (Pinus pinea)",
		"Cyprès (Cupressus sempervirens)","Olivier (Olea europaea)","Papyrus (Cyperus papyrus)",
		"Figuier (Ficus carica)","Eucalyptus (Eucalyptus globulus)","Acacia (Acacia dealbata)",
		"Bégonia (Begonia semperflorens)","Calathea (Calathea ornata)","Dieffenbachia (Dieffenbachia seguine)",
		"Ficus elastica","Sansevieria (Sansevieria trifasciata)","Philodendron (Philodendron scandens)",
		"Yucca (Yucca elephantipes)","Zamioculcas zamiifolia","Monstera deliciosa",
		"Pothos (Epipremnum aureum)","Agave (Agave americana)","Cactus raquette (Opuntia ficus-indica)"
	};

	private static final Random RNG = new Random();

	/* ---------- Helpers ---------- */
	private static int rnd(int min,int max){ return min + RNG.nextInt(max - min + 1); }

	/**
	 * Génère une phrase Lorem Ipsum aléatoire.
	 * @return Phrase générée avec 10-14 mots
	 */
	private static String loremSentence() {
		String[] words = {"lorem","ipsum","dolor","sit","amet","consectetur","adipiscing","elit",
				"sed","do","eiusmod","tempor","incididunt","ut","labore","et","dolore","magna","aliqua"};
		int n = rnd(10,14);
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<n;i++){
			String w = words[rnd(0,words.length-1)];
			sb.append(i==0? Character.toUpperCase(w.charAt(0))+w.substring(1): w);
			sb.append(i==n-1?'.':' ');
		}
		return sb.toString();
	}

	/* ---------- Main ---------- */
	public static void main(String[] args) throws Exception {

		Map<String,String> cfg = env();
		Connection db = DriverManager.getConnection(
				cfg.get("DATABASE_URL"), cfg.get("DATABASE_USER"), cfg.get("DATABASE_PASS")
		);

		/* Nettoyage */
		System.out.println("🧹 Nettoyage de la table plants…");
		try(Statement st=db.createStatement()){
			st.execute("TRUNCATE plants RESTART IDENTITY CASCADE");
		}
		System.out.println("✅ Table plants vidée.");

		/* ---------- Plants ---------- */
		PreparedStatement insPlant = db.prepareStatement(
			"INSERT INTO plants(name,description,price,stock) VALUES (?,?,?,?)",
			Statement.RETURN_GENERATED_KEYS);

		System.out.println("🌱 Création des plantes…");

		for(int i=0;i<NB_PLANTS;i++){
			String base = PLANT_NAMES[i % PLANT_NAMES.length];
			String name = NB_PLANTS>PLANT_NAMES.length ? base+" "+(i/PLANT_NAMES.length+1): base;
			int price = rnd(5,50);
			int stock = rnd(5,30);
			insPlant.setString(1,name);
			insPlant.setString(2,loremSentence());
			insPlant.setBigDecimal(3,new BigDecimal(price));
			insPlant.setInt(4,stock);
			insPlant.executeUpdate();
		}
		System.out.println("✅ "+NB_PLANTS+" plantes créées.");

		db.close();
		System.out.println("🎉 Seed catalog-service terminée !");
	}
}
