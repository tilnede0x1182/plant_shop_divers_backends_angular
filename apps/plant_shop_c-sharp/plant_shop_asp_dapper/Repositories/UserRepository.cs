using Dapper;
using plant_shop_asp_dapper.Data;
using plant_shop_asp_dapper.Models;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace plant_shop_asp_dapper.Repositories
{
    public class UserRepository : BaseRepository
    {
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

        public async Task<User?> FindByEmailAsync(string email)
        {
            using var connection = CreateConnection();
            return await connection.QuerySingleOrDefaultAsync<User>(
                $"{SelectSql} WHERE email = @Email", new { Email = email });
        }

        public async Task<User?> FindByIdAsync(int id)
        {
            using var connection = CreateConnection();
            return await connection.QuerySingleOrDefaultAsync<User>(
                $"{SelectSql} WHERE id = @Id", new { Id = id });
        }

        public async Task<IEnumerable<User>> FindAllAsync()
        {
            using var connection = CreateConnection();
            // Exclure le hash des listes
            var sql = SelectSql.Replace("password_hash AS PasswordHash,", "NULL AS PasswordHash,");
            return await connection.QueryAsync<User>(
                $"{sql} ORDER BY is_admin DESC, name ASC");
        }

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

        public async Task DeleteAsync(int id)
        {
            using var connection = CreateConnection();
            await connection.ExecuteAsync("DELETE FROM users WHERE id = @Id", new { Id = id });
        }
    }
}
