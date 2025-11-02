package models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.HasMany;
import org.javalite.activejdbc.annotations.Table;

@Table("orders")
@BelongsTo(parent = User.class, foreignKeyName = "user_id")
@HasMany(child = OrderItem.class, foreignKeyName = "order_id")
public class Order extends Model {}
