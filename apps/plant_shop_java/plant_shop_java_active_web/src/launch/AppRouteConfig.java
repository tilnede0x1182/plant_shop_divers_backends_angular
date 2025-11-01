package launch;

import org.javalite.activeweb.AbstractRouteConfig;
import org.javalite.activeweb.AppContext;

import app.controllers.PlantController;

public final class AppRouteConfig extends AbstractRouteConfig {

    @Override
    public void init(AppContext context) {
        route("/admin/plants")
            .to(PlantController.class)
            .get()
            .action("admin_plants");

        route("/admin/plants")
            .to(PlantController.class)
            .post()
            .action("create_admin_plant");

        route("/admin/plants/{id}")
            .to(PlantController.class)
            .patch()
            .action("update_admin_plant");

        route("/admin/plants/{id}")
            .to(PlantController.class)
            .delete()
            .action("delete_admin_plant");
    }
}
