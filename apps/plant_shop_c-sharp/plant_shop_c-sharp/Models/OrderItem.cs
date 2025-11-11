using System.ComponentModel.DataAnnotations.Schema;

namespace plant_shop_c_sharp.Models
{
    public class OrderItem
    {
        public int Id { get; set; }
        public int OrderId { get; set; }
        public int? PlantId { get; set; }
        public int Quantity { get; set; }

        [Column(TypeName = "decimal(18, 2)")]
        public decimal Price { get; set; } // Prix au moment de la commande

        // Propriétés de navigation
        public Plant? Plant { get; set; }
        public Order? Order { get; set; }
    }
}
