using Npgsql;
using plant_shop_c_sharp.Models;
using System.Net;
using System.Threading.Tasks;
using Newtonsoft.Json;
using System.Collections.Generic;

namespace plant_shop_c_sharp.Controllers
{
    public class OrderController : BaseController
    {
        public OrderController(NpgsqlDataSource dataSource) : base(dataSource) { }

        public override async Task HandleRequest(HttpListenerContext context, User? currentUser)
        {
            var request = context.Request;
            var response = context.Response;
            var path = request.Url?.AbsolutePath ?? "";
            var method = request.HttpMethod;

            if (currentUser == null)
            {
                await SendError(response, 401, "Authentification requise");
                return;
            }

            // Routage: /api/orders
            var segments = path.Split('/', StringSplitOptions.RemoveEmptyEntries);
            int id = -1;
            if (segments.Length == 3 && segments[1] == "orders") // /api/orders/:id
            {
                int.TryParse(segments[2], out id);
            }

            try
            {
                if (method == "GET")
                {
                    if (id != -1)
                        await GetOrder(response, currentUser, id);
                    else
                        await GetUserOrders(response, currentUser);
                }
                else if (method == "POST" && id == -1)
                {
                    await CreateOrder(request, response, currentUser);
                }
                else if (method == "PATCH" && id != -1 && currentUser.IsAdmin)
                {
                    // Seul l'admin peut modifier (ex: status)
                    await UpdateOrderStatus(request, response, id);
                }
                else if (method == "DELETE" && id != -1 && currentUser.IsAdmin)
                {
                    await DeleteOrder(response, id);
                }
                else
                {
                    await SendError(response, 404, "Route non trouvée ou action non autorisée");
                }
            }
            catch (Exception ex)
            {
                await SendError(response, 500, $"Erreur interne: {ex.Message}");
            }
        }

        // GET /api/orders
        private async Task GetUserOrders(HttpListenerResponse response, User user)
        {
            var orders = await OrderRepo.FindByUserIdAsync(user.Id);
            // Enrichir les commandes avec leurs items et les détails des plantes
            foreach (var order in orders)
            {
                order.OrderItems = await OrderItemRepo.FindByOrderIdAsync(order.Id);
                foreach (var item in order.OrderItems)
                {
                    item.Plant = await PlantRepo.FindByIdAsync(item.PlantId);
                }
            }
            await SendJsonResponse(response, 200, orders);
        }

        // GET /api/orders/:id
        private async Task GetOrder(HttpListenerResponse response, User user, int id)
        {
            var order = await OrderRepo.FindByIdAsync(id);
            if (order == null || (order.UserId != user.Id && !user.IsAdmin))
            {
                await SendError(response, 404, "Commande non trouvée ou accès refusé");
                return;
            }

            order.OrderItems = await OrderItemRepo.FindByOrderIdAsync(order.Id);
            foreach (var item in order.OrderItems)
            {
                item.Plant = await PlantRepo.FindByIdAsync(item.PlantId);
            }

            await SendJsonResponse(response, 200, order);
        }

        // POST /api/orders
        private async Task CreateOrder(HttpListenerRequest request, HttpListenerResponse response, User user)
        {
            var body = ParseBody<OrderRequest>(request);
            if (body == null || body.Items == null || body.Items.Count == 0)
            {
                await SendError(response, 400, "Requête invalide, 'items' est requis");
                return;
            }

            decimal totalPrice = 0;
            var orderItems = new List<OrderItem>();

            // Utilisation d'une transaction
            await using var conn = await DataSource.OpenConnectionAsync();
            await using var transaction = await conn.BeginTransactionAsync();

            try
            {
                foreach (var itemDto in body.Items)
                {
                    var plant = await PlantRepo.FindByIdAsync(itemDto.PlantId);
                    if (plant == null || plant.Stock < itemDto.Quantity)
                    {
                        throw new InvalidOperationException($"Stock insuffisant pour {plant?.Name ?? "ID " + itemDto.PlantId}");
                    }

                    // Décrémenter le stock
                    plant.Stock -= itemDto.Quantity;
                    await PlantRepo.UpdateAsync(plant); // Ce repo doit utiliser la connexion/transaction

                    totalPrice += plant.Price * itemDto.Quantity;

                    orderItems.Add(new OrderItem
                    {
                        PlantId = itemDto.PlantId,
                        Quantity = itemDto.Quantity,
                        Price = plant.Price
                    });
                }

                // Créer la commande
                var order = new Order
                {
                    UserId = user.Id,
                    TotalPrice = totalPrice,
                    Status = "confirmed"
                };
                var createdOrder = await OrderRepo.CreateAsync(order);

                // Créer les OrderItems
                foreach (var item in orderItems)
                {
                    item.OrderId = createdOrder.Id;
                    await OrderItemRepo.CreateAsync(item);
                }

                await transaction.CommitAsync();

                // Re-charger la commande complète pour la réponse
                var finalOrder = await OrderRepo.FindByIdAsync(createdOrder.Id);
                finalOrder.OrderItems = orderItems; // Attacher les items créés
                await SendJsonResponse(response, 201, finalOrder);
            }
            catch (Exception ex)
            {
                await transaction.RollbackAsync();
                await SendError(response, 400, $"Échec de la création de la commande: {ex.Message}");
            }
        }

        // PATCH /api/orders/:id (Admin seulement)
        private async Task UpdateOrderStatus(HttpListenerRequest request, HttpListenerResponse response, int id)
        {
            var order = await OrderRepo.FindByIdAsync(id);
            if (order == null)
            {
                await SendError(response, 404, "Commande non trouvée");
                return;
            }

            var body = ParseBody<StatusUpdateRequest>(request);
            if (body == null || string.IsNullOrEmpty(body.Status))
            {
                await SendError(response, 400, "Statut requis");
                return;
            }

            order.Status = body.Status;
            await OrderRepo.UpdateAsync(order);
            await SendJsonResponse(response, 200, order);
        }

        // DELETE /api/orders/:id (Admin seulement)
        private async Task DeleteOrder(HttpListenerResponse response, int id)
        {
             // Supposer que la BDD a un CASCADE DELETE sur order_items
            await OrderRepo.DeleteAsync(id);
            SendEmptyResponse(response, 200);
        }

        // DTOs locaux
        private class OrderItemRequest { public int PlantId { get; set; } public int Quantity { get; set; } }
        private class OrderRequest { public List<OrderItemRequest>? Items { get; set; } }
        private class StatusUpdateRequest { public string? Status { get; set; } }
    }
}
