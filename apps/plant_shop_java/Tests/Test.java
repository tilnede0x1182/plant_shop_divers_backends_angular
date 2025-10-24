package Tests;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.json.JSONObject;

/**
 * Test end-to-end Java 1.6, aligné sur le scénario Rust.
 * – Aucune dépendance hors org.json.
 * – Base URL et port tirés du .env (même logique que la seed Java).
 */
public final class Test {

    /* -------- .env -------- */
    private static Map<String,String> env() throws IOException {
        Map<String,String> m = new HashMap<String,String>();
        BufferedReader br = new BufferedReader(new FileReader(".env"));
        String l; while ((l = br.readLine()) != null) {
            int i = l.indexOf('=');
            if(i>0) m.put(l.substring(0,i).trim(), l.substring(i+1).trim());
        }
        br.close(); return m;
    }

    /* -------- Config -------- */
    private static final Map<String,String> CFG;
    static {
        try { CFG = env(); }
        catch(Exception e){ throw new RuntimeException(e); }
    }
    private static final String PORT  = CFG.get("SERVER_ADDRESS")!=null ?
                                        CFG.get("SERVER_ADDRESS") : "4100";
    private static final String BASE  = "http://localhost:"+PORT;
    private static final String ADMIN_EMAIL = "admin1@planteshop.com";
    private static final String ADMIN_PWD   = "password";

    /* -------- Cookies -------- */
    private final Map<String,String> cookie = new HashMap<String,String>();

    /* -------- Utilitaires -------- */
    private static String ts(){ return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()); }
    private static String rand(int n){
        String a="abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb=new StringBuilder();
        Random r=new Random();
        for(int i=0;i<n;i++) sb.append(a.charAt(r.nextInt(a.length())));
        return sb.toString();
    }

    /* -------- HTTP -------- */
    private JSONObject call(String m,String p,int exp,JSONObject body,String who) throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(BASE+p).openConnection();
        c.setRequestMethod(m);
        c.setRequestProperty("Content-Type","application/json");
        if(cookie.get(who)!=null) c.setRequestProperty("Cookie",cookie.get(who));
        if(body!=null){
            c.setDoOutput(true);
            OutputStream os=c.getOutputStream();
            os.write(body.toString().getBytes("UTF-8"));
            os.close();
        }
        int code=c.getResponseCode();
        String set=c.getHeaderField("Set-Cookie");
        if(set!=null) cookie.put(who,set.split(";",2)[0]);

        System.out.printf("%s %-6s %s [%d]%n",
            code==exp?"✅":"❌",m,p,code);

        if(code!=exp){
            InputStream er=c.getErrorStream(); if(er==null) er=c.getInputStream();
            throw new RuntimeException(stream(er));
        }
        if(c.getHeaderField("Content-Type")!=null &&
           c.getHeaderField("Content-Type").startsWith("application/json")){
            String txt = stream(c.getInputStream());
            return txt.trim().isEmpty()?new JSONObject():new JSONObject(txt);
        }
        return new JSONObject();
    }
    private static String stream(InputStream is)throws IOException{
        if(is==null) return "";
        BufferedReader br=new BufferedReader(new InputStreamReader(is,"UTF-8"));
        StringBuilder sb=new StringBuilder(); String l;
        while((l=br.readLine())!=null) sb.append(l);
        return sb.toString();
    }

    /* -------- Auth -------- */
    private void login(String mail,String pw,String who)throws Exception{
        JSONObject j=new JSONObject().put("email",mail).put("password",pw);
        call("POST","/auth/login",200,j,who);
    }
    private void register(String name,String mail,String pw,String who)throws Exception{
        JSONObject j=new JSONObject().put("name",name).put("email",mail).put("password",pw);
        call("POST","/auth/register",201,j,who);
    }

    /* -------- Assertions -------- */
    private static void eq(JSONObject o,String k,Object e){
        Object a=o.opt(k);
        if(e==null ? a!=null : !e.equals(a))
            throw new RuntimeException("Mismatch "+k+"="+a+" attendu "+e);
    }
    private static void num(JSONObject o,String k){
        if(!(o.opt(k) instanceof Number))
            throw new RuntimeException("Key "+k+" not numeric");
    }

    /* -------- Tests -------- */
    private void testPlants() throws Exception{
        System.out.println("\n📌 PLANTS");
        JSONObject req=new JSONObject().put("name","Plante-test").put("price",9.9).put("stock",3);
        JSONObject pl=call("POST","/plants",201,req,"admin");   // création
        num(pl,"id"); int id=pl.getInt("id");

        JSONObject get=call("GET","/plants/"+id,200,null,"admin");
        eq(get,"name","Plante-test");

        call("PATCH","/plants/"+id,200,new JSONObject().put("stock",8),"admin");
        JSONObject chk=call("GET","/plants/"+id,200,null,"admin");
        eq(chk,"stock",8);

        call("DELETE","/plants/"+id,204,null,"admin");
    }

    private void testUsers() throws Exception{
        System.out.println("\n📌 USERS");
        String mail="u_"+ts()+"_"+rand(4)+"@mail.test";
        JSONObject u=new JSONObject().put("name","User").put("email",mail).put("password","pw");
        JSONObject r=call("POST","/users",201,u,"admin");
        int id=r.getInt("id");

        call("PATCH","/users/"+id,200,new JSONObject().put("name","UserX"),"admin");
        JSONObject chk=call("GET","/users/"+id,200,null,"admin");
        eq(chk,"name","UserX");

        call("DELETE","/users/"+id,204,null,"admin");
    }

    /* -------- Main -------- */
    public static void main(String[] args) throws Exception{
        // wait server 5s
        long t0=System.currentTimeMillis();
        while(true){
            try{ new Socket("127.0.0.1",Integer.parseInt(PORT)).close(); break; }
            catch(IOException e){ if(System.currentTimeMillis()-t0>5000) System.exit(1); Thread.sleep(200); }
        }

        Test t=new Test();
        t.login(ADMIN_EMAIL,ADMIN_PWD,"admin");

        String mail="cli_"+ts()+"@mail.test";
        t.register("Client",mail,"pass","client");
        t.login(mail,"pass","client");

        t.testPlants();
        t.testUsers();

        System.out.println("\n🎉  Tous les tests OK");
    }
}
