using Microsoft.AspNetCore.Mvc;
using plant_shop_asp_dapper.Repositories;
using Microsoft.AspNetCore.Authorization;
using System.Security.Claims;
using plant_shop_asp_dapper.Models;

namespace plant_shop_asp_dapper.Controllers
{
    /// <summary>
    /// Controleur CRUD pour les utilisateurs.
    /// </summary>
    [ApiController]
    [Authorize] // Routes profil
    public class UserController : BaseController
    {
        private readonly UserRepository _userRepo;

        /// <summary>
        /// Constructeur avec injection du repository.
        /// </summary>
        /// <param name="userRepo">Repository utilisateurs.</param>
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
        public async Task<ActionResult<UserResponseDto>> GetUser(int id)
        {
            if (GetCurrentUserId() != id && !User.IsInRole("Admin"))
            {
                return Forbid();
            }

            var user = await _userRepo.FindByIdAsync(id);
            if (user == null) return NotFound();

            return Ok(UserDtoMapper.ToDto(user));
        }

        // PATCH: api/users/5
        [HttpPatch(Routes.UserUpdate)]
        public async Task<ActionResult<UserResponseDto>> UpdateUser(int id, [FromBody] UserUpdateDto dto)
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
            return Ok(UserDtoMapper.ToDto(user));
        }

        // DELETE: api/users/5 (admin)
        [HttpDelete(Routes.UserDetail)]
        [Authorize(Roles = "Admin")]
        public async Task<IActionResult> DeleteUser(int id)
        {
            await _userRepo.DeleteAsync(id);
            return Ok();
        }

    }
}
