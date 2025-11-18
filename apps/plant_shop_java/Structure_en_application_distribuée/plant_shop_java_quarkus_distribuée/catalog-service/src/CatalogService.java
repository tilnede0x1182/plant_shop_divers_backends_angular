import util.ServiceLauncher;

public final class CatalogService {

    private CatalogService() {}

    public static void main(String[] args) {
        ServiceLauncher.run("catalog-service", "CATALOG_SERVICE_PORT", 6102, args);
    }
}
