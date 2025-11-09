using Microsoft.AspNetCore.Mvc;
using plant_shop_asp_dapper.Repositories;
using plant_shop_asp_dapper.Models;
using Microsoft.AspNetCore.Authorization;

namespace plant_shop_asp_dapper.Controllers
{
    [ApiController]
    [Authorize(Roles = "Admin")] // Routes admin
    public class UsersController : BaseController
    {
        private readonly UserRepository _userRepo;

        public UsersController(UserRepository userRepo)
        {
            _userRepo = userRepo;
        }

        // GET: api/admin/users
        [HttpGet(Routes.AdminUsersList)]
        public async Task<ActionResult<IEnumerable<User>>> GetAllUsers()
        {
            var users = await _userRepo.FindAllAsync();
            return Ok(users);
        }

        // GET: api/admin/users/5
        [HttpGet(Routes.AdminUserDetail)]
        public async Task<ActionResult<User>> GetAdminUser(int id)
        {
            var user = await _userRepo.FindByIdAsync(id);
            if (user == null) return NotFound();
            user.PasswordHash = ""; // Nettoyage
            return Ok(user);
        }

        // PATCH: api/admin/users/5
        [HttpPatch(Routes.AdminUserUpdate)]
        public async Task<ActionResult<User>> UpdateAdminUser(int id, [FromBody] UserUpdateDto dto)
        {
            var user = await _userRepo.FindByIdAsync(id);
            if (user == null) return NotFound();

            // L'admin peut tout mettre à jour
            user.Name = dto.Name ?? user.Name;
            user.Email = dto.Email ?? user.Email;
            user.IsAdmin = dto.IsAdmin ?? user.IsAdmin;

            await _userRepo.UpdateAsync(user);
            user.PasswordHash = ""; // Nettoyage
            return Ok(user);
        }

        // DELETE: api/admin/users/5
        [HttpDelete(Routes.AdminUserDelete)]
        public async Task<IActionResult> DeleteUser(int id)
        {
            await _userRepo.DeleteAsync(id);
            return Ok(); // 200 OK
        }
    }

    // DTO partagé (utilisé aussi par UserController)
    public class UserUpdateDto
    {
        public string? Name { get; set; }
        public string? Email { get; set; }
        public bool? IsAdmin { get; set; }
    }
}
