using Microsoft.EntityFrameworkCore;
using plant_shop_asp_EF_core.Data;
using plant_shop_asp_EF_core.Models;
using plant_shop_asp_EF_core.Utils;

namespace plant_shop_asp_EF_core.Services
{
    /// <summary>
    /// Service CRUD pour les utilisateurs.
    /// </summary>
    public class UserService
    {
        private readonly AppDbContext _context;

        /// <summary>
        /// Constructeur avec injection du contexte DB.
        /// </summary>
        /// <param name="context">Contexte Entity Framework.</param>
        public UserService(AppDbContext context)
        {
            _context = context;
        }

        /// <summary>
        /// Recupere tous les utilisateurs.
        /// </summary>
        /// <returns>Liste des utilisateurs.</returns>
        public async Task<IEnumerable<UserResponseDto>> GetAllUsers()
        {
            return await _context.Users
                .OrderByDescending(u => u.IsAdmin)
                .ThenBy(u => u.Name)
                .Select(u => SanitizeUser(u))
                .ToListAsync();
        }

        /// <summary>
        /// Recupere un utilisateur par son ID.
        /// </summary>
        /// <param name="id">ID de l utilisateur.</param>
        /// <returns>DTO utilisateur ou null.</returns>
        public async Task<UserResponseDto?> GetUserById(int id)
        {
            var user = await _context.Users.FindAsync(id);
            return user != null ? SanitizeUser(user) : null;
        }

        /// <summary>
        /// Cree un nouvel utilisateur.
        /// </summary>
        /// <param name="dto">Donnees de creation.</param>
        /// <returns>DTO de l utilisateur cree.</returns>
        public async Task<UserResponseDto> CreateUser(UserCreateRequestDto dto)
        {
            if (await _context.Users.AnyAsync(u => u.Email == dto.Email))
            {
                throw new ApplicationException("Cet email existe déjà");
            }

            var user = new User
            {
                Name = string.IsNullOrWhiteSpace(dto.Name) ? "Utilisateur" : dto.Name,
                Email = dto.Email,
                PasswordHash = PasswordUtil.HashPassword(dto.Password),
                IsAdmin = dto.IsAdmin
            };

            _context.Users.Add(user);
            await _context.SaveChangesAsync();

            return SanitizeUser(user);
        }

        /// <summary>
        /// Met a jour un utilisateur.
        /// </summary>
        /// <param name="id">ID de l utilisateur a modifier.</param>
        /// <param name="dto">Donnees de mise a jour.</param>
        /// <param name="currentUserId">ID de l utilisateur courant.</param>
        /// <param name="isAdmin">True si l utilisateur courant est admin.</param>
        /// <returns>DTO mis a jour ou null.</returns>
        public async Task<UserResponseDto?> UpdateUser(int id, UserUpdateRequestDto dto, int currentUserId, bool isAdmin)
        {
            var userToUpdate = await _context.Users.FindAsync(id);
            if (userToUpdate == null) return null;

            bool isOwner = currentUserId == id;

            if (!isOwner && !isAdmin)
            {
                throw new UnauthorizedAccessException("Accès refusé");
            }

            userToUpdate.Name = dto.Name ?? userToUpdate.Name;
            userToUpdate.Email = dto.Email ?? userToUpdate.Email;

            if (isAdmin && dto.IsAdmin.HasValue)
            {
                userToUpdate.IsAdmin = dto.IsAdmin.Value;
            }

            await _context.SaveChangesAsync();
            return SanitizeUser(userToUpdate);
        }

        /// <summary>
        /// Supprime un utilisateur.
        /// </summary>
        /// <param name="id">ID de l utilisateur a supprimer.</param>
        /// <returns>True si supprime, false sinon.</returns>
        public async Task<bool> DeleteUser(int id)
        {
            var user = await _context.Users.FindAsync(id);
            if (user == null) return false;

            _context.Users.Remove(user);
            await _context.SaveChangesAsync();
            return true;
        }

        /// <summary>
        /// Convertit un User en DTO sans donnees sensibles.
        /// </summary>
        /// <param name="user">Entite User.</param>
        /// <returns>DTO utilisateur.</returns>
        public static UserResponseDto SanitizeUser(User user)
        {
            return new UserResponseDto
            {
                Id = user.Id,
                Name = user.Name,
                Email = user.Email,
                IsAdmin = user.IsAdmin,
                CreatedAt = user.CreatedAt
            };
        }
    }
}
