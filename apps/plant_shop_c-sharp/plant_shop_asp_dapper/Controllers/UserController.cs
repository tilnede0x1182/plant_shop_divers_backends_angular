using Microsoft.AspNetCore.Mvc;
using plant_shop_asp_dapper.Repositories;
using Microsoft.AspNetCore.Authorization;
using System.Security.Claims;
using plant_shop_asp_dapper.Models;

namespace plant_shop_asp_dapper.Controllers
{
    [ApiController]
    [Authorize] // Routes profil
    public class UserController : BaseController
    {
        private readonly UserRepository _userRepo;

        public UserController(UserRepository userRepo)
        {
            _userRepo = userRepo;
        }

        private int GetCurrentUserId()
        {
            return int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        }

        // GET: api/users/5
        [HttpGet(Routes.UserDetail)]
        public async Task<ActionResult<User>> GetUser(int id)
        {
            if (GetCurrentUserId() != id && !User.IsInRole("Admin"))
            {
                return Forbid();
            }

            var user = await _userRepo.FindByIdAsync(id);
            if (user == null) return NotFound();

            user.PasswordHash = ""; // Nettoyage
            return Ok(user);
        }

        // PATCH: api/users/5
        [HttpPatch(Routes.UserUpdate)]
        public async Task<ActionResult<User>> UpdateUser(int id, [FromBody] UserUpdateDto dto)
        {
            if (GetCurrentUserId() != id && !User.IsInRole("Admin"))
            {
                return Forbid();
            }

            var user = await _userRepo.FindByIdAsync(id);
            if (user == null) return NotFound();

            user.Name = dto.Name ?? user.Name;
            user.Email = dto.Email ?? user.Email;

            // Si un non-admin essaie de s'auto-promouvoir, on ignore
            if (User.IsInRole("Admin") && dto.IsAdmin.HasValue)
            {
                user.IsAdmin = dto.IsAdmin.Value;
            }

            await _userRepo.UpdateAsync(user);
            user.PasswordHash = ""; // Nettoyage
            return Ok(user);
        }
    }
}
