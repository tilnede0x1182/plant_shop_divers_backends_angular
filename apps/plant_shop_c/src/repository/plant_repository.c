#include "plant_repository.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

static void fill_plant(Plant *p, PGresult *r, int row) {
	p->id = atoi(PQgetvalue(r, row, 0));
	strncpy(p->name, PQgetvalue(r, row, 1), sizeof(p->name) - 1);
	p->name[sizeof(p->name) - 1] = '\0';
	strncpy(p->description, PQgetvalue(r, row, 2), sizeof(p->description) - 1);
	p->description[sizeof(p->description) - 1] = '\0';
	p->price = atoi(PQgetvalue(r, row, 3));
	p->stock = atoi(PQgetvalue(r, row, 4));
}

int plant_repo_add(PGconn *c, const Plant *p) {
	char price_str[12], stock_str[12];
	sprintf(price_str, "%.2f", (double)p->price);
	sprintf(stock_str, "%d", p->stock);
	const char *desc = p->description ? p->description : "";
	const char *v[4] = {p->name, desc, price_str, stock_str};

	PGresult *r = PQexecParams(
		c,
		"SELECT id,name,description,price,stock FROM plants WHERE id=$1",
		1, NULL, v, NULL, NULL, 0);

	if (PQresultStatus(r) != PGRES_TUPLES_OK) {
		fprintf(stderr, "plant_repo_add failed: %s\n", PQerrorMessage(c));
		PQclear(r);
		return 0;
	}
	int id = atoi(PQgetvalue(r, 0, 0));
	PQclear(r);
	return id;
}

int plant_repo_find(PGconn *c, int id, Plant *p) {
	char sid[12];
	sprintf(sid, "%d", id);
	const char *v[1] = {sid};

	PGresult *r = PQexecParams(
		c,
		"SELECT id,name,price,stock FROM plants WHERE id=$1",
		1, NULL, v, NULL, NULL, 0);

	int found = PQntuples(r);
	if (found) fill_plant(p, r, 0);
	PQclear(r);
	return found;
}

void plant_repo_patch(PGconn *c, int id, cJSON *j) {
	char sid[12];
	sprintf(sid, "%d", id);

	cJSON *price_json = cJSON_GetObjectItem(j, "price");
	if (price_json && cJSON_IsNumber(price_json)) {
		char price_str[12];
		sprintf(price_str, "%.2f", (double)price_json->valueint);
		const char *p[2] = {price_str, sid};
		PGresult *res = PQexecParams(
			c, "UPDATE plants SET price=$1 WHERE id=$2",
			2, NULL, p, NULL, NULL, 0);
		PQclear(res);
	}

	cJSON *stock_json = cJSON_GetObjectItem(j, "stock");
	if (stock_json && cJSON_IsNumber(stock_json)) {
		char stock_str[12];
		sprintf(stock_str, "%d", stock_json->valueint);
		const char *p[2] = {stock_str, sid};
		PGresult *res = PQexecParams(
			c, "UPDATE plants SET stock=$1 WHERE id=$2",
			2, NULL, p, NULL, NULL, 0);
		PQclear(res);
	}
}

void plant_repo_del(PGconn *c, int id) {
	char sid[12];
	sprintf(sid, "%d", id);
	const char *v[1] = {sid};

	PGresult *res = PQexecParams(
		c, "DELETE FROM plants WHERE id=$1",
		1, NULL, v, NULL, NULL, 0);
	PQclear(res);
}

void plant_repo_each(PGconn *c, void (*cb)(Plant*, void*), void *ctx) {
	PGresult *r = PQexec(c, "SELECT id,name,description,price,stock FROM plants");
	if (PQresultStatus(r) != PGRES_TUPLES_OK) {
		fprintf(stderr, "plant_repo_each failed: %s\n", PQerrorMessage(c));
		PQclear(r);
		return;
	}

	int rows = PQntuples(r);
	for (int i = 0; i < rows; i++) {
		Plant p;
		fill_plant(&p, r, i);
		cb(&p, ctx);
	}
	PQclear(r);
}
