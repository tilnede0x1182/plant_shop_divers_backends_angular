using System.Text.Json.Serialization;

namespace plant_shop_asp_dapper.Models
{
    /// <summary>
    /// Entite utilisateur.
    /// </summary>    // Dapper mappe directement aux noms de colonnes (snake_case)
    // si nous n'utilisons pas de mappeur customisé.
    // Pour la simplicité, nous utilisons des propriétés C# standards (PascalCase)
    // et nous utiliserons des alias SQL dans les repositories.

    public class User
    {
        public int Id { get; set; }
        public string? Name { get; set; }
        public required string Email { get; set; }
        [JsonIgnore]
        public string PasswordHash { get; set; } = string.Empty;
        public bool IsAdmin { get; set; }
        public DateTime CreatedAt { get; set; }
    }
}
