#ifndef MODEL_PLANT_H
#define MODEL_PLANT_H
/** """ Table plants
	@id    clé primaire
	@name  nom plante
	@desc  description courte
	@price prix entier € (aligné tests)
	@stock stock disponible """ */
typedef struct {
	int  id;
	char name[64];
	char description[128];
	int  price;   /* euros */
	int  stock;
} Plant;
#endif
