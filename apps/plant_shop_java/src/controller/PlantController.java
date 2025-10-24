package controller;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.URI;
import java.sql.Connection;
import java.util.List;
import model.Plant;
import repository.PlantRepository;

public final class PlantController implements HttpHandler {

    private final PlantRepository repo;
    public PlantController(Connection db){ this.repo=new PlantRepository(db); }

    public void handle(HttpExchange ex) throws IOException {
        try{
            URI uri = ex.getRequestURI();
            String path = uri.getPath();                 // /plants | /plants/3
            String method = ex.getRequestMethod();
            String[] seg = path.split("/");
            boolean hasId = seg.length==3;

            if("GET".equals(method)&&!hasId){ list(ex); return; }
            if("GET".equals(method)&& hasId){ show(ex,seg[2]); return; }
            if("POST".equals(method)&&!hasId){ create(ex); return; }
            if("PATCH".equals(method)&& hasId){ update(ex,seg[2]); return; }
            if("DELETE".equals(method)&&hasId){ destroy(ex,seg[2]); return; }

            send(ex,404,"{\"error\":\"Not Found\"}");
        }catch(Exception e){
            e.printStackTrace();
            send(ex,500,"{\"error\":\""+e.getMessage()+"\"}");
        }
    }

    /* ---------- Actions ---------- */

    private void list(HttpExchange ex) throws Exception{
        List<Plant> all = repo.list();
        StringBuilder sb=new StringBuilder("[");
        for(int i=0;i<all.size();i++){
            if(i>0) sb.append(',');
            sb.append(toJson(all.get(i)));
        }
        sb.append(']');
        send(ex,200,sb.toString());
    }

    private void show(HttpExchange ex,String idStr) throws Exception{
        int id=Integer.parseInt(idStr);
        Plant p=repo.find(id);
        if(p==null){ send(ex,404,"{\"error\":\"Not Found\"}"); return; }
        send(ex,200,toJson(p));
    }

    private void create(HttpExchange ex)throws Exception{
        String body=read(ex);
        String name = getJson(body,"name");
        String desc = getJson(body,"description");
        String price= getJson(body,"price");
        String stock= getJson(body,"stock");
        if(name==null||price==null){ send(ex,400,"{\"error\":\"name & price required\"}"); return; }

        java.math.BigDecimal pr = new java.math.BigDecimal(price);
        int st = stock!=null?Integer.parseInt(stock):0;
        int id = repo.create(new Plant(name,desc,pr,st));
        send(ex,201,"{\"id\":"+id+"}");
    }

    private void update(HttpExchange ex,String idStr)throws Exception{
        int id=Integer.parseInt(idStr);
        Plant p=repo.find(id);
        if(p==null){ send(ex,404,"{\"error\":\"Not Found\"}"); return; }

        String body=read(ex);
        String name = getJson(body,"name");
        String desc = getJson(body,"description");
        String price= getJson(body,"price");
        String stock= getJson(body,"stock");

        if(name!=null) p.name=name;
        if(desc!=null) p.description=desc;
        if(price!=null) p.price=new java.math.BigDecimal(price);
        if(stock!=null) p.stock=Integer.parseInt(stock);

        repo.update(p);
        send(ex,200,toJson(p));
    }

    private void destroy(HttpExchange ex,String idStr)throws Exception{
        int id=Integer.parseInt(idStr);
        repo.delete(id);
        sendEmpty(ex,204);
    }

    /* ---------- Helpers ---------- */

    private static String toJson(Plant p){
        return "{\"id\":"+p.id+
               ",\"name\":\""+esc(p.name)+
               "\",\"description\":"+(p.description==null?"null":"\""+esc(p.description)+"\"")+
               ",\"price\":"+p.price+
               ",\"stock\":"+p.stock+
               "}";
    }
    private static String esc(String s){ return s==null?"":s.replace("\"","\\\""); }
    private static String read(HttpExchange ex)throws IOException{
        BufferedReader br=new BufferedReader(new InputStreamReader(ex.getRequestBody(),"UTF-8"));
        StringBuilder sb=new StringBuilder(); String l;
        while((l=br.readLine())!=null) sb.append(l);
        return sb.toString();
    }
    private static String getJson(String json,String key){
        String pattern="\""+key+"\"\\s*:\\s*\"?([^\"]*?)\"?(,|})";
        java.util.regex.Matcher m=java.util.regex.Pattern.compile(pattern).matcher(json);
        return m.find()?m.group(1):null;
    }
    private static void send(HttpExchange ex,int code,String body)throws IOException{
        byte[] bytes=body.getBytes("UTF-8");
        ex.getResponseHeaders().add("Content-Type","application/json; charset=utf-8");
        ex.sendResponseHeaders(code,bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }
    private static void sendEmpty(HttpExchange ex,int code)throws IOException{
        ex.sendResponseHeaders(code,-1); ex.close();
    }
}
