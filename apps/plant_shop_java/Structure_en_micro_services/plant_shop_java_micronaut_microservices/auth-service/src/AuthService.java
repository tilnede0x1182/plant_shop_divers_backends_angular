import util.ServiceLauncher;

public final class AuthService {

    private AuthService() {}

    public static void main(String[] args) {
        ServiceLauncher.run("auth-service", "AUTH_SERVICE_PORT", 6101, args);
    }
}
