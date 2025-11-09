using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using plant_shop_asp_EF_core.Data;
using plant_shop_asp_EF_core.Models;
using Microsoft.AspNetCore.Authorization;
using System.Security.Claims;

namespace plant_shop_asp_EF_core.Controllers
{
    [ApiController]
    [Authorize] // Toutes les routes de commande nécessitent une connexion
    [Route("api/[controller]")]
    public class OrdersController : ControllerBase
    {
        private readonly AppDbContext _context;

        public OrdersController(AppDbContext context)
        {
            _context = context;
        }

        private int GetCurrentUserId()
        {
            var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);
            if (userId == null)
            {
                throw new UnauthorizedAccessException("Impossible de trouver l'ID utilisateur dans le token.");
            }
            return int.Parse(userId);
        }

        private bool IsAdmin()
        {
             return User.IsInRole("Admin");
        }

        // GET: api/orders
        [HttpGet]
        public async Task<ActionResult<IEnumerable<Order>>> GetOrders()
        {
            var userId = GetCurrentUserId();
            return await _context.Orders
                .Where(o => o.UserId == userId)
                .Include(o => o.OrderItems)
                    .ThenInclude(oi => oi.Plant)
                .OrderByDescending(o => o.CreatedAt)
                .ToListAsync();
        }

        // GET: api/orders/5
        [HttpGet("{id}")]
        public async Task<ActionResult<Order>> GetOrder(int id)
        {
            var userId = GetCurrentUserId();
            var order = await _context.Orders
                .Include(o => o.OrderItems)
                    .ThenInclude(oi => oi.Plant)
                .FirstOrDefaultAsync(o => o.Id == id);

            if (order == null)
            {
                return NotFound();
            }

            // Un utilisateur ne peut voir que ses propres commandes (sauf si admin)
            if (order.UserId != userId && !IsAdmin())
            {
                return Forbid();
            }

            return order;
        }

        // POST: api/orders
        [HttpPost]
        public async Task<ActionResult<Order>> PostOrder(OrderRequestDto orderDto)
        {
            var userId = GetCurrentUserId();

            if (orderDto.Items == null || !orderDto.Items.Any())
            {
                return BadRequest("La commande ne contient aucun article.");
            }

            // Utilisation d'une transaction
            await using var transaction = await _context.Database.BeginTransactionAsync();
            try
            {
                decimal totalPrice = 0;
                var order = new Order
                {
                    UserId = userId,
                    Status = "confirmed",
                    TotalPrice = 0 // Calculé ci-dessous
                };
                _context.Orders.Add(order);
                await _context.SaveChangesAsync(); // Sauvegarde pour obtenir l'ID de la commande

                var orderItems = new List<OrderItem>();

                foreach (var itemDto in orderDto.Items)
                {
                    var plant = await _context.Plants.FindAsync(itemDto.PlantId);
                    if (plant == null) throw new KeyNotFoundException($"Plante {itemDto.PlantId} non trouvée");
                    if (plant.Stock < itemDto.Quantity) throw new InvalidOperationException($"Stock insuffisant pour {plant.Name}");

                    plant.Stock -= itemDto.Quantity;
                    var itemPrice = plant.Price;
                    totalPrice += itemPrice * itemDto.Quantity;

                    orderItems.Add(new OrderItem
                    {
                        OrderId = order.Id,
                        PlantId = itemDto.PlantId,
                        Quantity = itemDto.Quantity,
                        Price = itemPrice // Prix au moment de l'achat
                    });
                }

                order.TotalPrice = totalPrice;
                _context.OrderItems.AddRange(orderItems);
                await _context.SaveChangesAsync();

                await transaction.CommitAsync();

                // Recharger pour la réponse (le test Java le vérifie)
                var result = await _context.Orders
                    .Include(o => o.OrderItems)
                        .ThenInclude(oi => oi.Plant)
                    .FirstAsync(o => o.Id == order.Id);

                return CreatedAtAction(nameof(GetOrder), new { id = order.Id }, result);
            }
            catch (Exception ex)
            {
                await transaction.RollbackAsync();
                return BadRequest(new { error = $"Échec de la création de la commande: {ex.Message}" });
            }
        }

        // PATCH: api/orders/5 (Admin uniquement)
        [HttpPatch("{id}")]
        [Authorize(Roles = "Admin")]
        public async Task<IActionResult> UpdateOrderStatus(int id, [FromBody] OrderStatusUpdateDto dto)
        {
            var order = await _context.Orders.FindAsync(id);
            if (order == null)
            {
                return NotFound();
            }

            if (dto.Status != null)
            {
                order.Status = dto.Status;
                await _context.SaveChangesAsync();
            }

            return Ok(order);
        }

        // DELETE: api/orders/5 (Admin uniquement)
        [HttpDelete("{id}")]
        [Authorize(Roles = "Admin")]
        public async Task<IActionResult> DeleteOrder(int id)
        {
            var order = await _context.Orders.FindAsync(id);
            if (order == null)
            {
                return NotFound();
            }

            // EF Core gère la suppression en cascade si configuré,
            // sinon il faut supprimer les OrderItems d'abord.
            // Supposons que la BDD gère le CASCADE.
            _context.Orders.Remove(order);
            await _context.SaveChangesAsync();

            return Ok(); // 200 OK pour le test Java
        }
    }

    // DTOs
    public class OrderItemRequestDto
    {
        public int PlantId { get; set; }
        public int Quantity { get; set; }
    }
    public class OrderRequestDto
    {
        public List<OrderItemRequestDto>? Items { get; set; }
    }
    public class OrderStatusUpdateDto
    {
        public string? Status { get; set; }
    }
}
