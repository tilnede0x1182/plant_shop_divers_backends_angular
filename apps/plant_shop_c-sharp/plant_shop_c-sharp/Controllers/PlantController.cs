using Npgsql;
using plant_shop_c_sharp.Models;
using System.Net;
using Newtonsoft.Json;

namespace plant_shop_c_sharp.Controllers
{
    public class PlantController : BaseController
    {
        public PlantController(NpgsqlDataSource dataSource) : base(dataSource) { }

        public override async Task HandleRequest(HttpListenerContext context, User? currentUser)
        {
            var request = context.Request;
            var response = context.Response;
            var path = request.Url?.AbsolutePath ?? "";
            var method = request.HttpMethod;

            // Routage: /api/plants et /api/admin/plants
            var segments = path.Split('/', StringSplitOptions.RemoveEmptyEntries);
            bool isAdminRoute = segments.Length > 1 && segments[1] == "admin";

            int id = -1;
            if (segments.Length == 3 && segments[1] == "plants") // GET /api/plants/1
            {
                int.TryParse(segments[2], out id);
            }
            else if (segments.Length == 4 && isAdminRoute) // GET, PATCH, DELETE /api/admin/plants/1
            {
                int.TryParse(segments[3], out id);
            }

            try
            {
                if (method == "GET")
                {
                    if (id != -1)
                        await GetPlant(response, id);
                    else
                        await GetAllPlants(response);
                }
                else if (isAdminRoute && currentUser?.IsAdmin == true)
                {
                    if (method == "POST" && id == -1)
                        await CreatePlant(request, response);
                    else if (method == "PATCH" && id != -1)
                        await UpdatePlant(request, response, id);
                    else if (method == "DELETE" && id != -1)
                        await DeletePlant(response, id);
                    else
                        await SendError(response, 404, "Route admin non trouvée");
                }
                else if (isAdminRoute)
                {
                    await SendError(response, 403, "Accès admin requis");
                }
                else
                {
                    await SendError(response, 405, "Méthode non autorisée");
                }
            }
            catch (Exception ex)
            {
                await SendError(response, 500, $"Erreur interne: {ex.Message}");
            }
        }

        // GET /api/plants
        private async Task GetAllPlants(HttpListenerResponse response)
        {
            var plants = await PlantRepo.FindAllAsync();
            await SendJsonResponse(response, 200, plants);
        }

        // GET /api/plants/:id
        private async Task GetPlant(HttpListenerResponse response, int id)
        {
            var plant = await PlantRepo.FindByIdAsync(id);
            if (plant == null)
            {
                await SendError(response, 404, "Plante non trouvée");
                return;
            }
            await SendJsonResponse(response, 200, plant);
        }

        // POST /api/admin/plants
        private async Task CreatePlant(HttpListenerRequest request, HttpListenerResponse response)
        {
            var body = ParseBody<PlantRequest>(request);
            if (body == null || string.IsNullOrEmpty(body.Name) || body.Price <= 0 || body.Stock < 0)
            {
                await SendError(response, 400, "Données invalides (Name, Price, Stock requis)");
                return;
            }
            var plant = new Plant
            {
                Name = body.Name,
                Description = body.Description,
                Price = body.Price,
                Stock = body.Stock
            };
            var createdPlant = await PlantRepo.CreateAsync(plant);
            await SendJsonResponse(response, 201, createdPlant);
        }

        // PATCH /api/admin/plants/:id
        private async Task UpdatePlant(HttpListenerRequest request, HttpListenerResponse response, int id)
        {
            var plant = await PlantRepo.FindByIdAsync(id);
            if (plant == null)
            {
                await SendError(response, 404, "Plante non trouvée");
                return;
            }

            var body = ParseBody<PlantRequest>(request);
            if (body == null)
            {
                await SendError(response, 400, "Corps de requête invalide");
                return;
            }

            // Mise à jour partielle
            plant.Name = body.Name ?? plant.Name;
            plant.Description = body.Description ?? plant.Description;
            plant.Price = body.Price > 0 ? body.Price : plant.Price;
            plant.Stock = body.Stock >= 0 ? body.Stock : plant.Stock;

            await PlantRepo.UpdateAsync(plant);
            await SendJsonResponse(response, 200, plant);
        }

        // DELETE /api/admin/plants/:id
        private async Task DeletePlant(HttpListenerResponse response, int id)
        {
            var plant = await PlantRepo.FindByIdAsync(id);
            if (plant == null)
            {
                await SendError(response, 404, "Plante non trouvée");
                return;
            }
            await PlantRepo.DeleteAsync(id);
            SendEmptyResponse(response, 200); // 200 OK (comme dans le test Java)
        }

        // DTO pour le body
        private class PlantRequest
        {
            public string? Name { get; set; }
            public string? Description { get; set; }
            public decimal Price { get; set; } = -1; // Utiliser -1 pour détecter l'absence
            public int Stock { get; set; } = -1;
        }
    }
}
