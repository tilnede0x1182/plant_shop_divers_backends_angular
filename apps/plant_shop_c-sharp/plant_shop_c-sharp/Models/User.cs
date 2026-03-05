namespace plant_shop_c_sharp.Models
{
    /// <summary>
    /// Entite utilisateur.
    /// </summary>
    public class User
    {
        public int Id { get; set; }
        public string? Name { get; set; }
        public required string Email { get; set; }
        public string? PasswordHash { get; set; } // Nullable pour la sérialisation
        public bool IsAdmin { get; set; }
        public DateTime CreatedAt { get; set; }
    }
}
