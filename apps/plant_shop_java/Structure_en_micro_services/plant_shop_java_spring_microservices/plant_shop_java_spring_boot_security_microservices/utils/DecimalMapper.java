package util;

import java.math.BigDecimal;

/** Utilitaire de conversion BigDecimal vers Double */
public final class DecimalMapper {

    /** Constructeur prive (classe utilitaire) */
    private DecimalMapper() {}

    /** Convertit un BigDecimal en Double */
    public static Double toDecimal(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
