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

public final class PlantController extends AppController {

    @GET
    public void index() {
        runAction(() -> {
            LazyList<Plant> plants = Plant.findAll().orderBy("name asc");
            respondJson(200, plants.toJson(false));
        });
    }

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

    public void adminPlants() {
        runAction(() -> {
            requireAdmin();
            LazyList<Plant> plants = Plant.findAll().orderBy("name asc");
            respondJson(200, plants.toJson(false));
        });
    }

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

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private Integer parseId(String id) {
        try {
            return Integer.valueOf(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
