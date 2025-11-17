package util;

import java.math.BigDecimal;

public final class DecimalMapper {

    private DecimalMapper() {}

    public static Double toDecimal(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
