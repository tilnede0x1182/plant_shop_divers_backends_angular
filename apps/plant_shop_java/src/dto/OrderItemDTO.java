package dto;

import java.math.BigDecimal;

public final class OrderItemDTO {
    public int        id;
    public int        plantId;
    public int        quantity;
    public BigDecimal price;     // prix unitaire enregistré à la commande

    public OrderItemDTO(){}
    public OrderItemDTO(int id,int pid,int qty,BigDecimal price){
        this.id=id; this.plantId=pid; this.quantity=qty; this.price=price;
    }
}
