using Microsoft.AspNetCore.Mvc;
using plant_shop_asp_dapper.Repositories;
using plant_shop_asp_dapper.Models;
using plant_shop_asp_dapper.Utils;
using Microsoft.AspNetCore.Authorization;
using System.Collections.Generic;
using System.Linq;

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

        // GET: api/users
        [HttpGet(Routes.UsersList)]
        public async Task<ActionResult<IEnumerable<UserResponseDto>>> GetUsers()
        {
            if (!User.IsInRole("Admin"))
            {
                return StatusCode(StatusCodes.Status403Forbidden, new { error = "L'accès à la liste /api/users est interdit" });
            }

            var users = await _userRepo.FindAllAsync();
            return Ok(users.Select(UserDtoMapper.ToDto));
        }

        // GET: api/admin/users
        [HttpGet(Routes.AdminUsersList)]
        public async Task<ActionResult<IEnumerable<UserResponseDto>>> GetAllUsers()
        {
            var users = await _userRepo.FindAllAsync();
            return Ok(users.Select(UserDtoMapper.ToDto));
        }

        // POST: api/users
        [HttpPost(Routes.UsersList)]
        public async Task<ActionResult<UserResponseDto>> CreateUser([FromBody] UserCreateDto dto)
        {
            if (!User.IsInRole("Admin"))
            {
                return Forbid();
            }

            var existing = await _userRepo.FindByEmailAsync(dto.Email);
            if (existing != null)
            {
                return BadRequest(new { error = "Cet email existe déjà" });
            }

            var user = new User
            {
                Name = string.IsNullOrWhiteSpace(dto.Name) ? "Utilisateur" : dto.Name,
                Email = dto.Email,
                PasswordHash = PasswordUtil.HashPassword(dto.Password),
                IsAdmin = dto.IsAdmin
            };

            var created = await _userRepo.CreateAsync(user);
            return Created($"/api/users/{created.Id}", UserDtoMapper.ToDto(created));
        }

        // POST: api/admin/users (alias)
        [HttpPost(Routes.AdminUsersList)]
        public Task<ActionResult<UserResponseDto>> CreateAdminUser([FromBody] UserCreateDto dto) => CreateUser(dto);

        // GET: api/admin/users/5
        [HttpGet(Routes.AdminUserDetail)]
        public async Task<ActionResult<UserResponseDto>> GetAdminUser(int id)
        {
            var user = await _userRepo.FindByIdAsync(id);
            if (user == null) return NotFound();
            return Ok(UserDtoMapper.ToDto(user));
        }

        // PATCH: api/admin/users/5
        [HttpPatch(Routes.AdminUserUpdate)]
        public async Task<ActionResult<UserResponseDto>> UpdateAdminUser(int id, [FromBody] UserUpdateDto dto)
        {
            var user = await _userRepo.FindByIdAsync(id);
            if (user == null) return NotFound();

            // L'admin peut tout mettre à jour
            user.Name = dto.Name ?? user.Name;
            user.Email = dto.Email ?? user.Email;
            user.IsAdmin = dto.IsAdmin ?? user.IsAdmin;

            await _userRepo.UpdateAsync(user);
            return Ok(UserDtoMapper.ToDto(user));
        }

        // DELETE: api/admin/users/5
        [HttpDelete(Routes.AdminUserDelete)]
        public async Task<IActionResult> DeleteUser(int id)
        {
            await _userRepo.DeleteAsync(id);
            return Ok(); // 200 OK
        }

    }
}
