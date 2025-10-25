#ifndef CTRL_PLANT_H
#define CTRL_PLANT_H

#include "mongoose/mongoose.h"

void plant_get(struct mg_connection *c, struct mg_http_message *hm, int id);
void admin_plants_list(struct mg_connection *c, struct mg_http_message *hm);
void admin_plants_add(struct mg_connection *c, struct mg_http_message *hm);
void admin_plants_patch(struct mg_connection *c, struct mg_http_message *hm, int id);
void admin_plants_del(struct mg_connection *c, struct mg_http_message *hm, int id);

#endif
