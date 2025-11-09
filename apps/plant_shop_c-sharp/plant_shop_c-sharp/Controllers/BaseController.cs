using Npgsql;
using plant_shop_c_sharp.Repositories;
using System.Net;
using plant_shop_c_sharp.Models;
using plant_shop_c_sharp.Utils;

namespace plant_shop_c_sharp.Controllers
{
    public abstract class BaseController
    {
        protected readonly NpgsqlDataSource DataSource;
        protected readonly UserRepository UserRepo;
        protected readonly PlantRepository PlantRepo;
        protected readonly OrderRepository OrderRepo;
        protected readonly OrderItemRepository OrderItemRepo;

        protected BaseController(NpgsqlDataSource dataSource)
        {
            DataSource = dataSource;
            UserRepo = new UserRepository(dataSource);
            PlantRepo = new PlantRepository(dataSource);
            OrderRepo = new OrderRepository(dataSource);
            OrderItemRepo = new OrderItemRepository(dataSource);
        }

        // Méthode abstraite pour la gestion des routes
        public abstract Task HandleRequest(HttpListenerContext context, User? currentUser);

        // Helper pour récupérer l'utilisateur
        protected async Task<User?> GetCurrentUser(HttpListenerRequest request)
        {
            return await RequestUtil.GetUserFromAuth(request, UserRepo);
        }

        // Helpers de réponse
        protected Task SendJsonResponse(HttpListenerResponse response, int code, object payload)
        {
            return ResponseUtil.SendJson(response, code, payload);
        }

        protected void SendEmptyResponse(HttpListenerResponse response, int code)
        {
            ResponseUtil.SendEmpty(response, code);
        }

        protected Task SendError(HttpListenerResponse response, int code, string message)
        {
            return ResponseUtil.SendError(response, code, message);
        }

        // Helper pour parser le body
        protected T? ParseBody<T>(HttpListenerRequest request) where T : class
        {
            return RequestUtil.ParseJsonBody<T>(request);
        }
    }
}
