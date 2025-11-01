package app.controllers;

import models.Plant;
import org.javalite.activejdbc.LazyList;
import org.javalite.activeweb.annotations.DELETE;
import org.javalite.activeweb.annotations.GET;
import org.javalite.activeweb.annotations.PATCH;
import org.javalite.activeweb.annotations.POST;
import org.javalite.common.JsonHelper;

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
    public void show(int id) {
        runAction(() -> {
            Plant plant = Plant.findById(id);
            if (plant == null) {
                respondJson(404, JsonHelper.toJsonString(Map.of("error", "Plante introuvable")));
                return;
            }
            respondJson(200, plant.toJson(false));
        });
    }

    public void admin_plants() {
        runAction(() -> {
            requireAdmin();
            LazyList<Plant> plants = Plant.findAll().orderBy("name asc");
            respondJson(200, plants.toJson(false));
        });
    }

    @POST
    public void create_admin_plant() {
        runAction(() -> {
            requireAdmin();
            Plant plant = new Plant();
            plant.fromMap(params1st());
            if (!plant.saveIt()) {
                respondJson(400, JsonHelper.toJsonString(plant.errors()));
                return;
            }
            respondJson(201, plant.toJson(false));
        });
    }

    @PATCH
    public void update_admin_plant(int id) {
        runAction(() -> {
            requireAdmin();
            Plant plant = Plant.findById(id);
            if (plant == null) {
                respondJson(404, JsonHelper.toJsonString(Map.of("error", "Plante introuvable")));
                return;
            }
            plant.fromMap(params1st());
            if (!plant.saveIt()) {
                respondJson(400, JsonHelper.toJsonString(plant.errors()));
                return;
            }
            respondJson(200, plant.toJson(false));
        });
    }

    @DELETE
    public void delete_admin_plant(int id) {
        runAction(() -> {
            requireAdmin();
            Plant plant = Plant.findById(id);
            if (plant == null) {
                respondJson(404, JsonHelper.toJsonString(Map.of("error", "Plante introuvable")));
                return;
            }
            plant.delete();
            respondEmpty(200);
        });
    }
}
