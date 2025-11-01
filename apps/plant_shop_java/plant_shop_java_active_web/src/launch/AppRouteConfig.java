package launch;

import org.javalite.activeweb.AbstractRouteConfig;
import org.javalite.activeweb.AppContext;

import app.controllers.PlantController;

public final class AppRouteConfig extends AbstractRouteConfig {

    @Override
    public void init(AppContext context) {
        route("/plants")
            .to(PlantController.class)
            .get()
            .action("index");

        route("/plants/{id}")
            .to(PlantController.class)
            .get()
            .action("show");

        route("/admin/plants")
            .to(PlantController.class)
            .get()
            .action("adminPlants");

        route("/admin/plants")
            .to(PlantController.class)
            .post()
            .action("createAdminPlant");

        route("/admin/plants/{id}")
            .to(PlantController.class)
            .patch()
            .action("updateAdminPlant");

        route("/admin/plants/{id}")
            .to(PlantController.class)
            .delete()
            .action("deleteAdminPlant");
    }
}
