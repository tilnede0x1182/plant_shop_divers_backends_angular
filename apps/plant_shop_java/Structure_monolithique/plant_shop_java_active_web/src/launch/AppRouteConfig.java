package launch;

import org.javalite.activeweb.AbstractRouteConfig;
import org.javalite.activeweb.AppContext;

import app.controllers.PlantController;
import app.controllers.UserController;
import app.controllers.OrderController;

/**
 * Configuration des routes ActiveWeb.
 * Définit le mapping URL vers contrôleurs.
 */
public final class AppRouteConfig extends AbstractRouteConfig {

    /**
     * Initialise les routes de l application.
     * @param context AppContext Contexte de l application
     */
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

        route("/users")
            .to(UserController.class)
            .get()
            .action("index");

        route("/admin/users")
            .to(UserController.class)
            .get()
            .action("index");

        route("/users/{id}")
            .to(UserController.class)
            .get()
            .action("show");

        route("/admin/users/{id}")
            .to(UserController.class)
            .get()
            .action("show");

        route("/users")
            .to(UserController.class)
            .post()
            .action("create");

        route("/users/{id}")
            .to(UserController.class)
            .patch()
            .action("update");

        route("/admin/users/{id}")
            .to(UserController.class)
            .patch()
            .action("update");

        route("/users/{id}")
            .to(UserController.class)
            .delete()
            .action("destroy");

        route("/admin/users/{id}")
            .to(UserController.class)
            .delete()
            .action("destroy");

        route("/orders")
            .to(OrderController.class)
            .get()
            .action("index");

        route("/orders")
            .to(OrderController.class)
            .post()
            .action("create");

        route("/orders/{id}")
            .to(OrderController.class)
            .patch()
            .action("update");

        route("/orders/{id}")
            .to(OrderController.class)
            .delete()
            .action("destroy");

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
