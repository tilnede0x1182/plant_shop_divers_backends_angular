using Npgsql;
using plant_shop_c_sharp.DTOs;
using plant_shop_c_sharp.Models;
using plant_shop_c_sharp.Utils;
using System.Net;
using System.Linq;

namespace plant_shop_c_sharp.Controllers
{
    /// <summary>
    /// Controleur CRUD pour les utilisateurs.
    /// </summary>
    public class UserController : BaseController
    {
        /// <summary>
        /// Constructeur avec injection de la source de donnees.
        /// </summary>
        /// <param name="dataSource">Source Npgsql.</param>
        public UserController(NpgsqlDataSource dataSource) : base(dataSource) { }

        /// <summary>
        /// Gere les requetes HTTP pour les routes users.
        /// </summary>
        /// <param name="context">Contexte HTTP.</param>
        /// <param name="currentUser">Utilisateur connecte ou null.</param>
        public override async Task HandleRequest(HttpListenerContext context, User? currentUser)
        {
            var request = context.Request;
            var response = context.Response;
            var path = request.Url?.AbsolutePath ?? "";
            var method = request.HttpMethod;

            // Routage: /api/users et /api/admin/users
            var segments = path.Split('/', StringSplitOptions.RemoveEmptyEntries);
            bool isAdminRoute = segments.Length > 1 && segments[1] == "admin";

            int id = -1;
            if (segments.Length == 3 && segments[1] == "users") // GET, PATCH /api/users/1
            {
                int.TryParse(segments[2], out id);
            }
            else if (segments.Length == 4 && isAdminRoute) // GET, PATCH, DELETE /api/admin/users/1
            {
                int.TryParse(segments[3], out id);
            }

            try
            {
                if (currentUser == null)
                {
                    await SendError(response, 401, "Authentification requise");
                    return;
                }

                if (isAdminRoute && !currentUser.IsAdmin)
                {
                     await SendError(response, 403, "Accès admin requis");
                     return;
                }

                if (method == "GET")
                {
                    if (id != -1) // GET /api/users/:id OR /api/admin/users/:id
                    {
                        await GetUser(response, currentUser, id);
                    }
                    else if (currentUser.IsAdmin)
                    {
                        await GetAllUsers(response);
                    }
                    else
                    {
                        await SendError(response, 403, "L'accès à la liste /api/users est interdit");
                    }
                }
                else if (method == "POST" && id == -1)
                {
                    if (!currentUser.IsAdmin)
                    {
                        await SendError(response, 403, "Accès admin requis");
                        return;
                    }
                    await CreateUser(request, response);
                }
                else if (method == "PATCH" && id != -1) // PATCH /api/users/:id OR /api/admin/users/:id
                {
                    await UpdateUser(request, response, currentUser, id, isAdminRoute);
                }
                else if (method == "DELETE" && id != -1)
                {
                    if (!currentUser.IsAdmin)
                    {
                        await SendError(response, 403, "Accès admin requis");
                        return;
                    }
                    await DeleteUser(response, id);
                }
                else
                {
                    await SendError(response, 404, "Route non trouvée");
                }
            }
            catch (Exception ex)
            {
                await SendError(response, 500, $"Erreur interne: {ex.Message}");
            }
        }

        /// <summary>
        /// Liste tous les utilisateurs (admin).
        /// </summary>
        /// <param name="response">Reponse HTTP.</param>
        private async Task GetAllUsers(HttpListenerResponse response)
        {
            var users = await UserRepo.FindAllAsync();
            var dto = users.Select(UserDtoMapper.ToDto).ToList();
            await SendJsonResponse(response, 200, dto);
        }

        /// <summary>
        /// Recupere un utilisateur par ID.
        /// </summary>
        /// <param name="response">Reponse HTTP.</param>
        /// <param name="currentUser">Utilisateur connecte.</param>
        /// <param name="id">ID de l utilisateur.</param>
        private async Task GetUser(HttpListenerResponse response, User currentUser, int id)
        {
            if (currentUser.Id != id && !currentUser.IsAdmin)
            {
                await SendError(response, 403, "Accès refusé");
                return;
            }

            var user = await UserRepo.FindByIdAsync(id);
            if (user == null)
            {
                await SendError(response, 404, "Utilisateur non trouvé");
                return;
            }
            await SendJsonResponse(response, 200, UserDtoMapper.ToDto(user));
        }

        /// <summary>
        /// Cree un nouvel utilisateur (admin).
        /// </summary>
        /// <param name="request">Requete HTTP.</param>
        /// <param name="response">Reponse HTTP.</param>
        private async Task CreateUser(HttpListenerRequest request, HttpListenerResponse response)
        {
            var body = ParseBody<UserCreateRequest>(request);
            if (body == null || string.IsNullOrWhiteSpace(body.Email) || string.IsNullOrWhiteSpace(body.Password))
            {
                await SendError(response, 400, "Nom, email et mot de passe sont requis");
                return;
            }

            var existing = await UserRepo.FindByEmailAsync(body.Email);
            if (existing != null)
            {
                await SendError(response, 400, "Cet email existe déjà");
                return;
            }

            var user = new User
            {
                Name = string.IsNullOrWhiteSpace(body.Name) ? "Utilisateur" : body.Name,
                Email = body.Email,
                PasswordHash = PasswordUtil.HashPassword(body.Password),
                IsAdmin = body.IsAdmin
            };

            var created = await UserRepo.CreateAsync(user);
            await SendJsonResponse(response, 201, UserDtoMapper.ToDto(created));
        }

        /// <summary>
        /// Met a jour un utilisateur.
        /// </summary>
        /// <param name="request">Requete HTTP.</param>
        /// <param name="response">Reponse HTTP.</param>
        /// <param name="currentUser">Utilisateur connecte.</param>
        /// <param name="id">ID de l utilisateur.</param>
        /// <param name="isAdminCall">True si route admin.</param>
        private async Task UpdateUser(HttpListenerRequest request, HttpListenerResponse response, User currentUser, int id, bool isAdminCall)
        {
            if (currentUser.Id != id && !currentUser.IsAdmin)
            {
                await SendError(response, 403, "Accès refusé");
                return;
            }

            var user = await UserRepo.FindByIdAsync(id);
            if (user == null)
            {
                await SendError(response, 404, "Utilisateur non trouvé");
                return;
            }

            var body = ParseBody<UserUpdateRequest>(request);
            if (body == null)
            {
                await SendError(response, 400, "Corps de requête invalide");
                return;
            }

            user.Name = body.Name ?? user.Name;
            user.Email = body.Email ?? user.Email;

            // Un admin peut changer le statut admin, peu importe la route utilisée
            if (currentUser.IsAdmin && body.IsAdmin.HasValue)
            {
                user.IsAdmin = body.IsAdmin.Value;
            }

            await UserRepo.UpdateAsync(user);
            await SendJsonResponse(response, 200, UserDtoMapper.ToDto(user));
        }

        /// <summary>
        /// Supprime un utilisateur (admin).
        /// </summary>
        /// <param name="response">Reponse HTTP.</param>
        /// <param name="id">ID de l utilisateur.</param>
        private async Task DeleteUser(HttpListenerResponse response, int id)
        {
            await UserRepo.DeleteAsync(id);
            SendEmptyResponse(response, 200);
        }

    }
}
