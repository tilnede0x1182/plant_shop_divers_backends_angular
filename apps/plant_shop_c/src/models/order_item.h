#ifndef MODEL_ORDER_ITEM_H
#define MODEL_ORDER_ITEM_H
/** """ Table order_items
	@id       clé primaire
	@order_id FK orders
	@plant_id FK plants
	@qty      quantité
	@price    prix unitaire € """ */
typedef struct {
	int id;
	int order_id;
	int plant_id;
	int qty;
	int price;           /* euros */
} OrderItem;
#endif
