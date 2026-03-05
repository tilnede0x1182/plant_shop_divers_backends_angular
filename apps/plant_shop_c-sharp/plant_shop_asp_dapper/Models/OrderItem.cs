namespace plant_shop_asp_dapper.Models
{
    /// <summary>
    /// Ligne de commande.
    /// </summary>
    public class OrderItem
    {
        public int Id { get; set; }
        public int OrderId { get; set; }
        public int PlantId { get; set; }
        public int Quantity { get; set; }
        public decimal Price { get; set; }

        // Propriété de navigation
        public Plant? Plant { get; set; }
    }
}
