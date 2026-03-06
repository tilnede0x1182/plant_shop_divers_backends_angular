package util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Order;

/**
 * Utilitaire de mapping pour l'API.
 */
public final class ApiMapper {

    /** Constructeur privé. */
    private ApiMapper() {}

    /**
     * Convertit une commande en Map pour l'API.
     * @param order Order Commande à convertir
     * @param items List Items de la commande
     * @return Map Représentation JSON
     */
    public static Map<String, Object> toOrder(Order order, List<Map<String, Object>> items) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", order.id);
        map.put("userId", order.userId);
        map.put("totalPrice", toDecimal(order.total));
        map.put("status", order.status);
        map.put("createdAt", toIso(order.createdAt));
        map.put("orderItems", items);
        return map;
    }

    /**
     * Convertit un BigDecimal en String.
     * @param bd BigDecimal Valeur à convertir
     * @return String Valeur convertie
     */
    private static String toDecimal(BigDecimal bd) {
        return bd != null ? bd.toPlainString() : "0";
    }

    /**
     * Convertit un Timestamp en String ISO.
     * @param ts Timestamp Valeur à convertir
     * @return String Format ISO
     */
    private static String toIso(Timestamp ts) {
        return (ts != null) ?
            ts.toInstant().atOffset(ZoneOffset.UTC).toString() : null;
    }
}
