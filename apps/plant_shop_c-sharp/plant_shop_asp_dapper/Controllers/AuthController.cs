using Microsoft.AspNetCore.Mvc;
using plant_shop_asp_dapper.Repositories;
using plant_shop_asp_dapper.Models;
using plant_shop_asp_dapper.Utils;
using Microsoft.AspNetCore.Authorization;
using System.Security.Claims;
using System.ComponentModel.DataAnnotations;

namespace plant_shop_asp_dapper.Controllers
{
    [ApiController]
    public class AuthController : BaseController
    {
        private readonly UserRepository _userRepo;
        private readonly JwtUtil _jwtUtil;

        public AuthController(UserRepository userRepo, JwtUtil jwtUtil)
        {
            _userRepo = userRepo;
            _jwtUtil = jwtUtil;
        }

        [HttpPost(Routes.AuthRegister)]
        [ProducesResponseType(StatusCodes.Status201Created)]
        [ProducesResponseType(StatusCodes.Status400BadRequest)]
        public async Task<IActionResult> Register([FromBody] RegisterDto model)
        {
            var existingUser = await _userRepo.FindByEmailAsync(model.Email);
            if (existingUser != null)
            {
                return BadRequest(new { message = "Email déjà utilisé" });
            }

            var user = new User
            {
                Email = model.Email,
                PasswordHash = PasswordUtil.HashPassword(model.Password),
                Name = model.Name,
                IsAdmin = false
            };

            var createdUser = await _userRepo.CreateAsync(user);

            // Connexion auto
            var token = _jwtUtil.GenerateToken(createdUser);
            AppendJwtCookie(token);

            createdUser.PasswordHash = ""; // Nettoyage
            return CreatedAtAction(nameof(Register), createdUser);
        }

        [HttpPost(Routes.AuthLogin)]
        [ProducesResponseType(StatusCodes.Status201Created)]
        [ProducesResponseType(StatusCodes.Status401Unauthorized)]
        public async Task<IActionResult> Login([FromBody] LoginDto model)
        {
            var user = await _userRepo.FindByEmailAsync(model.Email);
            if (user == null || !PasswordUtil.CheckPassword(model.Password, user.PasswordHash))
            {
                return Unauthorized(new { message = "Email ou mot de passe invalide" });
            }

            var token = _jwtUtil.GenerateToken(user);
            AppendJwtCookie(token);

            user.PasswordHash = ""; // Nettoyage
            return Created(nameof(Login), user); // 201 comme Nest/Test
        }

        [HttpPost(Routes.AuthLogout)]
        [ProducesResponseType(StatusCodes.Status200OK)]
        public IActionResult Logout()
        {
            Response.Cookies.Delete("jwt", new CookieOptions
            {
                HttpOnly = true,
                Secure = true,
                SameSite = SameSiteMode.Strict
            });
            return Ok(new { message = "Déconnecté" });
        }

        [HttpGet(Routes.AuthMe)]
        [Authorize]
        [ProducesResponseType(StatusCodes.Status200OK)]
        [ProducesResponseType(StatusCodes.Status401Unauthorized)]
        public async Task<IActionResult> Me()
        {
            var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);
            if (userId == null) return Unauthorized();

            var user = await _userRepo.FindByIdAsync(int.Parse(userId));
            if (user == null) return Unauthorized();

            user.PasswordHash = ""; // Nettoyage
            return Ok(user);
        }

        private void AppendJwtCookie(string token)
        {
             Response.Cookies.Append("jwt", token, new CookieOptions
            {
                HttpOnly = true,
                Secure = true, // false en dev
                SameSite = SameSiteMode.Strict,
                MaxAge = TimeSpan.FromHours(1)
            });
        }
    }

    // DTOs
    public class RegisterDto { [Required] public required string Email { get; set; } [Required] public required string Password { get; set; } public string? Name { get; set; } }
    public class LoginDto { [Required] public required string Email { get; set; } [Required] public required string Password { get; set; } }
}
