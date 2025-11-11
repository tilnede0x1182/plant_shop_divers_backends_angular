using Microsoft.AspNetCore.Mvc;
using plant_shop_asp_dapper.Repositories;
using plant_shop_asp_dapper.Models;
using Microsoft.AspNetCore.Authorization;
using System.Security.Claims;
using System.Collections.Generic;
using Dapper;
using plant_shop_asp_dapper.Data;

namespace plant_shop_asp_dapper.Controllers
{
    [ApiController]
    [Authorize] // Routes Commandes
    public class OrdersController : BaseController
    {
        private readonly OrderRepository _orderRepo;
        private readonly OrderItemRepository _itemRepo;
        private readonly PlantRepository _plantRepo;
        private readonly DbConnectionFactory _factory;

        public OrdersController(OrderRepository orderRepo, OrderItemRepository itemRepo, PlantRepository plantRepo, DbConnectionFactory factory)
        {
            _orderRepo = orderRepo;
            _itemRepo = itemRepo;
            _plantRepo = plantRepo;
            _factory = factory;
        }

        private int GetCurrentUserId() => int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);

        // GET: api/orders
        [HttpGet(Routes.OrdersList)]
        public async Task<ActionResult<IEnumerable<Order>>> GetOrders()
        {
            var userId = GetCurrentUserId();
            var orders = (await _orderRepo.FindByUserIdAsync(userId)).ToList();

            // Enrichissement (Dapper ne fait pas l'inclusion auto comme EF)
            foreach (var order in orders)
            {
                order.OrderItems = (await _itemRepo.FindByOrderIdAsync(order.Id)).ToList();
            }
            return Ok(orders);
        }

        // GET: api/orders/5
        [HttpGet(Routes.OrderDetail)]
        public async Task<ActionResult<Order>> GetOrder(int id)
        {
            var userId = GetCurrentUserId();
            var order = await _orderRepo.FindByIdAsync(id);

            if (order == null) return NotFound();
            if (order.UserId != userId && !User.IsInRole("Admin")) return Forbid();

            order.OrderItems = (await _itemRepo.FindByOrderIdAsync(order.Id)).ToList();
            return Ok(order);
        }

        // POST: api/orders
        [HttpPost(Routes.OrderCreate)]
        public async Task<ActionResult<Order>> PostOrder(OrderRequestDto orderDto)
        {
            var userId = GetCurrentUserId();
            if (orderDto.Items == null || !orderDto.Items.Any())
            {
                return BadRequest("La commande ne contient aucun article.");
            }

            // Dapper gère mieux les transactions manuellement
            using var connection = _factory.CreateConnection();
            connection.Open();
            await using var transaction = connection.BeginTransaction();

            try
            {
                decimal totalPrice = 0;
                var order = new Order
                {
                    UserId = userId,
                    Status = "confirmed",
                    TotalPrice = 0
                };

                // 1. Créer la commande pour obtenir l'ID
                var sqlOrder = @"INSERT INTO orders (user_id, total_price, status, created_at)
                                 VALUES (@UserId, @TotalPrice, @Status, @CreatedAt) RETURNING id";
                order.CreatedAt = DateTime.UtcNow;
                order.Id = await connection.ExecuteScalarAsync<int>(sqlOrder, order, transaction);

                // 2. Traiter les items
                foreach (var itemDto in orderDto.Items)
                {
                    // VERIFIER LE STOCK (critique)
                    var plant = await _plantRepo.FindByIdAsync(itemDto.PlantId);
                    if (plant == null) throw new KeyNotFoundException($"Plante {itemDto.PlantId} non trouvée");
                    if (plant.Stock < itemDto.Quantity) throw new InvalidOperationException($"Stock insuffisant pour {plant.Name}");

                    // Mettre à jour le stock
                    await connection.ExecuteAsync(
                        "UPDATE plants SET stock = @Stock WHERE id = @Id",
                        new { Id = plant.Id, Stock = plant.Stock - itemDto.Quantity },
                        transaction);

                    var itemPrice = plant.Price;
                    totalPrice += itemPrice * itemDto.Quantity;

                    var orderItem = new OrderItem
                    {
                        OrderId = order.Id,
                        PlantId = itemDto.PlantId,
                        Quantity = itemDto.Quantity,
                        Price = itemPrice
                    };

                    await _itemRepo.CreateAsync(orderItem); // Utilise sa propre connexion
                }

                // 3. Mettre à jour le prix total
                order.TotalPrice = totalPrice;
                await connection.ExecuteAsync(
                    "UPDATE orders SET total_price = @TotalPrice WHERE id = @Id",
                    new { order.TotalPrice, order.Id },
                    transaction);

                await transaction.CommitAsync();

                // Recharger pour la réponse
                var result = await GetOrder(order.Id);
                return CreatedAtAction(nameof(GetOrder), new { id = order.Id }, result.Value);
            }
            catch (Exception ex)
            {
                await transaction.RollbackAsync();
                return BadRequest(new { error = $"Échec de la création de la commande: {ex.Message}" });
            }
        }
    }

    // DTOs
    public class OrderItemRequestDto { public int PlantId { get; set; } public int Quantity { get; set; } }
    public class OrderRequestDto { public List<OrderItemRequestDto>? Items { get; set; } }
}
