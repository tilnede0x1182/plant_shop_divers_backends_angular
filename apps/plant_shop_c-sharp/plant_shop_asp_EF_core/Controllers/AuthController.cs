using Microsoft.AspNetCore.Mvc;
using plant_shop_asp_EF_core.Services;
using plant_shop_asp_EF_core.Models;
using Microsoft.AspNetCore.Authorization;
using System.Security.Claims;

namespace plant_shop_asp_EF_core.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class AuthController : ControllerBase
    {
        private readonly AuthService _authService;
        private readonly UserService _userService;

        public AuthController(AuthService authService, UserService userService)
        {
            _authService = authService;
            _userService = userService;
        }

        [HttpPost("register")]
        [ProducesResponseType(StatusCodes.Status201Created)]
        [ProducesResponseType(StatusCodes.Status400BadRequest)]
        public async Task<IActionResult> Register([FromBody] RegisterDto model)
        {
            try
            {
                var user = await _authService.RegisterUser(model.Email, model.Password, model.Name);
                // Connexion automatique après l'inscription
                var token = _authService.Login(user);

                // Stockage du token dans un cookie httpOnly (compatible Angular/Nest)
                Response.Cookies.Append("jwt", token, new CookieOptions
                {
                    HttpOnly = true,
                    Secure = true, // Mettre à false en dev si pas de HTTPS
                    SameSite = SameSiteMode.Strict,
                    MaxAge = TimeSpan.FromHours(1)
                });

                return CreatedAtAction(nameof(Register), UserService.SanitizeUser(user));
            }
            catch (ApplicationException ex)
            {
                return BadRequest(new { message = ex.Message });
            }
        }

        [HttpPost("login")]
        [ProducesResponseType(StatusCodes.Status201Created)]
        [ProducesResponseType(StatusCodes.Status401Unauthorized)]
        public async Task<IActionResult> Login([FromBody] LoginDto model)
        {
            var user = await _authService.ValidateUser(model.Email, model.Password);
            if (user == null)
            {
                return Unauthorized(new { message = "Email ou mot de passe invalide" });
            }

            var token = _authService.Login(user);

            Response.Cookies.Append("jwt", token, new CookieOptions
            {
                HttpOnly = true,
                Secure = true,
                SameSite = SameSiteMode.Strict,
                MaxAge = TimeSpan.FromHours(1)
            });

            // Le test Java attend 201
            return CreatedAtAction(nameof(Login), UserService.SanitizeUser(user));
        }

        [HttpPost("logout")]
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

        [HttpGet("me")]
        [Authorize] // Nécessite un token valide
        [ProducesResponseType(StatusCodes.Status200OK)]
        [ProducesResponseType(StatusCodes.Status401Unauthorized)]
        public async Task<IActionResult> Me()
        {
            var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);
            if (userId == null)
            {
                return Unauthorized();
            }

            var user = await _userService.GetUserById(int.Parse(userId));
            if (user == null)
            {
                return Unauthorized(); // L'utilisateur du token n'existe plus
            }
            return Ok(user);
        }
    }

    // DTOs
    public class RegisterDto
    {
        [Required]
        [EmailAddress]
        public required string Email { get; set; }
        [Required]
        public required string Password { get; set; }
        public string? Name { get; set; }
    }

    public class LoginDto
    {
        [Required]
        [EmailAddress]
        public required string Email { get; set; }
        [Required]
        public required string Password { get; set; }
    }
}
