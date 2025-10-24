package dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

public final class PlantDTO {
    public int         id;
    public String      name;
    public String      description;   // nullable
    public BigDecimal  price;
    public int         stock;
    public Timestamp   createdAt;

    public PlantDTO(){}
    public PlantDTO(int id,String n,String d,BigDecimal p,int s,Timestamp ts){
        this.id=id; name=n; description=d; price=p; stock=s; createdAt=ts;
    }
}
