using System.Collections.Generic;

namespace plant_shop_c_sharp.DTOs
{
    public class LoginRequest
    {
        public string? Email { get; set; }
        public string? Password { get; set; }
    }

    public class RegisterRequest
    {
        public string? Email { get; set; }
        public string? Password { get; set; }
        public string? Name { get; set; }
    }

    public class PlantRequest
    {
        public string? Name { get; set; }
        public string? Description { get; set; }
        public decimal Price { get; set; } = -1;
        public int Stock { get; set; } = -1;
    }

    public class UserCreateRequest
    {
        public string? Name { get; set; }
        public required string Email { get; set; }
        public required string Password { get; set; }
        public bool IsAdmin { get; set; } = false;
    }

    public class UserUpdateRequest
    {
        public string? Name { get; set; }
        public string? Email { get; set; }
        public bool? IsAdmin { get; set; }
    }

    public class OrderItemRequest
    {
        public int PlantId { get; set; }
        public int Quantity { get; set; }
    }

    public class OrderRequest
    {
        public List<OrderItemRequest>? Items { get; set; }
    }

    public class StatusUpdateRequest
    {
        public string? Status { get; set; }
    }
}
