import util.ServiceLauncher;

public final class OrderService {

    private OrderService() {}

    public static void main(String[] args) {
        ServiceLauncher.run("order-service", "ORDER_SERVICE_PORT", 6103, args);
    }
}
