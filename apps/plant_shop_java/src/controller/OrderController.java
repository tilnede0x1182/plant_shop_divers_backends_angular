package controller;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.URI;
import java.sql.Connection;
import java.util.*;
import java.math.BigDecimal;
import model.Order;
import model.OrderItem;
import repository.OrderRepository;
import repository.OrderItemRepository;

/**
 * Routes
 *   GET    /orders           → liste
 *   GET    /orders/{id}      → show (+ items)
 *   POST   /orders           → create (JSON: userId, items:[{plantId,qty,price},...])
 *   PATCH  /orders/{id}      → update status (JSON: status)
 *   DELETE /orders/{id}      → delete cascade
 */
public final class OrderController implements HttpHandler {

    private final OrderRepository repo;
    private final OrderItemRepository itemRepo;

    public OrderController(Connection db) {
        this.repo     = new OrderRepository(db);
        this.itemRepo = new OrderItemRepository(db);
    }

    public void handle(HttpExchange ex) throws IOException {
        try {
            URI   uri   = ex.getRequestURI();
            String path = uri.getPath();                 // /orders | /orders/5
            String m    = ex.getRequestMethod();
            String[] seg = path.split("/");
            boolean hasId = seg.length == 3;

            if("GET".equals(m)   && !hasId){ list(ex); return; }
            if("GET".equals(m)   &&  hasId){ show(ex, seg[2]); return; }
            if("POST".equals(m)  && !hasId){ create(ex); return; }
            if("PATCH".equals(m) &&  hasId){ patch(ex, seg[2]); return; }
            if("DELETE".equals(m)&&  hasId){ destroy(ex, seg[2]); return; }

            send(ex,404,"{\"error\":\"Not Found\"}");
        } catch (Exception e) {
            e.printStackTrace();
            send(ex,500,"{\"error\":\""+e.getMessage()+"\"}");
        }
    }

    /* -------- Actions -------- */

    private void list(HttpExchange ex) throws Exception{
        List<Order> all = repo.list();
        StringBuilder sb=new StringBuilder("[");
        for(int i=0;i<all.size();i++){
            if(i>0) sb.append(',');
            sb.append(toJson(all.get(i), false));
        }
        sb.append(']');
        send(ex,200,sb.toString());
    }

    private void show(HttpExchange ex,String idStr) throws Exception{
        int id=Integer.parseInt(idStr);
        Order o = repo.find(id);
        if(o==null){ send(ex,404,"{\"error\":\"Not Found\"}"); return; }
        List<model.OrderItem> items = itemRepo.listByOrder(id);
        StringBuilder j = new StringBuilder(toJson(o,false));
        j.insert(j.length()-1,",\"items\":"+itemsArray(items)+"}");
        send(ex,200,j.toString());
    }

    private void create(HttpExchange ex) throws Exception{
        String body = read(ex);
        String uidStr = getJson(body,"userId");
        if(uidStr==null){ send(ex,400,"{\"error\":\"userId required\"}"); return; }
        int userId = Integer.parseInt(uidStr);

        List<Map<String,String>> items = parseItems(body);
        if(items.isEmpty()){ send(ex,400,"{\"error\":\"items required\"}"); return; }

        BigDecimal total = BigDecimal.ZERO;
        int orderId = repo.create(new Order(userId, total,"pending"));

        for(Map<String,String> it: items){
            int plant = Integer.parseInt(it.get("plantId"));
            int qty   = Integer.parseInt(it.get("quantity"));
            BigDecimal price = new BigDecimal(it.get("price"));
            total = total.add(price.multiply(new BigDecimal(qty)));
            itemRepo.addItem(new OrderItem(orderId, plant, qty, price));
        }
        repo.updateTotal(orderId,total);
        send(ex,201,"{\"id\":"+orderId+",\"total\":"+total+"}");
    }

    private void patch(HttpExchange ex,String idStr)throws Exception{
        int id=Integer.parseInt(idStr);
        Order o=repo.find(id);
        if(o==null){ send(ex,404,"{\"error\":\"Not Found\"}"); return; }

        String body=read(ex);
        String status=getJson(body,"status");
        if(status!=null){
            o.status=status;
            PreparedStatement ps = repo.db().prepareStatement(
                "UPDATE orders SET status=? WHERE id=?");
            ps.setString(1,status); ps.setInt(2,id); ps.executeUpdate();
        }
        send(ex,200,toJson(o,false));
    }

    private void destroy(HttpExchange ex,String idStr)throws Exception{
        int id=Integer.parseInt(idStr);
        itemRepo.deleteByOrder(id);
        repo.delete(id);
        sendEmpty(ex,204);
    }

    /* -------- Helpers -------- */

    private static String toJson(Order o, boolean withItems){
        return "{\"id\":"+o.id+
               ",\"userId\":"+o.userId+
               ",\"total\":"+o.total+
               ",\"status\":\""+o.status+"\""+
               (withItems?",\"items\":[]":"")+
               "}";
    }
    private static String itemsArray(List<OrderItem> list){
        StringBuilder sb=new StringBuilder("[");
        for(int i=0;i<list.size();i++){
            if(i>0) sb.append(',');
            OrderItem it=list.get(i);
            sb.append("{\"id\":").append(it.id)
              .append(",\"plantId\":").append(it.plantId)
              .append(",\"quantity\":").append(it.quantity)
              .append(",\"price\":").append(it.price).append('}');
        }
        sb.append(']');
        return sb.toString();
    }

    /* naive JSON parsing */
    private static String getJson(String json,String key){
        String pat="\""+key+"\"\\s*:\\s*\"?([^\"]*?)\"?(,|})";
        java.util.regex.Matcher m=java.util.regex.Pattern.compile(pat).matcher(json);
        return m.find()?m.group(1):null;
    }
    private static String read(HttpExchange ex)throws IOException{
        BufferedReader br=new BufferedReader(new InputStreamReader(ex.getRequestBody(),"UTF-8"));
        StringBuilder sb=new StringBuilder(); String l;
        while((l=br.readLine())!=null) sb.append(l);
        return sb.toString();
    }
    private static List<Map<String,String>> parseItems(String json){
        List<Map<String,String>> out=new ArrayList<Map<String,String>>();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\{([^}]*)}");
        java.util.regex.Matcher  m = p.matcher(json);
        while(m.find()){
            String obj=m.group(1);
            Map<String,String> map=new HashMap<String,String>();
            for(String k:new String[]{"plantId","quantity","price"}){
                String v=getJson("{"+obj+"}",k);
                if(v!=null) map.put(k,v);
            }
            if(map.size()==3) out.add(map);
        }
        return out;
    }
    private static void send(HttpExchange ex,int code,String body)throws IOException{
        byte[] b=body.getBytes("UTF-8");
        ex.getResponseHeaders().add("Content-Type","application/json; charset=utf-8");
        ex.sendResponseHeaders(code,b.length);
        ex.getResponseBody().write(b); ex.close();
    }
    private static void sendEmpty(HttpExchange ex,int code)throws IOException{
        ex.sendResponseHeaders(code,-1); ex.close();
    }
}
