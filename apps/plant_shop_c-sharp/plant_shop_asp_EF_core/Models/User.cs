using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Text.Json.Serialization;

namespace plant_shop_asp_EF_core.Models
{
    /// <summary>
    /// Entite utilisateur avec relation vers ses commandes.
    /// </summary>
    [Table("users")]
    public class User
    {
        [Key]
        [Column("id")]
        public int Id { get; set; }

        [Column("name")]
        public string? Name { get; set; }

        [Required]
        [EmailAddress]
        [Column("email")]
        public required string Email { get; set; }

        [Required]
        [Column("password_hash")]
        [JsonIgnore] // Ne jamais exposer le hash
        public string PasswordHash { get; set; } = string.Empty;

        [Column("is_admin")]
        public bool IsAdmin { get; set; } = false;

        [Column("created_at")]
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        // Relation
        [JsonIgnore]
        public ICollection<Order> Orders { get; set; } = new List<Order>();
    }
}
