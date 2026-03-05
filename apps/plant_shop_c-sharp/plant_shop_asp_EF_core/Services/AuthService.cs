using plant_shop_asp_EF_core.Data;
using plant_shop_asp_EF_core.Models;
using plant_shop_asp_EF_core.Utils;
using Microsoft.EntityFrameworkCore;

namespace plant_shop_asp_EF_core.Services
{
    /// <summary>
    /// Service d authentification (validation, inscription, login).
    /// </summary>
    public class AuthService
    {
        private readonly AppDbContext _context;
        private readonly JwtUtil _jwtUtil;

        /// <summary>
        /// Constructeur avec injection du contexte DB et utilitaire JWT.
        /// </summary>
        /// <param name="context">Contexte Entity Framework.</param>
        /// <param name="jwtUtil">Utilitaire de generation JWT.</param>
        public AuthService(AppDbContext context, JwtUtil jwtUtil)
        {
            _context = context;
            _jwtUtil = jwtUtil;
        }

        /// <summary>
        /// Valide les credentials d un utilisateur.
        /// </summary>
        /// <param name="email">Email de l utilisateur.</param>
        /// <param name="password">Mot de passe en clair.</param>
        /// <returns>User si valide, null sinon.</returns>
        public async Task<User?> ValidateUser(string email, string password)
        {
            var user = await _context.Users.FirstOrDefaultAsync(u => u.Email == email);
            if (user != null && PasswordUtil.CheckPassword(password, user.PasswordHash))
            {
                return user;
            }
            return null;
        }

        /// <summary>
        /// Enregistre un nouvel utilisateur.
        /// </summary>
        /// <param name="email">Email unique.</param>
        /// <param name="password">Mot de passe en clair.</param>
        /// <param name="name">Nom optionnel.</param>
        /// <returns>Utilisateur cree.</returns>
        public async Task<User> RegisterUser(string email, string password, string? name)
        {
            var existingUser = await _context.Users.AnyAsync(u => u.Email == email);
            if (existingUser)
            {
                throw new ApplicationException("Email déjà utilisé");
            }

            var user = new User
            {
                Email = email,
                PasswordHash = PasswordUtil.HashPassword(password),
                Name = name,
                IsAdmin = false
            };

            _context.Users.Add(user);
            await _context.SaveChangesAsync();
            return user;
        }

        /// <summary>
        /// Genere un token JWT pour l utilisateur.
        /// </summary>
        /// <param name="user">Utilisateur authentifie.</param>
        /// <returns>Token JWT.</returns>
        public string Login(User user)
        {
            return _jwtUtil.GenerateToken(user);
        }
    }
}
