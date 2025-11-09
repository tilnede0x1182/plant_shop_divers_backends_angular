using System.ComponentModel.DataAnnotations.Schema;

namespace plant_shop_c_sharp.Models
{
    public class Order
    {
        public int Id { get; set; }
        public int UserId { get; set; }

        [Column(TypeName = "decimal(18, 2)")]
        public decimal TotalPrice { get; set; }
        public string Status { get; set; } = "pending";
        public DateTime CreatedAt { get; set; }

        // Propriété de navigation (non mappée directement par Npgsql simple)
        public List<OrderItem> OrderItems { get; set; } = new List<OrderItem>();
        public User? User { get; set; }
    }
}
