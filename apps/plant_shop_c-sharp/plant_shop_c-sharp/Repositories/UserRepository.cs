using Npgsql;
using plant_shop_c_sharp.Models;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace plant_shop_c_sharp.Repositories
{
    public class UserRepository : BaseRepository
    {
        public UserRepository(NpgsqlDataSource dataSource) : base(dataSource) { }

        public async Task<User?> FindByEmailAsync(string email)
        {
            await using var conn = GetConnection();
            await using var cmd = new NpgsqlCommand("SELECT * FROM users WHERE email = @email", conn);
            cmd.Parameters.AddWithValue("email", email);

            await using var reader = await cmd.ExecuteReaderAsync();
            if (await reader.ReadAsync())
            {
                return MapUser(reader);
            }
            return null;
        }

        public async Task<User?> FindByIdAsync(int id)
        {
            await using var conn = GetConnection();
            await using var cmd = new NpgsqlCommand("SELECT * FROM users WHERE id = @id", conn);
            cmd.Parameters.AddWithValue("id", id);

            await using var reader = await cmd.ExecuteReaderAsync();
            if (await reader.ReadAsync())
            {
                return MapUser(reader);
            }
            return null;
        }

        public async Task<List<User>> FindAllAsync()
        {
            var users = new List<User>();
            await using var conn = GetConnection();
            await using var cmd = new NpgsqlCommand("SELECT * FROM users ORDER BY is_admin DESC, name ASC", conn);

            await using var reader = await cmd.ExecuteReaderAsync();
            while (await reader.ReadAsync())
            {
                users.Add(MapUser(reader, includePassword: false)); // Ne pas exposer les hashs sur la liste
            }
            return users;
        }

        public async Task<User> CreateAsync(User user)
        {
            await using var conn = GetConnection();
            var sql = "INSERT INTO users (name, email, password_hash, is_admin, created_at) VALUES (@name, @email, @pass, @admin, @created) RETURNING id";
            await using var cmd = new NpgsqlCommand(sql, conn);

            var createdAt = DateTime.UtcNow;
            cmd.Parameters.AddWithValue("name", (object)user.Name ?? DBNull.Value);
            cmd.Parameters.AddWithValue("email", user.Email);
            cmd.Parameters.AddWithValue("pass", user.PasswordHash);
            cmd.Parameters.AddWithValue("admin", user.IsAdmin);
            cmd.Parameters.AddWithValue("created", createdAt);

            user.Id = (int)await cmd.ExecuteScalarAsync();
            user.CreatedAt = createdAt;
            return user;
        }

        public async Task UpdateAsync(User user)
        {
            await using var conn = GetConnection();
            // Ne met pas à jour le mot de passe
            var sql = "UPDATE users SET name = @name, email = @email, is_admin = @admin WHERE id = @id";
            await using var cmd = new NpgsqlCommand(sql, conn);

            cmd.Parameters.AddWithValue("id", user.Id);
            cmd.Parameters.AddWithValue("name", (object)user.Name ?? DBNull.Value);
            cmd.Parameters.AddWithValue("email", user.Email);
            cmd.Parameters.AddWithValue("admin", user.IsAdmin);

            await cmd.ExecuteNonQueryAsync();
        }

        public async Task DeleteAsync(int id)
        {
            await using var conn = GetConnection();
            await using var cmd = new NpgsqlCommand("DELETE FROM users WHERE id = @id", conn);
            cmd.Parameters.AddWithValue("id", id);
            await cmd.ExecuteNonQueryAsync();
        }

        private User MapUser(NpgsqlDataReader reader, bool includePassword = true)
        {
            return new User
            {
                Id = reader.GetInt32(reader.GetOrdinal("id")),
                Name = reader.IsDBNull(reader.GetOrdinal("name")) ? null : reader.GetString(reader.GetOrdinal("name")),
                Email = reader.GetString(reader.GetOrdinal("email")),
                PasswordHash = includePassword ? reader.GetString(reader.GetOrdinal("password_hash")) : null,
                IsAdmin = reader.GetBoolean(reader.GetOrdinal("is_admin")),
                CreatedAt = reader.GetDateTime(reader.GetOrdinal("created_at"))
            };
        }
    }
}
