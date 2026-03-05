// src/controllers/PlantController.java
package controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import model.Plant;
import org.json.JSONObject;
import repository.PlantRepository;
import catalog.util.ApiMapper;

/**
 * Contrôleur pour la gestion des plantes.
 */
public final class PlantController {

    private final PlantRepository repo;
    private static final Collator COLLATOR;
    static {
        COLLATOR = Collator.getInstance(Locale.ROOT);
        COLLATOR.setStrength(Collator.PRIMARY);
    }

    /**
	 * Constructeur.
	 * @param db Connexion à la base de données
	 */
	public PlantController(Connection db) {
        this.repo = new PlantRepository(db);
    }

    /**
	 * Liste les plantes publiquement.
	 * @param ctx Le contexte Javalin
	 */
	public void listPublic(Context ctx) throws Exception {
        List<Plant> plants = repo.list();
        plants.sort(this::comparePlants);
        ctx.json(mapPlants(plants));
    }

    /**
	 * Liste les plantes pour l'admin.
	 * @param ctx Le contexte Javalin
	 */
	public void listAdmin(Context ctx) throws Exception {
        List<Plant> plants = repo.list();
        plants.sort(this::comparePlants);
        ctx.json(mapPlants(plants));
    }

    /**
	 * Affiche une plante.
	 * @param ctx Le contexte Javalin
	 */
	public void show(Context ctx) throws Exception {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Plant plant = repo.find(id);
        if (plant == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }
        ctx.json(ApiMapper.toPlant(plant));
    }

    /**
	 * Crée une plante.
	 * @param ctx Le contexte Javalin
	 */
	public void create(Context ctx) throws Exception {
        JSONObject body = new JSONObject(ctx.body());
        if (!body.has("name") || !body.has("price")) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Champs name et price requis"));
            return;
        }

        String name = body.getString("name");
        String description = body.optString("description", null);
        BigDecimal price = BigDecimal.valueOf(body.getDouble("price"));
        int stock = body.has("stock") ? body.getInt("stock") : 0;

        Plant newPlant = new Plant(name, description, price, stock);
        int id = repo.create(newPlant);
        Plant created = repo.find(id);
        ctx.status(HttpStatus.CREATED).json(ApiMapper.toPlant(created));
    }

    /**
	 * Met à jour une plante.
	 * @param ctx Le contexte Javalin
	 */
	public void update(Context ctx) throws Exception {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Plant plant = repo.find(id);
        if (plant == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }
        JSONObject body = new JSONObject(ctx.body());

        if (body.has("name") && !body.isNull("name")) {
            plant.name = body.getString("name");
        }
        if (body.has("description")) {
            plant.description = body.isNull("description") ? null : body.getString("description");
        }
        if (body.has("price") && !body.isNull("price")) {
            plant.price = BigDecimal.valueOf(body.getDouble("price"));
        }
        if (body.has("stock") && !body.isNull("stock")) {
            plant.stock = body.getInt("stock");
        }

        repo.update(plant);
        ctx.json(ApiMapper.toPlant(repo.find(id)));
    }

    /**
	 * Supprime une plante.
	 * @param ctx Le contexte Javalin
	 */
	public void destroy(Context ctx) throws Exception {
        int id = Integer.parseInt(ctx.pathParam("id"));
        repo.delete(id);
        ctx.status(HttpStatus.OK).json(Map.of("deleted", true));
    }

    /**
	 * Convertit une liste de plantes en liste de Maps.
	 * @param plants Les plantes
	 * @return La liste de Maps
	 */
	private List<Map<String, Object>> mapPlants(List<Plant> plants) {
        List<Map<String, Object>> mapped = new ArrayList<>(plants.size());
        for (Plant plant : plants) {
            mapped.add(ApiMapper.toPlant(plant));
        }
        return mapped;
    }

    /**
	 * Compare deux plantes par nom.
	 * @param left Première plante
	 * @param right Seconde plante
	 * @return Résultat de comparaison
	 */
	private int comparePlants(Plant left, Plant right) {
        String ln = normalizeName(left.name);
        String rn = normalizeName(right.name);
        int compared = COLLATOR.compare(ln, rn);
        if (compared != 0) {
            return compared;
        }
        return Integer.compare(left.id, right.id);
    }

    /**
	 * Normalise un nom de plante.
	 * @param name Le nom
	 * @return Le nom normalisé
	 */
	private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }
}
