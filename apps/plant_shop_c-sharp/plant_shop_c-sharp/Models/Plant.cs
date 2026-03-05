using System.ComponentModel.DataAnnotations.Schema;

namespace plant_shop_c_sharp.Models
{
    /// <summary>
    /// Entite plante avec nom, description, prix et stock.
    /// </summary>
    public class Plant
    {
        public int Id { get; set; }
        public required string Name { get; set; }
        public string? Description { get; set; }

        [Column(TypeName = "decimal(18, 2)")]
        public decimal Price { get; set; }
        public int Stock { get; set; }
        public DateTime CreatedAt { get; set; }
    }
}
