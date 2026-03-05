using Npgsql;
using plant_shop_c_sharp.DTOs;
using plant_shop_c_sharp.Models;
using System.Net;
using System.Threading.Tasks;
using Newtonsoft.Json;
using System.Collections.Generic;

namespace plant_shop_c_sharp.Controllers
{
    /// <summary>
    /// Controleur CRUD pour les commandes.
    /// </summary>
    public class OrderController : BaseController
    {
        /// <summary>
        /// Constructeur avec injection de la source de donnees.
        /// </summary>
        /// <param name="dataSource">Source Npgsql.</param>
        public OrderController(NpgsqlDataSource dataSource) : base(dataSource) { }

        /// <summary>
        /// Gere les requetes HTTP pour les routes orders.
        /// </summary>
        /// <param name="context">Contexte HTTP.</param>
        /// <param name="currentUser">Utilisateur connecte ou null.</param>
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

        /// <summary>
        /// Liste les commandes de l utilisateur connecte.
        /// </summary>
        /// <param name="response">Reponse HTTP.</param>
        /// <param name="user">Utilisateur connecte.</param>
        private async Task GetUserOrders(HttpListenerResponse response, User user)
        {
            var orders = await OrderRepo.FindByUserIdAsync(user.Id);
            // Enrichir les commandes avec leurs items et les détails des plantes
            foreach (var order in orders)
            {
                order.OrderItems = await LoadRenderableItems(order.Id);
            }
            await SendJsonResponse(response, 200, orders);
        }

        /// <summary>
        /// Recupere une commande par ID.
        /// </summary>
        /// <param name="response">Reponse HTTP.</param>
        /// <param name="user">Utilisateur connecte.</param>
        /// <param name="id">ID de la commande.</param>
        private async Task GetOrder(HttpListenerResponse response, User user, int id)
        {
            var order = await OrderRepo.FindByIdAsync(id);
            if (order == null || (order.UserId != user.Id && !user.IsAdmin))
            {
                await SendError(response, 404, "Commande non trouvée ou accès refusé");
                return;
            }

            order.OrderItems = await LoadRenderableItems(order.Id);

            await SendJsonResponse(response, 200, order);
        }

        /// <summary>
        /// Cree une nouvelle commande.
        /// </summary>
        /// <param name="request">Requete HTTP.</param>
        /// <param name="response">Reponse HTTP.</param>
        /// <param name="user">Utilisateur connecte.</param>
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
                finalOrder.OrderItems = await LoadRenderableItems(createdOrder.Id);
                await SendJsonResponse(response, 201, finalOrder);
            }
            catch (Exception ex)
            {
                await transaction.RollbackAsync();
                await SendError(response, 400, $"Échec de la création de la commande: {ex.Message}");
            }
        }

        /// <summary>
        /// Met a jour le statut d une commande (admin).
        /// </summary>
        /// <param name="request">Requete HTTP.</param>
        /// <param name="response">Reponse HTTP.</param>
        /// <param name="id">ID de la commande.</param>
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

        /// <summary>
        /// Supprime une commande (admin).
        /// </summary>
        /// <param name="response">Reponse HTTP.</param>
        /// <param name="id">ID de la commande.</param>
        private async Task DeleteOrder(HttpListenerResponse response, int id)
        {
             // Supposer que la BDD a un CASCADE DELETE sur order_items
            await OrderRepo.DeleteAsync(id);
            SendEmptyResponse(response, 200);
        }

        /// <summary>
        /// Charge les items d une commande avec les details des plantes.
        /// </summary>
        /// <param name="orderId">ID de la commande.</param>
        /// <returns>Liste des items enrichis.</returns>
        private async Task<List<OrderItem>> LoadRenderableItems(int orderId)
        {
            var rawItems = await OrderItemRepo.FindByOrderIdAsync(orderId);
            var enriched = new List<OrderItem>();

            foreach (var item in rawItems)
            {
                if (!item.PlantId.HasValue)
                {
                    continue;
                }

                var plant = await PlantRepo.FindByIdAsync(item.PlantId.Value);
                if (plant == null)
                {
                    continue;
                }

                item.Plant = plant;
                enriched.Add(item);
            }

            return enriched;
        }

    }
}
