package models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.BelongsToParents;
import org.javalite.activejdbc.annotations.Table;

@Table("order_items")
@BelongsToParents({
	@BelongsTo(parent = Order.class, foreignKeyName = "order_id"),
	@BelongsTo(parent = Plant.class, foreignKeyName = "plant_id")
})
/**
 * Modèle ActiveJDBC pour les items de commande.
 */
public class OrderItem extends Model {}
