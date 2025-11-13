package model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.math.BigDecimal;

@Introspected
@Serdeable
public final class PlantStock {
    public final int id;
    public final String name;
    public final BigDecimal price;
    public final int stock;

    public PlantStock(int id, String name, BigDecimal price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }
}
