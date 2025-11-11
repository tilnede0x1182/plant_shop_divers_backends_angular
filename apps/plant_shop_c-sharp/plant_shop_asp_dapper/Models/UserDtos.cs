using System.Text.Json.Serialization;

namespace plant_shop_asp_dapper.Models
{
    public class UserCreateDto
    {
        public string? Name { get; set; }
        public required string Email { get; set; }
        public required string Password { get; set; }

        [JsonPropertyName("admin")]
        public bool IsAdmin { get; set; }
    }

    public class UserUpdateDto
    {
        public string? Name { get; set; }
        public string? Email { get; set; }

        [JsonPropertyName("admin")]
        public bool? IsAdmin { get; set; }
    }

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
