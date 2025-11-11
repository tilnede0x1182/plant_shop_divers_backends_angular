using Microsoft.AspNetCore.Mvc;
using plant_shop_asp_EF_core.Services;
using plant_shop_asp_EF_core.Models;
using Microsoft.AspNetCore.Authorization;
using System.Security.Claims;

namespace plant_shop_asp_EF_core.Controllers
{
    [ApiController]
    [Authorize] // Toutes les routes utilisateurs nécessitent une connexion
    public class UsersController : ControllerBase
    {
        private readonly UserService _userService;

        public UsersController(UserService userService)
        {
            _userService = userService;
        }

        private int GetCurrentUserId()
        {
            var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);
            if (userId == null) throw new UnauthorizedAccessException("ID utilisateur non trouvé");
            return int.Parse(userId);
        }

        private bool IsAdmin()
        {
             return User.IsInRole("Admin");
        }

        // GET: api/users (liste)
        [HttpGet("api/users")]
        public async Task<IActionResult> GetUsers()
        {
            if (!IsAdmin())
            {
                return StatusCode(StatusCodes.Status403Forbidden, new { error = "L'accès à la liste /api/users est interdit" });
            }

            return Ok(await _userService.GetAllUsers());
        }

        // GET: api/users/5 (Route profil ET admin)
        [HttpGet("api/users/{id}")]
        public async Task<IActionResult> GetUser(int id)
        {
            var currentUserId = GetCurrentUserId();
            if (currentUserId != id && !IsAdmin())
            {
                return Forbid();
            }

            var user = await _userService.GetUserById(id);
            if (user == null)
            {
                return NotFound();
            }
            return Ok(user);
        }

        // PATCH: api/users/5 (Route profil ET admin)
        [HttpPatch("api/users/{id}")]
        public async Task<IActionResult> UpdateUser(int id, [FromBody] UserUpdateRequestDto dto)
        {
            try
            {
                var updatedUser = await _userService.UpdateUser(id, dto, GetCurrentUserId(), IsAdmin());
                if (updatedUser == null) return NotFound();
                return Ok(updatedUser);
            }
            catch (UnauthorizedAccessException)
            {
                return Forbid();
            }
        }

        // --- Routes Admin ---

        [HttpPost("api/users")]
        [Authorize(Roles = "Admin")]
        public async Task<IActionResult> CreateUser([FromBody] UserCreateRequestDto dto)
        {
            var created = await _userService.CreateUser(dto);
            return Created($"/api/users/{created.Id}", created);
        }

        [HttpPost("api/admin/users")]
        [Authorize(Roles = "Admin")]
        public Task<IActionResult> CreateAdminUser([FromBody] UserCreateRequestDto dto)
            => CreateUser(dto);

        // GET: api/admin/users
        [HttpGet("api/admin/users")]
        [Authorize(Roles = "Admin")]
        public async Task<IActionResult> GetAllUsers()
        {
            return Ok(await _userService.GetAllUsers());
        }

        // GET: api/admin/users/5 (Redirige ou duplique la logique de GetUser)
        [HttpGet("api/admin/users/{id}")]
        [Authorize(Roles = "Admin")]
        public async Task<IActionResult> GetAdminUser(int id)
        {
            return await GetUser(id);
        }

        // PATCH: api/admin/users/5
        [HttpPatch("api/admin/users/{id}")]
        [Authorize(Roles = "Admin")]
        public async Task<IActionResult> UpdateAdminUser(int id, [FromBody] UserUpdateRequestDto dto)
        {
            // Appelle la même logique que UpdateUser, mais la garde IsAdmin (ici et dans le service)
            // garantit que seul un admin peut modifier le champ 'IsAdmin'.
            return await UpdateUser(id, dto);
        }

        // DELETE: api/admin/users/5
        [HttpDelete("api/admin/users/{id}")]
        [Authorize(Roles = "Admin")]
        public async Task<IActionResult> DeleteUser(int id)
        {
            var success = await _userService.DeleteUser(id);
            if (!success) return NotFound();
            return Ok(); // 200 OK
        }
    }
}
