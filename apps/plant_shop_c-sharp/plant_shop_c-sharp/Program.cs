using System;
using System.Net;
using System.Threading.Tasks;
using Npgsql;
using plant_shop_c_sharp;

class Program
{
    private static NpgsqlDataSource? _dataSource;

    static async Task Main(string[] args)
    {
        // 1. Charger la configuration (similaire au .env)
        // Pour C#, on utilise les variables d'environnement.
        // Assurez-vous de définir : DATABASE_URL, JWT_SECRET
        var dbUrl = Environment.GetEnvironmentVariable("DATABASE_URL")
                    ?? "Host=localhost;Username=votre_user;Password=votre_pass;Database=plant_shop_java";

        var port = Environment.GetEnvironmentVariable("SERVER_ADDRESS") ?? "4100";
        var prefix = $"http://localhost:{port}/api/";

        if (Environment.GetEnvironmentVariable("JWT_SECRET") == null)
        {
            Console.WriteLine("ATTENTION: JWT_SECRET n'est pas définie. Utilisation d'une clé par défaut.");
        }

        try
        {
            // 2. Initialiser le pool de connexions Npgsql
            _dataSource = NpgsqlDataSource.Create(dbUrl);
            Console.WriteLine("Connexion à la base de données PostgreSQL établie.");

            // 3. Initialiser le routeur
            var router = new Routes(_dataSource);

            // 4. Configurer et démarrer le serveur HttpListener
            using (var listener = new HttpListener())
            {
                listener.Prefixes.Add(prefix);
                listener.Start();
                Console.WriteLine($"Serveur C# basique démarré sur {prefix}");

                // Boucle de traitement des requêtes
                while (true)
                {
                    var context = await listener.GetContextAsync();
                    // Ne pas bloquer la boucle, traiter chaque requête dans un thread de pool
                    _ = Task.Run(async () => await router.Handle(context));
                }
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Erreur fatale: {ex.Message}");
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
}
