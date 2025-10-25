#ifndef REPO_PLANT_H
#define REPO_PLANT_H

#include <libpq-fe.h>
#include <cjson/cJSON.h>
#include "../models/plant.h"

int  plant_repo_add(PGconn*, const Plant*);
int  plant_repo_find(PGconn*, int, Plant*);
void plant_repo_patch(PGconn*, int, cJSON*);
void plant_repo_del(PGconn*, int);
void plant_repo_each(PGconn*, void(*)(Plant*, void*), void*);

#endif
