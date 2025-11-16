import controller.AuthController;
import repository.UserRepository;
import util.AuthMiddleware;
import util.ServiceRuntime;

public final class AuthService {

    public static void main(String[] args) throws Exception {
        ServiceRuntime.start(
            ServiceRuntime.descriptor("auth-service", "AUTH_SERVICE_PORT", 6101),
            (app, db) -> {
                AuthController authController = new AuthController(db);
                UserRepository userRepository = new UserRepository(db);

                app.post("/auth/register", authController::register);
                app.post("/auth/login", authController::login);
                app.post("/auth/logout", AuthMiddleware.requireUser(
                    auth -> userRepository.find(auth.userId()), authController::logout));
                app.get("/auth/me", AuthMiddleware.requireUser(
                    auth -> userRepository.find(auth.userId()), authController::me));
                app.get("/auth/_session", authController::sessionStatus);
            }
        );
    }
}
