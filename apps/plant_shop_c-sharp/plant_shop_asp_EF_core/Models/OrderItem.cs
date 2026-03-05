using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Text.Json.Serialization;

namespace plant_shop_asp_EF_core.Models
{
    /// <summary>
    /// Ligne de commande reliant une commande a une plante.
    /// </summary>
    [Table("order_items")]
    public class OrderItem
    {
        [Key]
        [Column("id")]
        public int Id { get; set; }

        [Required]
        [Column("order_id")]
        public int OrderId { get; set; }

        [Required]
        [Column("plant_id")]
        public int PlantId { get; set; }

        [Required]
        [Column("quantity")]
        public int Quantity { get; set; }

        [Required]
        [Column("price", TypeName = "decimal(18, 2)")]
        public decimal Price { get; set; } // Prix au moment de la commande

        // Relations
        [ForeignKey("PlantId")]
        public Plant? Plant { get; set; }

        [JsonIgnore] // Evite la référence circulaire
        [ForeignKey("OrderId")]
        public Order? Order { get; set; }
    }
}
