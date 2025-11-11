using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Threading.Tasks;
using Npgsql;
using plant_shop_c_sharp;

class Program
{
    private static NpgsqlDataSource? _dataSource;

    static async Task Main(string[] args)
    {
        var cfg = LoadEnv();

        string rawDbUrl = Environment.GetEnvironmentVariable("DATABASE_URL")
                           ?? cfg.GetValueOrDefault("DATABASE_URL");
        string dbUser = Environment.GetEnvironmentVariable("DATABASE_USER")
                        ?? cfg.GetValueOrDefault("DATABASE_USER", string.Empty);
        string dbPass = Environment.GetEnvironmentVariable("DATABASE_PASS")
                        ?? cfg.GetValueOrDefault("DATABASE_PASS", string.Empty);

        string connString = BuildConnectionString(rawDbUrl, dbUser, dbPass);

        string portValue = Environment.GetEnvironmentVariable("SERVER_ADDRESS")
                            ?? cfg.GetValueOrDefault("SERVER_ADDRESS", "4100");
        if (!int.TryParse(portValue, out int port))
        {
            port = 4100;
        }
        string prefix = $"http://localhost:{port}/api/";

        if (string.IsNullOrEmpty(Environment.GetEnvironmentVariable("JWT_SECRET")
            ?? cfg.GetValueOrDefault("JWT_SECRET")))
        {
            Console.WriteLine("⚠️  JWT_SECRET n'est pas définie. Utilisation d'une clé par défaut.");
        }

        try
        {
            // 2. Initialiser le pool de connexions Npgsql
            _dataSource = NpgsqlDataSource.Create(connString);
            Console.WriteLine("Connexion à la base de données PostgreSQL établie.");

            // 3. Initialiser le routeur
            var router = new Routes(_dataSource);

            // 4. Configurer et démarrer le serveur HttpListener
            using (var listener = new HttpListener())
            {
                listener.Prefixes.Add(prefix);
                listener.Start();
                Console.WriteLine($"🚀 Serveur démarré sur http://localhost:{port}");
                Console.WriteLine($"   Routes API disponibles sur http://localhost:{port}/api");

                // Boucle de traitement des requêtes
                while (true)
                {
                    var context = await listener.GetContextAsync();
                    // Ne pas bloquer la boucle, traiter chaque requête dans un thread de pool
                    _ = Task.Run(async () => await router.Handle(context));
                }
            }
        }
        catch (HttpListenerException ex)
        {
            Console.Error.WriteLine($"❌ Erreur : le port {port} est indisponible ({ex.Message}).");
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine($"❌ Erreur lors du démarrage du serveur : {ex.Message}");
        }
        finally
        {
            // 5. Fermer le pool de connexions lors de l'arrêt
            if (_dataSource != null)
            {
                await _dataSource.DisposeAsync();
                Console.WriteLine("Connexion à la base de données fermée.");
            }
        }
    }

    private static Dictionary<string, string> LoadEnv()
    {
        var map = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        string? path = ResolveEnvPath();
        if (path == null)
        {
            Console.WriteLine("⚠️  Fichier .env non trouvé. Utilisation des valeurs par défaut.");
            return map;
        }

        try
        {
            foreach (var line in File.ReadAllLines(path))
            {
                int idx = line.IndexOf('=');
                if (idx > 0)
                {
                    string key = line.Substring(0, idx).Trim();
                    string value = line.Substring(idx + 1).Trim();
                    map[key] = value;
                }
            }
        }
        catch (IOException e)
        {
            Console.WriteLine($"⚠️  Lecture .env impossible: {e.Message}");
        }
        return map;
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

    private static string BuildConnectionString(string? rawUrl, string user, string pass)
    {
        string fallback = "Host=localhost;Database=plant_shop_c-sharp";
        string normalized = string.IsNullOrWhiteSpace(rawUrl) ? fallback : rawUrl;
        if (normalized.StartsWith("jdbc:", StringComparison.OrdinalIgnoreCase))
        {
            normalized = normalized.Substring("jdbc:".Length);
        }

        if (normalized.StartsWith("postgresql://", StringComparison.OrdinalIgnoreCase))
        {
            var uri = new Uri(normalized);
            var builder = new NpgsqlConnectionStringBuilder
            {
                Host = uri.Host,
                Port = uri.IsDefaultPort ? 5432 : uri.Port,
                Database = string.IsNullOrWhiteSpace(uri.AbsolutePath.Trim('/')) ? "plant_shop_c-sharp" : uri.AbsolutePath.Trim('/')
            };

            if (!string.IsNullOrEmpty(uri.UserInfo))
            {
                var parts = uri.UserInfo.Split(':', 2);
                builder.Username = string.IsNullOrEmpty(parts[0]) ? user : parts[0];
                builder.Password = parts.Length > 1 ? parts[1] : pass;
            }
            else
            {
                if (!string.IsNullOrEmpty(user)) builder.Username = user;
                if (!string.IsNullOrEmpty(pass)) builder.Password = pass;
            }

            return builder.ConnectionString;
        }
        else
        {
            var builder = new NpgsqlConnectionStringBuilder(normalized);
            if (!string.IsNullOrEmpty(user)) builder.Username = user;
            if (!string.IsNullOrEmpty(pass)) builder.Password = pass;
            if (string.IsNullOrEmpty(builder.Database)) builder.Database = "plant_shop_c-sharp";
            return builder.ConnectionString;
        }
    }
}
