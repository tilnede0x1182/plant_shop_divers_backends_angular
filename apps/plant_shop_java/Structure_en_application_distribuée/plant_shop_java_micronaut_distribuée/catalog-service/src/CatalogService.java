import util.ServiceLauncher;

/**
 * Point d'entrée du service catalogue.
 */
public final class CatalogService {

    /**
 * Constructeur privé.
 */
private CatalogService() {}

    /**
 * Lance le service catalogue.
 * @param args Arguments de la ligne de commande
 */
public static void main(String[] args) {
        ServiceLauncher.run("catalog-service", "CATALOG_SERVICE_PORT", 6102, args);
    }
}
