package controller;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.URI;
import java.sql.Connection;
import java.util.List;
import model.User;
import repository.UserRepository;

/**
 * Routes gérés :
 *   GET    /users            → liste
 *   GET    /users/{id}       → show
 *   POST   /users            → create   (JSON: name, email, password, isAdmin)
 *   PATCH  /users/{id}       → update   (JSON: name?, email?, isAdmin?)
 *   DELETE /users/{id}       → delete
 *
 * Réponses JSON basiques, HTTP 200/201/204/400/404
 */
public final class UserController implements HttpHandler {

    private final UserRepository repo;

    public UserController(Connection db) {
        this.repo = new UserRepository(db);
    }

    public void handle(HttpExchange ex) throws IOException {
        try {
            URI     uri    = ex.getRequestURI();
            String  path   = uri.getPath();            // /users | /users/42
            String  method = ex.getRequestMethod();    // GET / POST / PATCH / DELETE
            String[] seg   = path.split("/");
            boolean hasId  = seg.length == 3;          // /users/{id}

            if ("GET".equals(method) && !hasId) { list(ex);       return; }
            if ("GET".equals(method) &&  hasId) { show(ex, seg[2]);   return; }
            if ("POST".equals(method) && !hasId){ create(ex);     return; }
            if ("PATCH".equals(method) && hasId){ update(ex, seg[2]); return; }
            if ("DELETE".equals(method)&& hasId){ destroy(ex, seg[2]);return; }

            send(ex, 404, "{\"error\":\"Not Found\"}");
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, "{\"error\":\""+e.getMessage()+"\"}");
        }
    }

    /* ---------- Actions ---------- */

    private void list(HttpExchange ex) throws Exception {
        List<User> all = repo.list();
        StringBuilder sb = new StringBuilder("[");
        for (int i=0;i<all.size();i++) {
            if (i>0) sb.append(',');
            sb.append(toJson(all.get(i), false));
        }
        sb.append(']');
        send(ex, 200, sb.toString());
    }

    private void show(HttpExchange ex, String idStr) throws Exception {
        int id = Integer.parseInt(idStr);
        User u = repo.find(id);
        if (u==null) { send(ex,404,"{\"error\":\"Not Found\"}"); return; }
        send(ex,200,toJson(u,false));
    }

    private void create(HttpExchange ex) throws Exception {
        String body = read(ex);
        // parse minimal : name,email,password,isAdmin
        String name  = getJson(body,"name");
        String email = getJson(body,"email");
        String pass  = getJson(body,"password");
        boolean adm  = "true".equalsIgnoreCase(getJson(body,"isAdmin"));
        if (name==null||email==null||pass==null) {
            send(ex,400,"{\"error\":\"Missing fields\"}");
            return;
        }
        int id = repo.create(new User(name,email,hash(pass),adm));
        send(ex,201,"{\"id\":"+id+"}");
    }

    private void update(HttpExchange ex, String idStr) throws Exception {
        int id = Integer.parseInt(idStr);
        User u = repo.find(id);
        if (u==null){ send(ex,404,"{\"error\":\"Not Found\"}"); return; }

        String body = read(ex);
        String name  = getJson(body,"name");
        String email = getJson(body,"email");
        String isAd  = getJson(body,"isAdmin");

        if (name!=null)  u.name   = name;
        if (email!=null) u.email  = email;
        if (isAd!=null)  u.isAdmin= "true".equalsIgnoreCase(isAd);

        repo.update(u);
        send(ex,200,toJson(u,false));
    }

    private void destroy(HttpExchange ex, String idStr) throws Exception {
        int id = Integer.parseInt(idStr);
        repo.delete(id);
        sendEmpty(ex,204);
    }

    /* ---------- Helpers JSON/minis ---------- */

    private static String toJson(User u, boolean withPwd){
        return "{\"id\":"+u.id+
               ",\"name\":\""+esc(u.name)+
               "\",\"email\":\""+esc(u.email)+
               "\",\"isAdmin\":"+(u.isAdmin?"true":"false")+
               (withPwd ? ",\"passwordHash\":\""+esc(u.passwordHash)+"\"" : "")+
               "}";
    }
    private static String esc(String s){ return s==null?"":s.replace("\"","\\\""); }
    private static String read(HttpExchange ex) throws IOException{
        BufferedReader br = new BufferedReader(new InputStreamReader(ex.getRequestBody(),"UTF-8"));
        StringBuilder sb = new StringBuilder(); String l;
        while((l=br.readLine())!=null) sb.append(l);
        return sb.toString();
    }
    private static String getJson(String json,String key){
        String pattern="\""+key+"\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        return m.find()?m.group(1):null;
    }
    private static String hash(String pw){ return "h$"+pw; }     // stub
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
