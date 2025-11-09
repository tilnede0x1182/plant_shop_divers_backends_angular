using plant_shop_asp_EF_core.Data;
using plant_shop_asp_EF_core.Models;
using plant_shop_asp_EF_core.Utils;
using Microsoft.EntityFrameworkCore;

namespace plant_shop_asp_EF_core.Services
{
    public class AuthService
    {
        private readonly AppDbContext _context;
        private readonly JwtUtil _jwtUtil;

        public AuthService(AppDbContext context, JwtUtil jwtUtil)
        {
            _context = context;
            _jwtUtil = jwtUtil;
        }

        public async Task<User?> ValidateUser(string email, string password)
        {
            var user = await _context.Users.FirstOrDefaultAsync(u => u.Email == email);
            if (user != null && PasswordUtil.CheckPassword(password, user.PasswordHash))
            {
                return user;
            }
            return null;
        }

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

        public string Login(User user)
        {
            return _jwtUtil.GenerateToken(user);
        }
    }
}
