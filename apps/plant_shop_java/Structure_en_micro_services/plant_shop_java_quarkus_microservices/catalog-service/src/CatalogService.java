import util.ServiceLauncher;

/**
 * Point d'entrée du service de catalogue.
 * Lance le serveur Quarkus sur le port configuré.
 */
public final class CatalogService {

    /**
     * Constructeur privé pour empêcher l'instanciation.
     */
    private CatalogService() {}

    /**
     * Point d'entrée principal du service.
     *
     * @param args Arguments de ligne de commande
     */
    public static void main(String[] args) {
        ServiceLauncher.run("catalog-service", "CATALOG_SERVICE_PORT", 6102, args);
    }
}
