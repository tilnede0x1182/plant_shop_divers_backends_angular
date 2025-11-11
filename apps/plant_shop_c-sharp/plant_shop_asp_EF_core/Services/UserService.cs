using Microsoft.EntityFrameworkCore;
using plant_shop_asp_EF_core.Data;
using plant_shop_asp_EF_core.Models;
using plant_shop_asp_EF_core.Utils;

namespace plant_shop_asp_EF_core.Services
{
    public class UserService
    {
        private readonly AppDbContext _context;

        public UserService(AppDbContext context)
        {
            _context = context;
        }

        public async Task<IEnumerable<UserResponseDto>> GetAllUsers()
        {
            return await _context.Users
                .OrderByDescending(u => u.IsAdmin)
                .ThenBy(u => u.Name)
                .Select(u => SanitizeUser(u))
                .ToListAsync();
        }

        public async Task<UserResponseDto?> GetUserById(int id)
        {
            var user = await _context.Users.FindAsync(id);
            return user != null ? SanitizeUser(user) : null;
        }

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

        public async Task<bool> DeleteUser(int id)
        {
            var user = await _context.Users.FindAsync(id);
            if (user == null) return false;

            _context.Users.Remove(user);
            await _context.SaveChangesAsync();
            return true;
        }

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
