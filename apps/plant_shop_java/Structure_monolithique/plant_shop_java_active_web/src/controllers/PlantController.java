package app.controllers;

import models.Plant;
import org.javalite.activejdbc.LazyList;
import org.javalite.activeweb.annotations.DELETE;
import org.javalite.activeweb.annotations.GET;
import org.javalite.activeweb.annotations.PATCH;
import org.javalite.activeweb.annotations.POST;
import org.javalite.common.JsonHelper;
import util.ApiMapper;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Controleur de gestion des plantes.
 * Gere les operations CRUD sur les plantes.
 */
public final class PlantController extends AppController {

    /**
     * Liste toutes les plantes.
     */
    @GET
    public void index() {
        runAction(() -> {
            LazyList<Plant> plants = Plant.findAll().orderBy("name asc");
            respondJson(200, plants.toJson(false));
        });
    }

    /**
     * Affiche une plante specifique.
     */
    @GET
    public void show() {
        runAction(() -> {
            Integer plantId = parseId(getId());
            Plant plant = (plantId == null) ? null : Plant.findById(plantId);
            if (plant == null) {
                respondJson(404, JsonHelper.toJsonString(Map.of("error", "Plante introuvable")));
                return;
            }
            respondJson(200, plant.toJson(false));
        });
    }

    /**
     * Liste toutes les plantes (admin uniquement).
     */
    public void adminPlants() {
        runAction(() -> {
            requireAdmin();
            LazyList<Plant> plants = Plant.findAll().orderBy("name asc");
            respondJson(200, plants.toJson(false));
        });
    }

    /**
     * Cree une nouvelle plante (admin uniquement).
     */
    @POST
    public void createAdminPlant() {
        runAction(() -> {
            requireAdmin();
            Map<String, Object> body = ApiMapper.jsonToMap(getRequestString());

            Plant plant = new Plant();
            applyPlantPayload(plant, body, true);
            if (!plant.saveIt()) {
                respondJson(400, JsonHelper.toJsonString(plant.errors()));
                return;
            }
            respondJson(201, plant.toJson(false));
        });
    }

    /**
     * Met a jour une plante (admin uniquement).
     */
    @PATCH
    public void updateAdminPlant() {
        runAction(() -> {
            requireAdmin();
            Integer plantId = parseId(getId());
            Plant plant = (plantId == null) ? null : Plant.findById(plantId);
            if (plant == null) {
                respondJson(404, JsonHelper.toJsonString(Map.of("error", "Plante introuvable")));
                return;
            }
            Map<String, Object> body = ApiMapper.jsonToMap(getRequestString());
            applyPlantPayload(plant, body, false);
            if (!plant.saveIt()) {
                respondJson(400, JsonHelper.toJsonString(plant.errors()));
                return;
            }
            respondJson(200, plant.toJson(false));
        });
    }

    /**
     * Supprime une plante (admin uniquement).
     */
    @DELETE
    public void deleteAdminPlant() {
        runAction(() -> {
            requireAdmin();
            Integer plantId = parseId(getId());
            Plant plant = (plantId == null) ? null : Plant.findById(plantId);
            if (plant == null) {
                respondJson(404, JsonHelper.toJsonString(Map.of("error", "Plante introuvable")));
                return;
            }
            plant.delete();
            respondEmpty(200);
        });
    }

    /**
     * Applique les donnees du payload sur une plante.
     * @param plant Plant Plante a modifier
     * @param payload Map<String,Object> Donnees a appliquer
     * @param requireAll boolean Si true, tous les champs sont requis
     */
    private void applyPlantPayload(Plant plant, Map<String, Object> payload, boolean requireAll) {
        Object name = payload.get("name");
        Object price = payload.get("price");
        Object stock = payload.get("stock");

        if (requireAll) {
            if (name == null || price == null || stock == null) {
                throw new IllegalArgumentException("Les champs name, price et stock sont requis.");
            }
        }

        if (name != null) {
            plant.set("name", name.toString());
        }
        Object description = payload.get("description");
        if (description != null) {
            plant.set("description", description.toString());
        }
        if (price != null) {
            plant.set("price", toBigDecimal(price));
        }
        if (stock != null) {
            plant.set("stock", toInteger(stock));
        }
    }

    /**
     * Convertit un objet en BigDecimal.
     * @param value Object Valeur a convertir
     * @return BigDecimal Valeur convertie
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    /**
     * Convertit un objet en Integer.
     * @param value Object Valeur a convertir
     * @return Integer Valeur convertie
     */
    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    /**
     * Parse un ID depuis une chaine.
     * @param id String ID a parser
     * @return Integer ID parse ou null
     */
    private Integer parseId(String id) {
        try {
            return Integer.valueOf(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
