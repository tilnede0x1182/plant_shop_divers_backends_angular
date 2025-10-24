package dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

public final class OrderDTO {
    public int               id;
    public int               userId;
    public BigDecimal        total;
    public String            status;
    public Timestamp         createdAt;
    public List<OrderItemDTO> items;   // nullable si non chargé

    public OrderDTO(){}
    public OrderDTO(int id,int uid,BigDecimal tot,String st,Timestamp ts,List<OrderItemDTO> its){
        this.id=id; userId=uid; total=tot; status=st; createdAt=ts; items=its;
    }
}
