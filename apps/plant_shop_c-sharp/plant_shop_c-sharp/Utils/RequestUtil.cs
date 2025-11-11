using System.IdentityModel.Tokens.Jwt;
using System.Net;
using System.Text;
using Newtonsoft.Json;
using plant_shop_c_sharp.Models;
using plant_shop_c_sharp.Repositories;

namespace plant_shop_c_sharp.Utils
{
    public static class RequestUtil
    {
        public static T? ParseJsonBody<T>(HttpListenerRequest request) where T : class
        {
            if (!request.HasEntityBody)
            {
                return null;
            }
            using (var reader = new StreamReader(request.InputStream, request.ContentEncoding))
            {
                string body = reader.ReadToEnd();
                if (string.IsNullOrEmpty(body))
                {
                    return null;
                }
                try
                {
                    return JsonConvert.DeserializeObject<T>(body);
                }
                catch (JsonSerializationException)
                {
                    return null; // JSON mal formé
                }
            }
        }

        public static async Task<User?> GetUserFromAuth(HttpListenerRequest request, UserRepository userRepo, string authScheme = "Bearer ")
        {
            string? token = ExtractTokenFromAuthorization(request, authScheme)
                              ?? ExtractTokenFromCookies(request);

            if (string.IsNullOrEmpty(token))
            {
                return null;
            }

            var principal = JwtUtil.ValidateToken(token);
            if (principal == null)
            {
                return null;
            }

            var userIdClaim = principal.FindFirst(JwtRegisteredClaimNames.Sub);
            if (userIdClaim == null || !int.TryParse(userIdClaim.Value, out int userId))
            {
                return null;
            }

            // Récupérer l'utilisateur complet depuis la BDD pour s'assurer qu'il est à jour
            return await userRepo.FindByIdAsync(userId);
        }

        private static string? ExtractTokenFromAuthorization(HttpListenerRequest request, string authScheme)
        {
            string? authHeader = request.Headers["Authorization"];
            if (authHeader != null && authHeader.StartsWith(authScheme, StringComparison.OrdinalIgnoreCase))
            {
                return authHeader.Substring(authScheme.Length).Trim();
            }
            return null;
        }

        private static string? ExtractTokenFromCookies(HttpListenerRequest request)
        {
            // HttpListenerRequest.Cookies n'est pas toujours renseigné suivant l'hôte, on gère donc les deux cas
            if (request.Cookies["jwt"] != null)
            {
                string? value = request.Cookies["jwt"]!.Value;
                if (!string.IsNullOrWhiteSpace(value))
                {
                    return value;
                }
            }

            string? cookieHeader = request.Headers["Cookie"];
            if (string.IsNullOrEmpty(cookieHeader))
            {
                return null;
            }

            var cookies = cookieHeader.Split(';');
            foreach (var raw in cookies)
            {
                var kv = raw.Split('=', 2);
                if (kv.Length == 2 && kv[0].Trim() == "jwt")
                {
                    string token = kv[1].Trim();
                    if (!string.IsNullOrEmpty(token))
                    {
                        return token;
                    }
                }
            }
            return null;
        }
    }
}
