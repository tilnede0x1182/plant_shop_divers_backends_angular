using System.Net;
using Npgsql;
using plant_shop_c_sharp.Controllers;
using plant_shop_c_sharp.Models;

namespace plant_shop_c_sharp
{
    // Le routeur principal qui délègue aux contrôleurs
    public class Routes
    {
        private readonly AuthController _authController;
        private readonly PlantController _plantController;
        private readonly UserController _userController;
        private readonly OrderController _orderController;

        // Map pour les contrôleurs
        private readonly Dictionary<string, BaseController> _controllerMap;

        public Routes(NpgsqlDataSource dataSource)
        {
            _authController = new AuthController(dataSource);
            _plantController = new PlantController(dataSource);
            _userController = new UserController(dataSource);
            _orderController = new OrderController(dataSource);

            // Mappage des préfixes de route aux contrôleurs
            _controllerMap = new Dictionary<string, BaseController>
            {
                { "/api/auth", _authController },
                { "/api/plants", _plantController },
                { "/api/admin/plants", _plantController },
                { "/api/users", _userController },
                { "/api/admin/users", _userController },
                { "/api/orders", _orderController },
                // Note : /api/admin/orders est géré par OrderController (vérification admin interne)
            };
        }

        public async Task Handle(HttpListenerContext context)
        {
            var request = context.Request;
            var response = context.Response;
            var path = request.Url?.AbsolutePath ?? "/";

            // CORS Preflight
            if (request.HttpMethod == "OPTIONS")
            {
                response.AddHeader("Access-Control-Allow-Origin", request.Headers["Origin"] ?? "*");
                response.AddHeader("Access-Control-Allow-Methods", "GET, POST, PATCH, DELETE, OPTIONS");
                response.AddHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
                response.AddHeader("Access-Control-Allow-Credentials", "true");
                ResponseUtil.SendEmpty(response, 204);
                return;
            }

            // Ajout des headers CORS
            response.AddHeader("Access-Control-Allow-Origin", request.Headers["Origin"] ?? "*");
            response.AddHeader("Access-Control-Allow-Credentials", "true");

            try
            {
                // Récupérer l'utilisateur (peut être null si non authentifié)
                User? currentUser = await _userController.GetCurrentUser(request);

                // Trouver le bon contrôleur
                var controllerKey = _controllerMap.Keys.FirstOrDefault(p => path.StartsWith(p));

                if (controllerKey != null)
                {
                    await _controllerMap[controllerKey].HandleRequest(context, currentUser);
                }
                else
                {
                    await ResponseUtil.SendError(response, 404, "Route API non trouvée");
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Erreur: {ex.Message}\n{ex.StackTrace}");
                await ResponseUtil.SendError(response, 500, "Erreur interne du serveur");
            }
        }
    }
}
