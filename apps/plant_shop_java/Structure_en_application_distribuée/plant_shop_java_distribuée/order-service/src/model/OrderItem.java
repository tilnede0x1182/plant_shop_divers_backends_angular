import java.math.BigDecimal;

public final class OrderItem {
    int id;
    int orderId;
    int plantId;
    int quantity;
    BigDecimal price;

    public OrderItem(int id, int orderId, int plantId, int quantity, BigDecimal price) {
        this.id = id;
        this.orderId = orderId;
        this.plantId = plantId;
        this.quantity = quantity;
        this.price = price;
    }
}

final class Plant {
    int id;
    String name;
    BigDecimal price;
    int stock;

    Plant(int id, String name, BigDecimal price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }
}
