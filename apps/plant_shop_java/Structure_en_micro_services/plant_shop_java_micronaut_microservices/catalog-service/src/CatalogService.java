import util.ServiceLauncher;

/**
 * Point d'entrée du microservice catalog.
 */
public final class CatalogService {

    /**
     * Constructeur privé.
     */
    private CatalogService() {}

    /**
     * Lance le service catalog sur le port configuré.
     * @param args Arguments CLI
     */
    public static void main(String[] args) {
        ServiceLauncher.run("catalog-service", "CATALOG_SERVICE_PORT", 6102, args);
    }
}
