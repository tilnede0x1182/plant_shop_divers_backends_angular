namespace plant_shop_asp_dapper.Models
{
    public class Order
    {
        public int Id { get; set; }
        public int UserId { get; set; }
        public decimal TotalPrice { get; set; }
        public string Status { get; set; } = "pending";
        public DateTime CreatedAt { get; set; }

        // Propriétés de navigation (remplies manuellement)
        public List<OrderItem> OrderItems { get; set; } = new List<OrderItem>();
    }
}
