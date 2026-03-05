using Microsoft.AspNetCore.Mvc;
using plant_shop_asp_dapper.Repositories;
using plant_shop_asp_dapper.Models;
using plant_shop_asp_dapper.Utils;
using Microsoft.AspNetCore.Authorization;
using System.Security.Claims;
using System.ComponentModel.DataAnnotations;

namespace plant_shop_asp_dapper.Controllers
{
    /// <summary>
    /// Controleur d authentification (register, login, logout, me).
    /// </summary>
    [ApiController]
    public class AuthController : BaseController
    {
        private readonly UserRepository _userRepo;
        private readonly JwtUtil _jwtUtil;

        /// <summary>
        /// Constructeur du controleur d authentification.
        /// </summary>
        /// <param name="userRepo">Repository utilisateurs.</param>
        /// <param name="jwtUtil">Utilitaire JWT.</param>
        public AuthController(UserRepository userRepo, JwtUtil jwtUtil)
        {
            _userRepo = userRepo;
            _jwtUtil = jwtUtil;
        }

        [HttpPost(Routes.AuthRegister)]
        [ProducesResponseType(StatusCodes.Status201Created)]
        [ProducesResponseType(StatusCodes.Status400BadRequest)]
        /// <summary>
        /// Inscription d un nouvel utilisateur.
        /// </summary>
        /// <param name="model">Donnees d inscription.</param>
        /// <returns>Utilisateur cree.</returns>
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

            var dto = UserDtoMapper.ToDto(createdUser);
            return CreatedAtAction(nameof(Register), dto);
        }

        [HttpPost(Routes.AuthLogin)]
        [ProducesResponseType(StatusCodes.Status201Created)]
        [ProducesResponseType(StatusCodes.Status401Unauthorized)]
        /// <summary>
        /// Connexion utilisateur.
        /// </summary>
        /// <param name="model">Credentials.</param>
        /// <returns>Utilisateur connecte.</returns>
        public async Task<IActionResult> Login([FromBody] LoginDto model)
        {
            var user = await _userRepo.FindByEmailAsync(model.Email);
            if (user == null || !PasswordUtil.CheckPassword(model.Password, user.PasswordHash))
            {
                return Unauthorized(new { message = "Email ou mot de passe invalide" });
            }

            var token = _jwtUtil.GenerateToken(user);
            AppendJwtCookie(token);

            var dto = UserDtoMapper.ToDto(user);
            return Created(nameof(Login), dto); // 201 comme Nest/Test
        }

        [HttpPost(Routes.AuthLogout)]
        [ProducesResponseType(StatusCodes.Status200OK)]
        /// <summary>
        /// Deconnexion et suppression du cookie JWT.
        /// </summary>
        /// <returns>Message de confirmation.</returns>
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
        /// <summary>
        /// Retourne le profil de l utilisateur connecte.
        /// </summary>
        /// <returns>Profil utilisateur.</returns>
        public async Task<IActionResult> Me()
        {
            var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);
            if (userId == null) return Unauthorized();

            var user = await _userRepo.FindByIdAsync(int.Parse(userId));
            if (user == null) return Unauthorized();

            var dto = UserDtoMapper.ToDto(user);
            return Ok(dto);
        }

        /// <summary>
        /// Ajoute le cookie JWT a la reponse.
        /// </summary>
        /// <param name="token">Token JWT.</param>
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

    /// <summary>
    /// DTO pour l inscription.
    /// </summary>
    public class RegisterDto { [Required] public required string Email { get; set; } [Required] public required string Password { get; set; } public string? Name { get; set; } }
    /// <summary>
    /// DTO pour la connexion.
    /// </summary>
    public class LoginDto { [Required] public required string Email { get; set; } [Required] public required string Password { get; set; } }
}
