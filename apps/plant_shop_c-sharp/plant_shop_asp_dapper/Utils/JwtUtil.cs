using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.IdentityModel.Tokens;
using plant_shop_asp_dapper.Models;

namespace plant_shop_asp_dapper.Utils
{
    /// <summary>
    /// Utilitaire de generation et validation JWT.
    /// </summary>    // (Identique au Projet 2)
    public class JwtUtil
    {
        private readonly IConfiguration _config;

        public JwtUtil(IConfiguration config)
        {
            _config = config;
        }

        /// <summary>
        /// Genere un token JWT pour un utilisateur.
        /// </summary>
        /// <param name="user">Utilisateur.</param>
        /// <returns>Token JWT.</returns>
        public string GenerateToken(User user)
        {
            var tokenHandler = new JwtSecurityTokenHandler();

            var key = Encoding.ASCII.GetBytes(_config["Jwt:Key"] ?? throw new InvalidOperationException("Jwt:Key non configurée"));
            var issuer = _config["Jwt:Issuer"] ?? "plant-shop-api";
            var audience = _config["Jwt:Audience"] ?? "plant-shop-clients";

            var tokenDescriptor = new SecurityTokenDescriptor
            {
                Subject = new ClaimsIdentity(new[]
                {
                    new Claim(JwtRegisteredClaimNames.Sub, user.Id.ToString()),
                    new Claim(JwtRegisteredClaimNames.Email, user.Email),
                    new Claim(JwtRegisteredClaimNames.Name, user.Name ?? string.Empty),
                    new Claim(ClaimTypes.Role, user.IsAdmin ? "Admin" : "User")
                }),
                Expires = DateTime.UtcNow.AddHours(1),
                Issuer = issuer,
                Audience = audience,
                SigningCredentials = new SigningCredentials(new SymmetricSecurityKey(key), SecurityAlgorithms.HmacSha256Signature)
            };
            var token = tokenHandler.CreateToken(tokenDescriptor);
            return tokenHandler.WriteToken(token);
        }
    }
}
