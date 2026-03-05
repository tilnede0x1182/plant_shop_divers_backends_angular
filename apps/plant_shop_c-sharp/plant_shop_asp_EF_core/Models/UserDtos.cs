using System.Text.Json.Serialization;

namespace plant_shop_asp_EF_core.Models
{
    /// <summary>
    /// DTO pour la creation d un utilisateur.
    /// </summary>
    public class UserCreateRequestDto
    {
        public string? Name { get; set; }
        public required string Email { get; set; }
        public required string Password { get; set; }

        [JsonPropertyName("admin")]
        public bool IsAdmin { get; set; } = false;
    }

    /// <summary>
    /// DTO pour la mise a jour d un utilisateur.
    /// </summary>
    public class UserUpdateRequestDto
    {
        public string? Name { get; set; }
        public string? Email { get; set; }

        [JsonPropertyName("admin")]
        public bool? IsAdmin { get; set; }
    }

    /// <summary>
    /// DTO de reponse contenant les infos publiques d un utilisateur.
    /// </summary>
    public class UserResponseDto
    {
        public int Id { get; set; }
        public string? Name { get; set; }
        public required string Email { get; set; }

        [JsonPropertyName("admin")]
        public bool IsAdmin { get; set; }

        public DateTime CreatedAt { get; set; }
    }
}
