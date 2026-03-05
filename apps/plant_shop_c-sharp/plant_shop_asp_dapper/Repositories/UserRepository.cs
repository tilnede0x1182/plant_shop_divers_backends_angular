using Dapper;
using plant_shop_asp_dapper.Data;
using plant_shop_asp_dapper.Models;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace plant_shop_asp_dapper.Repositories
{
    /// <summary>
    /// Repository CRUD pour les utilisateurs.
    /// </summary>
    public class UserRepository : BaseRepository
    {
        /// <summary>
        /// Constructeur avec injection de la factory.
        /// </summary>
        /// <param name="factory">Factory de connexions.</param>
        public UserRepository(DbConnectionFactory factory) : base(factory) { }

        // SQL Mappage (snake_case -> PascalCase)
        private const string SelectSql = @"
            SELECT id AS Id,
                   name AS Name,
                   email AS Email,
                   password_hash AS PasswordHash,
                   is_admin AS IsAdmin,
                   created_at AS CreatedAt
            FROM users";

        /// <summary>
        /// Trouve un utilisateur par email.
        /// </summary>
        /// <param name="email">Email a rechercher.</param>
        /// <returns>Utilisateur ou null.</returns>
        public async Task<User?> FindByEmailAsync(string email)
        {
            using var connection = CreateConnection();
            return await connection.QuerySingleOrDefaultAsync<User>(
                $"{SelectSql} WHERE email = @Email", new { Email = email });
        }

        /// <summary>
        /// Trouve un utilisateur par ID.
        /// </summary>
        /// <param name="id">ID a rechercher.</param>
        /// <returns>Utilisateur ou null.</returns>
        public async Task<User?> FindByIdAsync(int id)
        {
            using var connection = CreateConnection();
            return await connection.QuerySingleOrDefaultAsync<User>(
                $"{SelectSql} WHERE id = @Id", new { Id = id });
        }

        /// <summary>
        /// Liste tous les utilisateurs.
        /// </summary>
        /// <returns>Liste des utilisateurs.</returns>
        public async Task<IEnumerable<User>> FindAllAsync()
        {
            using var connection = CreateConnection();
            // Exclure le hash des listes
            var sql = SelectSql.Replace("password_hash AS PasswordHash,", "NULL AS PasswordHash,");
            return await connection.QueryAsync<User>(
                $"{sql} ORDER BY is_admin DESC, name ASC");
        }

        /// <summary>
        /// Cree un nouvel utilisateur.
        /// </summary>
        /// <param name="user">Utilisateur a creer.</param>
        /// <returns>Utilisateur avec ID genere.</returns>
        public async Task<User> CreateAsync(User user)
        {
            using var connection = CreateConnection();
            var sql = @"
                INSERT INTO users (name, email, password_hash, is_admin, created_at)
                VALUES (@Name, @Email, @PasswordHash, @IsAdmin, @CreatedAt)
                RETURNING id";

            user.CreatedAt = DateTime.UtcNow;
            user.Id = await connection.ExecuteScalarAsync<int>(sql, user);
            return user;
        }

        /// <summary>
        /// Met a jour un utilisateur.
        /// </summary>
        /// <param name="user">Utilisateur modifie.</param>
        public async Task UpdateAsync(User user)
        {
            using var connection = CreateConnection();
            var sql = @"
                UPDATE users
                SET name = @Name,
                    email = @Email,
                    is_admin = @IsAdmin
                WHERE id = @Id";
            await connection.ExecuteAsync(sql, user);
        }

        /// <summary>
        /// Supprime un utilisateur.
        /// </summary>
        /// <param name="id">ID a supprimer.</param>
        public async Task DeleteAsync(int id)
        {
            using var connection = CreateConnection();
            await connection.ExecuteAsync("DELETE FROM users WHERE id = @Id", new { Id = id });
        }
    }
}
