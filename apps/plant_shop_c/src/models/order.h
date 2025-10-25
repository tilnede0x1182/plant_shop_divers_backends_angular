#ifndef MODEL_ORDER_H
#define MODEL_ORDER_H
/** """ Table orders
	@id      clé primaire
	@user_id FK users
	@total   somme en € (entier)
	@status  confirmed/pending/shipped/delivered """ */
typedef struct {
	int  id;
	int  user_id;
	int  total;          /* euros */
	char status[12];
} Order;
#endif
