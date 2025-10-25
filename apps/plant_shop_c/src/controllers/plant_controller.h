#ifndef CTRL_PLANT_H
#define CTRL_PLANT_H

#include <regex.h>
#include <kore/http.h>

int plant_get(struct http_request*);
int admin_plants_list(struct http_request*);
int admin_plants_add(struct http_request*);
int admin_plants_patch(struct http_request*);
int admin_plants_del(struct http_request*);

#endif
