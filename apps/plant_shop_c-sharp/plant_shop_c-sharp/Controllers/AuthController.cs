using Npgsql;
using plant_shop_c_sharp.DTOs;
using plant_shop_c_sharp.Models;
using plant_shop_c_sharp.Utils;
using System.Net;
using Newtonsoft.Json;

namespace plant_shop_c_sharp.Controllers
{
    public class AuthController : BaseController
    {
        public AuthController(NpgsqlDataSource dataSource) : base(dataSource) { }

        public override async Task HandleRequest(HttpListenerContext context, User? currentUser)
        {
            var request = context.Request;
            var response = context.Response;
            var path = request.Url?.AbsolutePath;
            var method = request.HttpMethod;

            // Simple routage basé sur le chemin exact
            if (method == "POST" && path == "/api/auth/register")
            {
                await Register(context);
            }
            else if (method == "POST" && path == "/api/auth/login")
            {
                await Login(context);
            }
            else if (method == "POST" && path == "/api/auth/logout")
            {
                await Logout(context);
            }
            else if (method == "GET" && path == "/api/auth/me")
            {
                await Me(context, currentUser);
            }
            else
            {
                await SendError(response, 404, "Route non trouvée");
            }
        }

        private async Task Register(HttpListenerContext context)
        {
            var body = ParseBody<RegisterRequest>(context.Request);
            if (body == null || string.IsNullOrEmpty(body.Email) || string.IsNullOrEmpty(body.Password))
            {
                await SendError(context.Response, 400, "Email et mot de passe requis");
                return;
            }

            var existingUser = await UserRepo.FindByEmailAsync(body.Email);
            if (existingUser != null)
            {
                await SendError(context.Response, 400, "Cet email est déjà utilisé");
                return;
            }

            var user = new User
            {
                Name = body.Name,
                Email = body.Email,
                PasswordHash = PasswordUtil.HashPassword(body.Password),
                IsAdmin = false // L'inscription ne crée jamais d'admin
            };

            var createdUser = await UserRepo.CreateAsync(user);
            await SendJsonResponse(context.Response, 201, createdUser);
        }

        private async Task Login(HttpListenerContext context)
        {
            var body = ParseBody<LoginRequest>(context.Request);
            if (body == null || string.IsNullOrEmpty(body.Email) || string.IsNullOrEmpty(body.Password))
            {
                await SendError(context.Response, 400, "Email et mot de passe requis");
                return;
            }

            var user = await UserRepo.FindByEmailAsync(body.Email);
            if (user == null || user.PasswordHash == null || !PasswordUtil.CheckPassword(body.Password, user.PasswordHash))
            {
                await SendError(context.Response, 401, "Email ou mot de passe invalide");
                return;
            }

            string token = JwtUtil.GenerateToken(user);

            // Cookie httpOnly pour reproduire le backend Java/Nest
            var cookie = new Cookie("jwt", token)
            {
                HttpOnly = true,
                Secure = false,
                Path = "/"
            };
            context.Response.Cookies.Add(cookie);

            await SendJsonResponse(context.Response, 201, new { token, user });
        }

        private async Task Logout(HttpListenerContext context)
        {
            var cookie = new Cookie("jwt", "")
            {
                HttpOnly = true,
                Secure = false,
                Path = "/",
                Expires = DateTime.UtcNow.AddDays(-1)
            };
            context.Response.Cookies.Add(cookie);
            await SendJsonResponse(context.Response, 200, new { message = "Déconnecté" });
        }

        private async Task Me(HttpListenerContext context, User? currentUser)
        {
            if (currentUser == null)
            {
                await SendError(context.Response, 401, "Non authentifié");
                return;
            }
            await SendJsonResponse(context.Response, 200, currentUser);
        }

    }
}
