using Npgsql;
using plant_shop_c_sharp.Repositories;
using System.Net;
using plant_shop_c_sharp.Models;
using plant_shop_c_sharp.Utils;

namespace plant_shop_c_sharp.Controllers
{
    /// <summary>
    /// Controleur de base avec repositories et helpers.
    /// </summary>
    public abstract class BaseController
    {
        protected readonly NpgsqlDataSource DataSource;
        protected readonly UserRepository UserRepo;
        protected readonly PlantRepository PlantRepo;
        protected readonly OrderRepository OrderRepo;
        protected readonly OrderItemRepository OrderItemRepo;

        /// <summary>
        /// Constructeur avec injection de la source de donnees.
        /// </summary>
        /// <param name="dataSource">Source Npgsql.</param>
        protected BaseController(NpgsqlDataSource dataSource)
        {
            DataSource = dataSource;
            UserRepo = new UserRepository(dataSource);
            PlantRepo = new PlantRepository(dataSource);
            OrderRepo = new OrderRepository(dataSource);
            OrderItemRepo = new OrderItemRepository(dataSource);
        }

        /// <summary>
        /// Methode abstraite pour la gestion des routes.
        /// </summary>
        /// <param name="context">Contexte HTTP.</param>
        /// <param name="currentUser">Utilisateur connecte ou null.</param>
        public abstract Task HandleRequest(HttpListenerContext context, User? currentUser);

        /// <summary>
        /// Recupere l utilisateur depuis le token JWT.
        /// </summary>
        /// <param name="request">Requete HTTP.</param>
        /// <returns>Utilisateur ou null.</returns>
        public async Task<User?> GetCurrentUser(HttpListenerRequest request)
        {
            return await RequestUtil.GetUserFromAuth(request, UserRepo);
        }

        /// <summary>
        /// Envoie une reponse JSON.
        /// </summary>
        /// <param name="response">Reponse HTTP.</param>
        /// <param name="code">Code HTTP.</param>
        /// <param name="payload">Objet a serialiser.</param>
        protected Task SendJsonResponse(HttpListenerResponse response, int code, object payload)
        {
            return ResponseUtil.SendJson(response, code, payload);
        }

        /// <summary>
        /// Envoie une reponse vide.
        /// </summary>
        /// <param name="response">Reponse HTTP.</param>
        /// <param name="code">Code HTTP.</param>
        protected void SendEmptyResponse(HttpListenerResponse response, int code)
        {
            ResponseUtil.SendEmpty(response, code);
        }

        /// <summary>
        /// Envoie une reponse d erreur.
        /// </summary>
        /// <param name="response">Reponse HTTP.</param>
        /// <param name="code">Code HTTP.</param>
        /// <param name="message">Message d erreur.</param>
        protected Task SendError(HttpListenerResponse response, int code, string message)
        {
            return ResponseUtil.SendError(response, code, message);
        }

        /// <summary>
        /// Parse le body JSON de la requete.
        /// </summary>
        /// <typeparam name="T">Type de destination.</typeparam>
        /// <param name="request">Requete HTTP.</param>
        /// <returns>Objet deserialise ou null.</returns>
        protected T? ParseBody<T>(HttpListenerRequest request) where T : class
        {
            return RequestUtil.ParseJsonBody<T>(request);
        }
    }
}
