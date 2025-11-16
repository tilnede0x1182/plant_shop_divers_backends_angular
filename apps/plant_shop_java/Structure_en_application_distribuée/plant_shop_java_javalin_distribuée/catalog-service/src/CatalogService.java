import controller.PlantController;
import util.AuthMiddleware;
import util.ServiceRuntime;

public final class CatalogService {

    public static void main(String[] args) throws Exception {
        ServiceRuntime.start(
            ServiceRuntime.descriptor("catalog-service", "CATALOG_SERVICE_PORT", 6102),
            (app, db) -> {
                PlantController plantController = new PlantController(db);

                app.get("/plants", plantController::listPublic);
                app.get("/plants/{id}", plantController::show);
                app.get("/admin/plants", AuthMiddleware.requireAdmin(plantController::listAdmin));
                app.post("/admin/plants", AuthMiddleware.requireAdmin(plantController::create));
                app.patch("/admin/plants/{id}", AuthMiddleware.requireAdmin(plantController::update));
                app.delete("/admin/plants/{id}", AuthMiddleware.requireAdmin(plantController::destroy));
            }
        );
    }
}
