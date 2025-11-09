using Microsoft.EntityFrameworkCore;
using plant_shop_asp_EF_core.Data;
using plant_shop_asp_EF_core.Models;

namespace plant_shop_asp_EF_core.Services
{
    public class UserService
    {
        private readonly AppDbContext _context;

        public UserService(AppDbContext context)
        {
            _context = context;
        }

        public async Task<IEnumerable<User>> GetAllUsers()
        {
            return await _context.Users
                .OrderByDescending(u => u.IsAdmin)
                .ThenBy(u => u.Name)
                .Select(u => SanitizeUser(u)) // Exclure les hashs
                .ToListAsync();
        }

        public async Task<User?> GetUserById(int id)
        {
            var user = await _context.Users.FindAsync(id);
            return user != null ? SanitizeUser(user) : null;
        }

        public async Task<User?> UpdateUser(int id, UserUpdateRequestDto dto, User currentUser)
        {
            var userToUpdate = await _context.Users.FindAsync(id);
            if (userToUpdate == null) return null;

            // Logique de permission
            bool isOwner = currentUser.Id == id;
            bool isAdmin = currentUser.IsAdmin;

            if (!isOwner && !isAdmin)
            {
                throw new UnauthorizedAccessException("Accès refusé");
            }

            userToUpdate.Name = dto.Name ?? userToUpdate.Name;
            userToUpdate.Email = dto.Email ?? userToUpdate.Email;

            // Seul un admin peut changer le statut admin
            if (isAdmin && dto.IsAdmin.HasValue)
            {
                userToUpdate.IsAdmin = dto.IsAdmin.Value;
            }

            await _context.SaveChangesAsync();
            return SanitizeUser(userToUpdate);
        }

        public async Task<bool> DeleteUser(int id)
        {
            var user = await _context.Users.FindAsync(id);
            if (user == null) return false;

            _context.Users.Remove(user);
            await _context.SaveChangesAsync();
            return true;
        }

        // Exclut le hash du mot de passe des objets retournés
        public static User SanitizeUser(User user)
        {
            user.PasswordHash = ""; // Vide le hash pour la sécurité
            return user;
        }
    }

    // DTO pour les mises à jour
    public class UserUpdateRequestDto
    {
        public string? Name { get; set; }
        public string? Email { get; set; }
        public bool? IsAdmin { get; set; }
    }
}
