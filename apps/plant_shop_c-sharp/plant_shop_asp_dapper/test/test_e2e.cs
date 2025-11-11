using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Newtonsoft.Json.Linq; // Remplace org.json

namespace Tests; // Remplace package Tests

/**
 * Test end-to-end .NET 8 – aligné sur le scénario C++ complet.
 * Dépendance : Newtonsoft.Json.
 */
public sealed class Test
{
    /* -------- .env -------- */
    private static Dictionary<string, string> Env()
    {
        var m = new Dictionary<string, string>();
        string? envPath = ResolveEnvPath();
        if (envPath == null)
        {
            Console.WriteLine("⚠️ Fichier .env non trouvé (test). Valeurs par défaut utilisées.");
            return m;
        }

        try
        {
            using var br = new StreamReader(envPath);
            string? l;
            while ((l = br.ReadLine()) != null)
            {
                int i = l.IndexOf('=');
                if (i > 0)
                    m[l.Substring(0, i).Trim()] = l.Substring(i + 1).Trim();
            }
        }
        catch (IOException e)
        {
            Console.WriteLine($"Erreur lecture .env: {e.Message}");
        }
        return m;
    }

    private static string? ResolveEnvPath()
    {
        string? current = Directory.GetCurrentDirectory();
        for (int i = 0; i < 8 && current != null; i++)
        {
            string candidate = Path.Combine(current, ".env");
            if (File.Exists(candidate))
            {
                return candidate;
            }
            current = Directory.GetParent(current)?.FullName;
        }
        return null;
    }

    /* -------- Config -------- */
    private static readonly Dictionary<string, string> CFG = Env();
    private static readonly string BASE = ResolveBaseUrl();
    private static readonly string ADMIN_EMAIL = "admin1@planteshop.com";
    private static readonly string ADMIN_PWD = "password";

    // En C#, HttpClient est conçu pour être réutilisé (statique)
    // Le créer à chaque 'call' (comme en Java 11+) causerait une exhaustion de sockets en .NET
    private static readonly HttpClient client = new HttpClient();

    /* -------- Cookies -------- */
    // La gestion manuelle des cookies est conservée pour répliquer le test Java
    private readonly Dictionary<string, string> cookie = new Dictionary<string, string>();
    private readonly string timestamp;

    public Test()
    {
        this.timestamp = Ts();
    }

    /* -------- Utilitaires -------- */
    private static string Ts()
    {
        // 'InvariantCulture' garantit le format, peu importe la locale de la machine
        return DateTime.Now.ToString("yyyyMMddHHmmss", CultureInfo.InvariantCulture);
    }

    private static string Rand(int n)
    {
        string a = "abcdefghijklmnopqrstuvwxyz0123456789";
        var sb = new StringBuilder();
        // Note: 'new Random()' ici est fidèle au code Java,
        // même si un 'Random' statique est souvent préférable en C#.
        var r = new Random();
        for (int i = 0; i < n; i++)
            sb.Append(a[r.Next(a.Length)]);
        return sb.ToString();
    }

    private static string ResolveBaseUrl()
    {
        if (CFG.TryGetValue("SERVER_PORT", out var port) && !string.IsNullOrWhiteSpace(port))
        {
            return $"http://localhost:{port}/api";
        }

        if (CFG.TryGetValue("SERVER_ADDRESS", out var addr) && !string.IsNullOrWhiteSpace(addr))
        {
            if (addr.StartsWith("http", StringComparison.OrdinalIgnoreCase))
            {
                return addr.TrimEnd('/') + "/api";
            }
            return $"http://localhost:{addr}/api";
        }

        return "http://localhost:4100/api";
    }

    private static int ResolvePort(string baseUrl)
    {
        if (Uri.TryCreate(baseUrl, UriKind.Absolute, out var uri) && uri.Port > 0)
        {
            return uri.Port;
        }
        return 4100;
    }

    private static bool WaitForServer(string host, int port, int timeoutMs)
    {
        long startTime = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        while (DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() - startTime < timeoutMs)
        {
            try
            {
                // Utilise TcpClient pour une vérification de connexion avec timeout
                using (var tcpClient = new TcpClient())
                {
                    // L'équivalent C# de 'socket.connect(address, 100)'
                    var result = tcpClient.BeginConnect(host, port, null, null);
                    var success = result.AsyncWaitHandle.WaitOne(TimeSpan.FromMilliseconds(100));
                    if (success)
                    {
                        tcpClient.EndConnect(result);
                        return true;
                    }
                }
            }
            catch (SocketException)
            {
                // Ignore "Connection refused" et réessaie
            }

            try
            {
                Thread.Sleep(100);
            }
            catch (ThreadInterruptedException)
            {
                Thread.CurrentThread.Interrupt();
                return false;
            }
        }
        return false;
    }

    // Les méthodes 'call' deviennent 'async Task' en C#
    private async Task<JObject> Call(string m, string p, int exp, JObject? body, string who)
    {
        var method = new HttpMethod(m);
        var request = new HttpRequestMessage(method, BASE + p);

        if (body != null)
        {
            request.Content = new StringContent(body.ToString(), Encoding.UTF8, "application/json");
        }

        if (cookie.ContainsKey(who))
        {
            request.Headers.Add("Cookie", cookie[who]);
        }

        HttpResponseMessage response = await client.SendAsync(request);
        int code = (int)response.StatusCode;

        // Gestion améliorée des cookies (portage direct de la logique Java)
        if (response.Headers.TryGetValues("Set-Cookie", out var setCookies))
        {
            string currentCookies = cookie.GetValueOrDefault(who, "");
            foreach (string setCookieHeader in setCookies)
            {
                string newCookie = setCookieHeader.Split(';', 2)[0];
                if (string.IsNullOrEmpty(currentCookies))
                {
                    currentCookies = newCookie;
                }
                else
                {
                    currentCookies += "; " + newCookie;
                }
            }
            cookie[who] = currentCookies;
        }

        // Affichage console identique (formatage C# : {m,-7})
        Console.WriteLine($"{(code == exp ? "✅" : "❌")} {m,-7} {p} [{code}]");

        string responseBody = await response.Content.ReadAsStringAsync();

        if (code != exp)
        {
            throw new Exception($"API {m} {p} -> {code} (attendu {exp})\n{responseBody}");
        }

        string contentType = response.Content.Headers.ContentType?.MediaType ?? "";
        string txt = responseBody.Trim();
        if (contentType.StartsWith("application/json") || txt.StartsWith("{"))
        {
            return txt.Length == 0 ? new JObject() : JObject.Parse(txt);
        }
        return new JObject();
    }

    private async Task<JArray> CallArray(string m, string p, int exp, JObject? body, string who)
    {
        // Wrapper pour les réponses qui sont des listes JSON
        var method = new HttpMethod(m);
        var request = new HttpRequestMessage(method, BASE + p);

        if (body != null)
        {
            request.Content = new StringContent(body.ToString(), Encoding.UTF8, "application/json");
        }

        if (cookie.ContainsKey(who))
        {
            request.Headers.Add("Cookie", cookie[who]);
        }

        HttpResponseMessage response = await client.SendAsync(request);
        int code = (int)response.StatusCode;

        // Affichage console identique
        Console.WriteLine($"{(code == exp ? "✅" : "❌")} {m,-7} {p} [{code}]");

        string responseBody = await response.Content.ReadAsStringAsync();

        if (code != exp)
        {
            throw new Exception($"API {m} {p} -> {code} (attendu {exp})\n{responseBody}");
        }

        string contentType = response.Content.Headers.ContentType?.MediaType ?? "";
        if (contentType.StartsWith("application/json"))
        {
            string txt = responseBody.Trim();
            return txt.Length == 0 ? new JArray() : JArray.Parse(txt);
        }
        return new JArray();
    }


    /* -------- Auth -------- */
    private async Task Login(string mail, string pw, string who)
    {
        var j = new JObject { ["email"] = mail, ["password"] = pw };
        await Call("POST", "/auth/login", 201, j, who);
    }

    private async Task Register(string name, string mail, string pw, string who)
    {
        var j = new JObject { ["name"] = name, ["email"] = mail, ["password"] = pw };
        await Call("POST", "/auth/register", 201, j, who);
    }

    /* -------- Assertions -------- */
    // Note: 'assert_eq' renommé 'AssertEq' (convention C#)
    private static void AssertEq(JObject o, string k, object e)
    {
        if (!o.ContainsKey(k))
        {
            Console.WriteLine($"❌   ↳ Clé '{k}' manquante dans l'objet JSON");
            throw new Exception($"Objet vide – clé {k} recherchée");
        }

        JToken a = o[k]!;
        bool ok;

        // Logique de comparaison Java (via 'doubleValue()')
        if ((e is int or long or double or decimal or float) && (a.Type == JTokenType.Integer || a.Type == JTokenType.Float))
        {
            ok = Convert.ToDouble(e, CultureInfo.InvariantCulture) == a.Value<double>();
        }
        // Logique de comparaison Java (via 'e.equals(a)')
        else if (a is JValue aValue)
        {
            // Compare la valeur brute (string, bool, etc.)
            ok = e.Equals(aValue.Value);
        }
        // Gérer les nulls
        else if (e == null && a.Type == JTokenType.Null)
        {
            ok = true;
        }
        else
        {
            ok = JToken.FromObject(e).Equals(a);
        }

        // Recrée l'affichage printf Java à l'identique
        // (Appelle ToString() sur la valeur brute)
        string aPrint = (a.Type == JTokenType.Null) ? "null" : a.Value<object>()?.ToString() ?? "";
        string ePrint = (e == null) ? "null" : e.ToString()!;

        Console.Write($"{(ok ? "✅" : "❌")}   ↳ {k}={aPrint}\n (attendu {ePrint})\n");

        if (!ok)
        {
            throw new Exception($"Assertion échouée pour la clé '{k}'");
        }
    }

    // Note: 'assert_num' renommé 'AssertNum' (convention C#)
    private static void AssertNum(JObject o, string k)
    {
        if (!o.ContainsKey(k) || !(o[k]!.Type == JTokenType.Integer || o[k]!.Type == JTokenType.Float))
        {
            throw new Exception($"Clé {k} n'est pas numérique ou absente");
        }
    }

    /* -------- Modules de Test -------- */
    // Tous les modules de test deviennent 'async Task'

    private async Task TestPlants()
    {
        Console.WriteLine("\n📌 TEST MODULE: PLANTS (admin)");
        var plant_data = new JObject
        {
            ["name"] = "Test Plant",
            ["price"] = 10,
            ["stock"] = 5
        };
        JObject plant = await Call("POST", "/admin/plants", 201, plant_data, "admin");
        AssertNum(plant, "id");
        int id = (int)plant["id"]!;
        JObject get = await Call("GET", $"/plants/{id}", 200, null, "admin");
        AssertEq(get, "name", plant_data["name"]!.Value<string>());
        var price_update = new JObject { ["price"] = 15 };
        await Call("PATCH", $"/admin/plants/{id}", 200, price_update, "admin");
        JObject check = await Call("GET", $"/plants/{id}", 200, null, "admin");
        AssertEq(check, "price", 15);
        Console.WriteLine($"   ↳ name={(string)check["name"]!}");
        await Call("DELETE", $"/admin/plants/{id}", 200, null, "admin");
    }

    private async Task TestUsers()
    {
        Console.WriteLine("\n📌 TEST MODULE: USERS (admin)");
        string email = $"utilisateur_test_{this.timestamp}@example.com";
        var user_data = new JObject
        {
            ["email"] = email,
            ["name"] = "Utilisateur de test",
            ["password"] = "pass123"
        };
        JObject user = await Call("POST", "/users", 201, user_data, "admin");
        int id = (int)user["id"]!;
        var name_update = new JObject { ["name"] = "Tester Update" };
        await Call("PATCH", $"/users/{id}", 200, name_update, "admin");
        JObject get = await Call("GET", $"/users/{id}", 200, null, "admin");
        AssertEq(get, "name", "Tester Update");
        await Call("DELETE", $"/users/{id}", 200, null, "admin");
    }

    private async Task TestOrders()
    {
        Console.WriteLine("\n📌 TEST MODULE: ORDERS & ORDER ITEMS");
        string plantName = $"Plante_de_test_{this.timestamp}";
        var plant_data = new JObject
        {
            ["name"] = plantName,
            ["price"] = 10,
            ["stock"] = 5
        };
        JObject plant = await Call("POST", "/admin/plants", 201, plant_data, "admin");
        AssertNum(plant, "id");
        int pid = (int)plant["id"]!;

        var item = new JObject { ["plantId"] = pid, ["quantity"] = 2 };
        var order_data = new JObject { ["items"] = new JArray { item } };
        JObject order = await Call("POST", "/orders", 201, order_data, "user");
        AssertNum(order, "id");
        int oid = (int)order["id"]!;

        var status_update = new JObject { ["status"] = "shipped" };
        await Call("PATCH", $"/orders/{oid}", 200, status_update, "admin");

        JArray list = await CallArray("GET", "/orders", 200, null, "user");
        JObject? found = null;
        // JArray.Count remplace list.length()
        for (int i = 0; i < list.Count; i++)
        {
            JObject o = (JObject)list[i];
            if ((int)o["id"]! == oid)
            {
                found = o;
                break;
            }
        }
        if (found == null) throw new Exception("Commande absente");

        AssertEq(found, "status", "shipped");
        if (!found.ContainsKey("orderItems") || ((JArray)found["orderItems"]!).Count == 0)
        {
            throw new Exception("Items absents dans la commande");
        }
        JObject nestedPlant = (JObject)((JArray)found["orderItems"]!)[0]["plant"]!;
        AssertEq(nestedPlant, "name", plantName);

        await Call("DELETE", $"/orders/{oid}", 200, null, "admin");
        await Call("DELETE", $"/admin/plants/{pid}", 200, null, "admin");
    }

    private async Task TestUserProfile(string email)
    {
        Console.WriteLine("\n📌 TEST MODULE: USER PROFILE (user)");
        JArray users = await CallArray("GET", "/users", 200, null, "admin");
        JObject? user_obj = null;
        foreach (JObject u in users.Cast<JObject>())
        {
            if ((string)u["email"]! == email)
            {
                user_obj = u;
                break;
            }
        }
        if (user_obj == null) throw new Exception("Utilisateur de test non trouvé");
        int uid = (int)user_obj["id"]!;

        JObject profile = await Call("GET", $"/users/{uid}", 200, null, "user");
        AssertEq(profile, "id", uid);

        string new_name = $"Utilisateur_de_test_{this.timestamp}";
        var name_update = new JObject { ["name"] = new_name };
        await Call("PATCH", $"/users/{uid}", 200, name_update, "user");

        JObject updated = await Call("GET", $"/users/{uid}", 200, null, "user");
        AssertEq(updated, "name", new_name);

        var admin_update = new JObject { ["admin"] = true };
        await Call("PATCH", $"/users/{uid}", 200, admin_update, "user"); // L'API doit ignorer ce champ

        JObject check = await Call("GET", $"/users/{uid}", 200, null, "admin");
        AssertEq(check, "admin", false); // Vérification que l'utilisateur n'est pas devenu admin
    }

    private async Task TestAuthRoles()
    {
        Console.WriteLine("\n📌 TEST MODULE: ROLES");
        var bad_plant = new JObject { ["name"] = "Bad", ["price"] = 1, ["stock"] = 1 };
        await Call("POST", "/admin/plants", 403, bad_plant, "user");

        var good_plant = new JObject { ["name"] = "Good", ["price"] = 1, ["stock"] = 1 };
        JObject plant = await Call("POST", "/admin/plants", 201, good_plant, "admin");
        int pid = (int)plant["id"]!;
        await Call("DELETE", $"/admin/plants/{pid}", 200, null, "admin");

        await Call("GET", "/users", 403, null, "user");
    }

    private async Task TestAdminPlants()
    {
        Console.WriteLine("\n📌 TEST MODULE: ADMIN PLANTS");
        JArray plantes = await CallArray("GET", "/admin/plants", 200, null, "admin");
        Console.WriteLine($"   ↳ {plantes.Count} plantes récupérées");

        var plant_data = new JObject
        {
            ["name"] = $"Plante_admin_{this.timestamp}",
            ["price"] = 99,
            ["stock"] = 12
        };
        JObject p = await Call("POST", "/admin/plants", 201, plant_data, "admin");
        int id = (int)p["id"]!;

        var price_update = new JObject { ["price"] = 123 };
        await Call("PATCH", $"/admin/plants/{id}", 200, price_update, "admin");
        await Call("DELETE", $"/admin/plants/{id}", 200, null, "admin");
    }

    private async Task TestAdminUsers()
    {
        Console.WriteLine("\n📌 TEST MODULE: ADMIN USERS");
        string email = $"admin_temp_{this.timestamp}@example.com";
        string name = $"Admin Temporaire {this.timestamp}";

        var temp_admin_data = new JObject
        {
            ["email"] = email,
            ["name"] = name,
            ["password"] = "password",
            ["admin"] = true
        };
        JObject temp = await Call("POST", "/users", 201, temp_admin_data, "admin");
        int id = (int)temp["id"]!;

        JArray list = await CallArray("GET", "/admin/users", 200, null, "admin");
        JObject? cible = null;
        foreach (JObject u in list.Cast<JObject>())
        {
            if ((string)u["email"]! == email)
            {
                cible = u;
                break;
            }
        }
        if (cible == null) throw new Exception("L'admin temporaire n'a pas été trouvé dans la liste !");
        AssertEq(cible, "name", name);

        string nouveau_nom = $"Admin_temp_modifié_{this.timestamp}";
        var name_update = new JObject { ["name"] = nouveau_nom };
        await Call("PATCH", $"/users/{id}", 200, name_update, "admin");

        JObject user_get = await Call("GET", $"/users/{id}", 200, null, "admin");
        AssertEq(user_get, "name", nouveau_nom);

        await Call("DELETE", $"/users/{id}", 200, null, "admin");
    }

    private async Task TestAuthMe()
    {
        Console.WriteLine("\n📌 TEST MODULE: AUTH /me");
        JObject me = await Call("GET", "/auth/me", 200, null, "user");
        string mail = (string)me["email"]!;
        string nom = (string)me["name"]!;
        AssertEq(me, "email", mail);
        AssertEq(me, "name", nom);
        Console.WriteLine($"   ↳ Utilisateur connecté: {mail} ({nom})");
    }

    /* -------- Main -------- */
    // Le Main devient 'async Task' pour supporter 'await'
    public static async Task Main(string[] args)
    {
        try
        {
            int serverPort = ResolvePort(BASE);
            if (!WaitForServer("127.0.0.1", serverPort, 5000))
            {
                // System.err.println -> Console.Error.WriteLine
                Console.Error.WriteLine($"❌ Serveur http://localhost:{serverPort} injoignable");
                // System.exit(2) -> Environment.Exit(2)
                Environment.Exit(2);
            }

            Test t = new Test();

            string random_tag = Rand(4);
            string userEmail = $"utilisateur_de_test_{t.timestamp}_{random_tag}@example.com";
            string userPassword = "pass123";

            Console.WriteLine($"🧪 Démarrage des tests: {BASE}\n");

            // Connexion des utilisateurs de base pour les tests
            await t.Login(ADMIN_EMAIL, ADMIN_PWD, "admin");
            await t.Register("User", userEmail, userPassword, "user"); // Utilise un nom générique
            await t.Login(userEmail, userPassword, "user");

            // Exécution des suites de tests
            await t.TestPlants();
            await t.TestUsers();
            await t.TestOrders();
            await t.TestUserProfile(userEmail);
            await t.TestAuthRoles();
            await t.TestAdminPlants();
            await t.TestAdminUsers();
            await t.TestAuthMe();

            Console.WriteLine("\n🎉 Tous les tests ont réussi!");
            Environment.Exit(0);

        }
        catch (Exception e)
        {
            Console.Error.WriteLine($"\n❌ Tests interrompus: {e.Message}");
            // e.StackTrace; // Décommenter pour un débogage détaillé
            Environment.Exit(1);
        }
    }
}
