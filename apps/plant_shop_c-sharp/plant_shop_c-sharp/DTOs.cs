using System;
using System.Collections.Generic;
using Newtonsoft.Json;
using plant_shop_c_sharp.Models;

namespace plant_shop_c_sharp.DTOs
{
    /// <summary>
    /// DTO pour la requete de login.
    /// </summary>
    public class LoginRequest
    {
        public string? Email { get; set; }
        public string? Password { get; set; }
    }

    /// <summary>
    /// DTO pour la requete d inscription.
    /// </summary>
    public class RegisterRequest
    {
        public string? Email { get; set; }
        public string? Password { get; set; }
        public string? Name { get; set; }
    }

    /// <summary>
    /// DTO pour creation/mise a jour d une plante.
    /// </summary>
    public class PlantRequest
    {
        public string? Name { get; set; }
        public string? Description { get; set; }
        public decimal Price { get; set; } = -1;
        public int Stock { get; set; } = -1;
    }

    /// <summary>
    /// DTO pour creation d un utilisateur.
    /// </summary>
    public class UserCreateRequest
    {
        public string? Name { get; set; }
        public required string Email { get; set; }
        public required string Password { get; set; }
        [JsonProperty("admin")]
        public bool IsAdmin { get; set; } = false;
    }

    /// <summary>
    /// DTO pour mise a jour d un utilisateur.
    /// </summary>
    public class UserUpdateRequest
    {
        public string? Name { get; set; }
        public string? Email { get; set; }
        [JsonProperty("admin")]
        public bool? IsAdmin { get; set; }
    }

    /// <summary>
    /// DTO pour un item de commande.
    /// </summary>
    public class OrderItemRequest
    {
        public int PlantId { get; set; }
        public int Quantity { get; set; }
    }

    /// <summary>
    /// DTO pour creation d une commande.
    /// </summary>
    public class OrderRequest
    {
        public List<OrderItemRequest>? Items { get; set; }
    }

    /// <summary>
    /// DTO pour mise a jour du statut.
    /// </summary>
    public class StatusUpdateRequest
    {
        public string? Status { get; set; }
    }

    /// <summary>
    /// DTO de reponse utilisateur (sans password).
    /// </summary>
    public class UserResponseDto
    {
        public int Id { get; set; }
        public string? Name { get; set; }
        public required string Email { get; set; }
        public bool Admin { get; set; }
        public DateTime CreatedAt { get; set; }
    }

    /// <summary>
    /// Mapper User vers UserResponseDto.
    /// </summary>
    public static class UserDtoMapper
    {
        /// <summary>
        /// Convertit un User en DTO.
        /// </summary>
        /// <param name="user">Entite User.</param>
        /// <returns>DTO sans password.</returns>
        public static UserResponseDto ToDto(User user)
        {
            return new UserResponseDto
            {
                Id = user.Id,
                Name = user.Name,
                Email = user.Email,
                Admin = user.IsAdmin,
                CreatedAt = user.CreatedAt
            };
        }
    }
}
