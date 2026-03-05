package controller;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.URI;
import java.sql.Connection;
import java.util.List;
import model.OrderItem;
import repository.OrderItemRepository;

/**
 * Routes
 *   GET    /orders/{id}/items        → liste items d'une commande
 *   DELETE /orders/{id}/items        → flush items d'une commande
 */
public final class OrderItemController implements HttpHandler {

    private final OrderItemRepository repo;

    /**
     * Constructeur du contrôleur d'items de commande.
     * @param db Connection Connexion à la base de données
     */
    public OrderItemController(Connection db){ this.repo=new OrderItemRepository(db); }

    /**
     * Dispatche les requêtes vers les méthodes CRUD.
     * @param ex HttpExchange Échange HTTP
     * @throws IOException En cas d'erreur I/O
     */
    public void handle(HttpExchange ex) throws IOException {
        try{
            URI uri = ex.getRequestURI();                 // /orders/5/items
            String[] seg = uri.getPath().split("/");
            if(seg.length!=4 || !"orders".equals(seg[1]) || !"items".equals(seg[3])){
                send(ex,404,"{\"error\":\"Not Found\"}"); return;
            }
            int orderId=Integer.parseInt(seg[2]);
            String m=ex.getRequestMethod();

            if("GET".equals(m)){ list(ex,orderId); return; }
            if("DELETE".equals(m)){ flush(ex,orderId); return; }

            send(ex,404,"{\"error\":\"Not Found\"}");
        }catch(Exception e){
            e.printStackTrace();
            send(ex,500,"{\"error\":\""+e.getMessage()+"\"}");
        }
    }

    /**
     * Liste les items d'une commande.
     * @param ex HttpExchange Échange HTTP
     * @param orderId int ID de la commande
     * @throws Exception En cas d'erreur
     */
    private void list(HttpExchange ex,int orderId)throws Exception{
        List<OrderItem> items=repo.listByOrder(orderId);
        StringBuilder sb=new StringBuilder("[");
        for(int i=0;i<items.size();i++){
            if(i>0) sb.append(',');
            OrderItem it=items.get(i);
            sb.append("{\"id\":").append(it.id)
              .append(",\"plantId\":").append(it.plantId)
              .append(",\"quantity\":").append(it.quantity)
              .append(",\"price\":").append(it.price).append('}');
        }
        sb.append(']');
        send(ex,200,sb.toString());
    }

    /**
     * Supprime tous les items d'une commande.
     * @param ex HttpExchange Échange HTTP
     * @param orderId int ID de la commande
     * @throws Exception En cas d'erreur
     */
    private void flush(HttpExchange ex,int orderId)throws Exception{
        repo.deleteByOrder(orderId);
        sendEmpty(ex,204);
    }

    /**
     * Envoie une réponse JSON.
     *
     * @param ex HttpExchange Échange HTTP
     * @param code int Code HTTP
     * @param body String Corps de la réponse
     */
    private static void send(HttpExchange ex,int code,String body)throws IOException{
        byte[] b=body.getBytes("UTF-8");
        ex.getResponseHeaders().add("Content-Type","application/json; charset=utf-8");
        ex.sendResponseHeaders(code,b.length);
        ex.getResponseBody().write(b); ex.close();
    }
    /**
     * Envoie une réponse vide.
     * @param ex HttpExchange Échange HTTP
     * @param code int Code HTTP
     * @throws IOException En cas d'erreur I/O
     */
    private static void sendEmpty(HttpExchange ex,int code)throws IOException{
        ex.sendResponseHeaders(code,-1); ex.close();
    }
}
