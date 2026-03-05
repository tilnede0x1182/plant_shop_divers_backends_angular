using System.Security.Claims;
using plant_shop_asp_dapper.Repositories;

namespace plant_shop_asp_dapper.Utils
{
    /// <summary>
    /// Utilitaire de parsing des requetes.
    /// </summary>    // Ce fichier est moins utile dans ASP.NET Core car le framework gère le parsing
    // et l'authentification, mais nous le remplissons pour la complétude du squelette.
    public static class RequestUtil
    {
        public static async Task<Models.User?> GetUserFromClaims(ClaimsPrincipal userPrincipal, UserRepository userRepo)
        {
            var userIdClaim = userPrincipal.FindFirstValue(ClaimTypes.NameIdentifier);
            if (userIdClaim == null || !int.TryParse(userIdClaim, out int userId))
            {
                return null;
            }
            return await userRepo.FindByIdAsync(userId);
        }
    }
}
