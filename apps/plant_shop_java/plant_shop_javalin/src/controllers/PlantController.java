// src/controllers/PlantController.java
package controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.sql.Connection;
import java.util.List;
import model.Plant;
import repository.PlantRepository;

public final class PlantController {

    private final PlantRepository repo;

    public PlantController(Connection db) {
        this.repo = new PlantRepository(db);
    }

    public void listPublic(Context ctx) throws Exception {
        List<Plant> plants = repo.list();
        plants.sort((a, b) -> a.name.compareTo(b.name));
        ctx.json(plants);
    }

    public void listAdmin(Context ctx) throws Exception {
        List<Plant> plants = repo.list();
        plants.sort((a, b) -> a.name.compareTo(b.name));
        ctx.json(plants);
    }

    public void show(Context ctx) throws Exception {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Plant plant = repo.find(id);
        if (plant == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }
        ctx.json(plant);
    }

    public void create(Context ctx) throws Exception {
        Plant newPlant = ctx.bodyAsClass(Plant.class);
        int id = repo.create(newPlant);
        newPlant.id = id;
        ctx.status(HttpStatus.CREATED).json(newPlant);
    }

    public void update(Context ctx) throws Exception {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Plant plant = repo.find(id);
        if (plant == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }
        Plant updatedData = ctx.bodyAsClass(Plant.class);
        if (updatedData.name != null) plant.name = updatedData.name;
        if (updatedData.description != null) plant.description = updatedData.description;
        if (updatedData.price != null) plant.price = updatedData.price;
        plant.stock = updatedData.stock; // stock peut être 0
        repo.update(plant);
        ctx.json(plant);
    }

    public void destroy(Context ctx) throws Exception {
        int id = Integer.parseInt(ctx.pathParam("id"));
        repo.delete(id);
        ctx.status(HttpStatus.OK);
    }
}
