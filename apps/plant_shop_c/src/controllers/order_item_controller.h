#ifndef CTRL_ORDER_ITEM_H
#define CTRL_ORDER_ITEM_H
#include <kore/http.h>

/* GET /orders/:id/items   → liste JSON                               */
int order_items_by_order(struct http_request*);

/* PATCH /order-items/:id   → maj quantité / price (admin)            */
int order_item_patch(struct http_request*);

/* DELETE /order-items/:id  → supprime un item (admin)                */
int order_item_del(struct http_request*);

#endif
