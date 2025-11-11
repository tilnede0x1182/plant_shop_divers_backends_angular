using Npgsql;
using NpgsqlTypes;
using System.Linq;
using System.Text;
using plant_shop_asp_dapper.Utils; // Accès au PasswordUtil local

namespace Db; // Correspond au 'package db'

/** Seed aligné sur la version C++ : noms réalistes, descriptions, prix cohérents,
    décrémentation du stock, génération users.txt                           */
public sealed class Seed
{
    private sealed class PlantInfo
    {
        public int Id { get; }
        public int Price { get; }
        public int Stock { get; set; }

        public PlantInfo(int id, int price, int stock)
        {
            Id = id;
            Price = price;
            Stock = stock;
        }
    }

    /* ---------- Lecture .env ---------- */
    // Note : C# utilise souvent des variables d'environnement chargées
    // ou des fichiers JSON (appsettings.json). Cette méthode imite
    // directement le parseur manuel simple du code Java.
    private static Dictionary<string, string> Env()
    {
        var outDict = new Dictionary<string, string>();
        string envPath = ".env";

        if (!File.Exists(envPath))
        {
            Console.WriteLine("⚠️ Fichier .env non trouvé.");
            return outDict;
        }

        try
        {
            using (var br = new StreamReader(envPath))
            {
                string? l;
                while ((l = br.ReadLine()) != null)
                {
                    int i = l.IndexOf('=');
                    if (i > 0)
                    {
                        outDict[l.Substring(0, i).Trim()] = l.Substring(i + 1).Trim();
                    }
                }
            }
        }
        catch (IOException e)
        {
            Console.WriteLine($"Erreur lors de la lecture du .env: {e.Message}");
        }
        return outDict;
    }

    /* ---------- Constantes ---------- */
    // En C#, 'const' est pour les constantes de compilation,
    // 'static readonly' est pour les valeurs initialisées à l'exécution (comme les tableaux).
    private const int NB_ADMINS = 3;
    private const int NB_USERS = 20;
    private const int NB_PLANTS = 50;
    private const int MAX_ORDERS_PER_USER = 7;

    private static readonly string[] PLANT_NAMES = {
        "Rose","Tulipe","Lavande","Orchidée","Basilic","Menthe","Pivoine","Tournesol",
        "Cactus (Echinopsis)","Bambou","Camomille (Matricaria recutita)","Sauge (Salvia officinalis)",
        "Romarin (Rosmarinus officinalis)","Thym (Thymus vulgaris)","Laurier-rose (Nerium oleander)",
        "Aloe vera","Jasmin (Jasminum officinale)","Hortensia (Hydrangea macrophylla)",
        "Marguerite (Leucanthemum vulgare)","Géranium (Pelargonium graveolens)",
        "Fuchsia (Fuchsia magellanica)","Anémone (Anemone coronaria)","Azalée (Rhododendron simsii)",
        "Chrysanthème (Chrysanthemum morifolium)","Digitale pourpre (Digitalis purpurea)",
        "Glaïeul (Gladiolus hortulanus)","Lys (Lilium candidum)","Violette (Viola odorata)",
        "Muguet (Convallaria majalis)","Iris (Iris germanica)","Lavandin (Lavandula intermedia)",
        "Érable du Japon (Acer palmatum)","Citronnelle (Cymbopogon citratus)","Pin parasol (Pinus pinea)",
        "Cyprès (Cupressus sempervirens)","Olivier (Olea europaea)","Papyrus (Cyperus papyrus)",
        "Figuier (Ficus carica)","Eucalyptus (Eucalyptus globulus)","Acacia (Acacia dealbata)",
        "Bégonia (Begonia semperflorens)","Calathea (Calathea ornata)","Dieffenbachia (Dieffenbachia seguine)",
        "Ficus elastica","Sansevieria (Sansevieria trifasciata)","Philodendron (Philodendron scandens)",
        "Yucca (Yucca elephantipes)","Zamioculcas zamiifolia","Monstera deliciosa",
        "Pothos (Epipremnum aureum)","Agave (Agave americana)","Cactus raquette (Opuntia ficus-indica)"
    };

    private static readonly string[] FIRST = {
        "Alice","Bruno","Cathy","David","Emma","Franck",
        "Gwen","Hugo","Inès","Jules","Katia","Léo"
    };
    private static readonly string[] LAST = {
        "Dupont","Martin","Bernard","Petit","Robert","Richard","Durand","Moreau","Roux","Fournier"
    };
    private static readonly string[] EMAIL_DOMAINS = { "gmail.com", "yahoo.com", "hotmail.com" };

    // 'static readonly' en C# est crucial pour Random afin d'éviter
    // des problèmes de génération de nombres identiques.
    private static readonly Random RNG = new Random();

    /* ---------- Helpers ---------- */
    // Note : Le .Next(min, max) de C# a une borne supérieure *exclusive*,
    // d'où le 'max + 1'.
    private static int Rnd(int min, int max) { return min + RNG.Next(max - min + 1); }
    private static T Pick<T>(T[] arr) { return arr[Rnd(0, arr.Length - 1)]; }
    private static string RandPwd() { return "pw" + Rnd(100000000, 999999999); }
    private static string Hash(string p) { return PasswordUtil.HashPassword(p); }

    private static string LoremSentence()
    {
        string[] words = {"lorem","ipsum","dolor","sit","amet","consectetur","adipiscing","elit",
            "sed","do","eiusmod","tempor","incididunt","ut","labore","et","dolore","magna","aliqua"};
        int n = Rnd(10, 14);
        var sb = new StringBuilder();
        for (int i = 0; i < n; i++)
        {
            string w = words[Rnd(0, words.Length - 1)];
            // char.ToUpper(w[0]) + w.Substring(1) est l'équivalent C#
            sb.Append(i == 0 ? char.ToUpper(w[0]) + w.Substring(1) : w);
            sb.Append(i == n - 1 ? '.' : ' ');
        }
        return sb.ToString();
    }

    /* ---------- Main ---------- */
    public static void Main(string[] args)
    {
        var cfg = Env();
        if (!cfg.ContainsKey("DATABASE_URL") || !cfg.ContainsKey("DATABASE_USER") || !cfg.ContainsKey("DATABASE_PASS"))
        {
            Console.WriteLine("Erreur: DATABASE_URL, DATABASE_USER, ou DATABASE_PASS manquant dans .env");
            return;
        }

        string connString = BuildConnectionString(
            cfg["DATABASE_URL"],
            cfg["DATABASE_USER"],
            cfg["DATABASE_PASS"]
        );

        // 'using var' gère automatiquement la fermeture de la connexion (db.Close())
        using var db = new NpgsqlConnection(connString);
        db.Open();

        /* Nettoyage */
        Console.WriteLine("🧹 Nettoyage de la base de données…");
        // 'using' gère le 'Statement' (NpgsqlCommand en C#)
        using (var st = db.CreateCommand())
        {
            st.CommandText = "TRUNCATE order_items,orders,plants,users RESTART IDENTITY CASCADE";
            st.ExecuteNonQuery(); // Équivalent de st.execute()
        }
        Console.WriteLine("✅ Base vidée.");

        /* ---------- Users ---------- */
        // En ADO.NET (Npgsql), nous utilisons des commandes paramétrées (avec @)
        // et 'RETURNING id' pour récupérer les clés générées.
        using var insUser = db.CreateCommand();
        insUser.CommandText = "INSERT INTO users(name,email,password_hash,is_admin) VALUES (@name,@email,@hash,@is_admin) RETURNING id";

        // Définir les types de paramètres une seule fois
        insUser.Parameters.Add("@name", NpgsqlDbType.Varchar);
        insUser.Parameters.Add("@email", NpgsqlDbType.Varchar);
        insUser.Parameters.Add("@hash", NpgsqlDbType.Varchar);
        insUser.Parameters.Add("@is_admin", NpgsqlDbType.Boolean);

        var adminIds = new List<int>();
        var userIds = new List<int>();
        var credsOut = new List<string>();
        credsOut.Add("Administrateurs :\n");

        // Admins
        Console.WriteLine("👑 Création des administrateurs…");
        for (int i = 0; i < NB_ADMINS; i++)
        {
            string name = Pick(FIRST) + " " + Pick(LAST);
            string email = "admin" + (i + 1) + "@planteshop.com";
            string pwd = "password";

            // Assigner les valeurs
            insUser.Parameters["@name"].Value = name;
            insUser.Parameters["@email"].Value = email;
            insUser.Parameters["@hash"].Value = Hash(pwd);
            insUser.Parameters["@is_admin"].Value = true;

            // ExecuteScalar() récupère la première colonne de la première ligne (notre 'id')
            int id = (int)(insUser.ExecuteScalar() ?? 0);
            adminIds.Add(id);
            credsOut.Add(email + " " + pwd);
        }
        Console.WriteLine("✅ " + adminIds.Count + " admins.");

        credsOut.Add("");
        credsOut.Add("Utilisateurs :\n");

        // Users
        Console.WriteLine("👥 Création des utilisateurs…");
        for (int i = 0; i < NB_USERS; i++)
        {
            string first = Pick(FIRST), last = Pick(LAST);
            string email = first.ToLower() + "_" + last.ToLower() + Rnd(20, 99) + "@" + Pick(EMAIL_DOMAINS);
            string pwd = RandPwd();
            string name = first + " " + last;

            insUser.Parameters["@name"].Value = name;
            insUser.Parameters["@email"].Value = email;
            insUser.Parameters["@hash"].Value = Hash(pwd);
            insUser.Parameters["@is_admin"].Value = false;

            int id = (int)(insUser.ExecuteScalar() ?? 0);
            userIds.Add(id);
            credsOut.Add(email + " " + pwd);
        }
        Console.WriteLine("✅ " + userIds.Count + " utilisateurs.");

        /* ---------- Plants ---------- */
        using var insPlant = db.CreateCommand();
        insPlant.CommandText = "INSERT INTO plants(name,description,price,stock) VALUES (@name,@desc,@price,@stock) RETURNING id";
        insPlant.Parameters.Add("@name", NpgsqlDbType.Varchar);
        insPlant.Parameters.Add("@desc", NpgsqlDbType.Text); // Description
        insPlant.Parameters.Add("@price", NpgsqlDbType.Numeric); // Équivalent de BigDecimal
        insPlant.Parameters.Add("@stock", NpgsqlDbType.Integer);


        Console.WriteLine("🌱 Création des plantes…");
        var plants = new List<PlantInfo>();

        for (int i = 0; i < NB_PLANTS; i++)
        {
            string baseName = PLANT_NAMES[i % PLANT_NAMES.Length]; // 'base' est un mot-clé C#, renommé 'baseName'
            string name = NB_PLANTS > PLANT_NAMES.Length ? baseName + " " + (i / PLANT_NAMES.Length + 1) : baseName;
            int price = Rnd(5, 50);
            int stock = Rnd(5, 30);

            insPlant.Parameters["@name"].Value = name;
            insPlant.Parameters["@desc"].Value = LoremSentence();
            insPlant.Parameters["@price"].Value = (decimal)price; // Convertir l'int en 'decimal' pour la BDD
            insPlant.Parameters["@stock"].Value = stock;

            int id = (int)(insPlant.ExecuteScalar() ?? 0);
            plants.Add(new PlantInfo(id, price, stock));
        }
        Console.WriteLine("✅ " + plants.Count + " plantes.");

        /* ---------- Orders & items ---------- */
        using var insOrder = db.CreateCommand();
        insOrder.CommandText = "INSERT INTO orders(user_id,total,status) VALUES (@user_id,@total,@status) RETURNING id";
        insOrder.Parameters.Add("@user_id", NpgsqlDbType.Integer);
        insOrder.Parameters.Add("@total", NpgsqlDbType.Numeric);
        insOrder.Parameters.Add("@status", NpgsqlDbType.Varchar);

        using var insItem = db.CreateCommand();
        insItem.CommandText = "INSERT INTO order_items(order_id,plant_id,quantity,price) VALUES (@order_id,@plant_id,@qty,@price)";
        insItem.Parameters.Add("@order_id", NpgsqlDbType.Integer);
        insItem.Parameters.Add("@plant_id", NpgsqlDbType.Integer);
        insItem.Parameters.Add("@qty", NpgsqlDbType.Integer);
        insItem.Parameters.Add("@price", NpgsqlDbType.Numeric);

        using var updPlantStock = db.CreateCommand();
        updPlantStock.CommandText = "UPDATE plants SET stock = stock - @qty WHERE id = @id";
        updPlantStock.Parameters.Add("@qty", NpgsqlDbType.Integer);
        updPlantStock.Parameters.Add("@id", NpgsqlDbType.Integer);

        string[] statusArr = { "confirmed", "pending", "shipped", "delivered" };
        int totalOrders = 0;

        Console.WriteLine("🛒 Création des commandes…");
        foreach (int uid in userIds)
        {
            int nb = Rnd(0, MAX_ORDERS_PER_USER);
            for (int k = 0; k < nb; k++)
            {
                insOrder.Parameters["@user_id"].Value = uid;
                insOrder.Parameters["@total"].Value = decimal.Zero; // placeholder
                insOrder.Parameters["@status"].Value = statusArr[Rnd(0, 3)];

                int orderId = (int)(insOrder.ExecuteScalar() ?? 0);

                decimal total = decimal.Zero; // 'decimal' est l'équivalent C# de BigDecimal
                for (int it = 0; it < 2; it++) // Logique Java conservée (2 items max)
                {
                    // Utilisation de LINQ (Where().ToList()) en C#
                    var avail = plants.Where(p => p.Stock > 0).ToList();
                    if (avail.Count == 0) break; // .isEmpty() -> .Count == 0

                    PlantInfo p = avail[Rnd(0, avail.Count - 1)];
                    int qty = Math.Min(Rnd(1, 5), p.Stock);

                    insItem.Parameters["@order_id"].Value = orderId;
                    insItem.Parameters["@plant_id"].Value = p.Id;
                    insItem.Parameters["@qty"].Value = qty;
                    insItem.Parameters["@price"].Value = (decimal)p.Price; // cast
                    insItem.ExecuteNonQuery();

                    // stock --
                    p.Stock -= qty;
                    updPlantStock.Parameters["@qty"].Value = qty;
                    updPlantStock.Parameters["@id"].Value = p.Id;
                    updPlantStock.ExecuteNonQuery();

                    // L'opérateur '+=' fonctionne avec 'decimal'
                    total += (p.Price * qty);
                }

                // Mise à jour du total de la commande
                using (var up = db.CreateCommand())
                {
                    up.CommandText = "UPDATE orders SET total=@total WHERE id=@id";
                    // AddWithValue est pratique pour les commandes rapides
                    up.Parameters.AddWithValue("@total", total);
                    up.Parameters.AddWithValue("@id", orderId);
                    up.ExecuteNonQuery();
                }
                totalOrders++;
            }
        }
        Console.WriteLine("✅ " + totalOrders + " commandes.");

        /* ---------- users.txt ---------- */
        try
        {
            // File.WriteAllLines est l'équivalent C# simple de PrintWriter
            File.WriteAllLines("users.txt", credsOut);
            Console.WriteLine("✍️ Fichier users.txt généré (" + credsOut.Count + " lignes).");
        }
        catch (IOException e)
        {
            Console.WriteLine($"Erreur lors de l'écriture de users.txt: {e.Message}");
        }

        // db.Close() est géré automatiquement par 'using var db'
        Console.WriteLine("🎉 Seed terminée !");
    }

    private static string BuildConnectionString(string rawUrl, string user, string pass)
    {
        if (string.IsNullOrWhiteSpace(rawUrl))
        {
            throw new ArgumentException("DATABASE_URL manquant ou vide");
        }

        string normalized = rawUrl.StartsWith("jdbc:", StringComparison.OrdinalIgnoreCase)
            ? rawUrl.Substring("jdbc:".Length)
            : rawUrl;

        if (normalized.StartsWith("postgresql://", StringComparison.OrdinalIgnoreCase))
        {
            var uri = new Uri(normalized);
            var builder = new NpgsqlConnectionStringBuilder
            {
                Host = uri.Host,
                Port = uri.IsDefaultPort ? 5432 : uri.Port,
                Database = uri.AbsolutePath.Trim('/')
            };

            if (!string.IsNullOrEmpty(uri.UserInfo))
            {
                var parts = uri.UserInfo.Split(':');
                builder.Username = parts.Length > 0 && !string.IsNullOrEmpty(parts[0]) ? parts[0] : user;
                builder.Password = parts.Length > 1 ? parts[1] : pass;
            }
            else
            {
                builder.Username = user;
                builder.Password = pass;
            }

            return builder.ConnectionString;
        }
        else
        {
            var builder = new NpgsqlConnectionStringBuilder(normalized)
            {
                Username = user,
                Password = pass
            };
            return builder.ConnectionString;
        }
    }
}
