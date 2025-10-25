#ifndef REPO_PLANT_H
#define REPO_PLANT_H
#include <libpq-fe.h>
#include "../models/plant.h"

int  plant_repo_insert(PGconn*,const Plant*);
int  plant_repo_find(PGconn*,int,Plant*);
void plant_repo_update_price(PGconn*,int,int);
void plant_repo_update_stock(PGconn*,int,int);
void plant_repo_delete(PGconn*,int);
void plant_repo_all(PGconn*,void(*)(Plant*,void*),void*);
#endif
