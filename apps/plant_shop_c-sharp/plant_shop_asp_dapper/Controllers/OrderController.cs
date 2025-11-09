using Microsoft.AspNetCore.Mvc;
using plant_shop_asp_dapper.Repositories;
using Microsoft.AspNetCore.Authorization;
using plant_shop_asp_dapper.Models;

namespace plant_shop_asp_dapper.Controllers
{
    [ApiController]
    [Authorize(Roles = "Admin")] // Routes admin
    public class OrderController : BaseController
    {
        private readonly OrderRepository _orderRepo;
        private readonly OrderItemRepository _itemRepo;

        public OrderController(OrderRepository orderRepo, OrderItemRepository itemRepo)
        {
            _orderRepo = orderRepo;
            _itemRepo = itemRepo;
        }

        // PATCH: api/admin/orders/5 (ou /api/orders/5, le test Java utilise /orders/5)
        // Pour la compatibilité avec le test Java, nous utilisons la route non-admin
        [HttpPatch(Routes.OrderDetail)]
        public async Task<IActionResult> UpdateOrderStatus(int id, [FromBody] OrderStatusUpdateDto dto)
        {
            var order = await _orderRepo.FindByIdAsync(id);
            if (order == null)
            {
                return NotFound();
            }

            if (dto.Status != null)
            {
                order.Status = dto.Status;
                await _orderRepo.UpdateAsync(order);
            }

            return Ok(order);
        }

        // DELETE: api/admin/orders/5 (ou /api/orders/5, le test Java utilise /orders/5)
        [HttpDelete(Routes.OrderDetail)]
        public async Task<IActionResult> DeleteOrder(int id)
        {
            // Supprimer les items d'abord (Dapper ne gère pas le cascade auto)
            await _itemRepo.DeleteByOrderIdAsync(id);
            await _orderRepo.DeleteAsync(id);
            return Ok(); // 200 OK
        }
    }

    public class OrderStatusUpdateDto { public string? Status { get; set; } }
}
