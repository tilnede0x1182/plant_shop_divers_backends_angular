import util.ServiceLauncher;

public final class UserService {

    private UserService() {}

    public static void main(String[] args) {
        ServiceLauncher.run("user-service", "USER_SERVICE_PORT", 6104, args);
    }
}
