package db;

import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Script de seed pour peupler la base de données.
 */
public final class Seed {

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
    /**
     * Génère un nombre aléatoire entre min et max.
     * @param min Valeur minimum
     * @param max Valeur maximum
     * @return Nombre aléatoire
     */
    private static int rnd(int min,int max){ return min + RNG.nextInt(max - min + 1); }
    private static <T> T pick(T[] arr){ return arr[rnd(0,arr.length-1)]; }

    /**
     * Génère une phrase lorem ipsum.
     * @return Phrase générée
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

    /**
     * Point d'entrée principal.
     * @param args Arguments de ligne de commande
     * @throws Exception En cas d'erreur
     */
    public static void main(String[] args) throws Exception {
        Map<String,String> cfg = env();
        Connection db = DriverManager.getConnection(
            cfg.get("DATABASE_URL"), cfg.get("DATABASE_USER"), cfg.get("DATABASE_PASS")
        );

        System.out.println("🧹 Nettoyage [CatalogService] tables…");
        try(Statement st=db.createStatement()){
            st.execute("TRUNCATE plants RESTART IDENTITY CASCADE");
        }

        PreparedStatement insPlant = db.prepareStatement(
            "INSERT INTO plants(name,description,price,stock) VALUES (?,?,?,?)");

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
        System.out.println("🎉 Seed [CatalogService] terminée !");
    }
}
