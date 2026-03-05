using Npgsql;
using plant_shop_c_sharp.Models;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace plant_shop_c_sharp.Repositories
{
    /// <summary>
    /// Repository CRUD pour les commandes.
    /// </summary>
    public class OrderRepository : BaseRepository
    {
        /// <summary>
        /// Constructeur avec injection de la source de donnees.
        /// </summary>
        /// <param name="dataSource">Source Npgsql.</param>
        public OrderRepository(NpgsqlDataSource dataSource) : base(dataSource) { }

        /// <summary>
        /// Trouve une commande par ID.
        /// </summary>
        /// <param name="id">ID a rechercher.</param>
        /// <returns>Commande ou null.</returns>
        public async Task<Order?> FindByIdAsync(int id)
        {
            await using var conn = GetConnection();
            await using var cmd = new NpgsqlCommand("SELECT * FROM orders WHERE id = @id", conn);
            cmd.Parameters.AddWithValue("id", id);

            await using var reader = await cmd.ExecuteReaderAsync();
            if (await reader.ReadAsync())
            {
                return MapOrder(reader);
            }
            return null;
        }

        /// <summary>
        /// Liste les commandes d un utilisateur.
        /// </summary>
        /// <param name="userId">ID de l utilisateur.</param>
        /// <returns>Liste des commandes.</returns>
        public async Task<List<Order>> FindByUserIdAsync(int userId)
        {
            var orders = new List<Order>();
            await using var conn = GetConnection();
            // Trié par date de création (plus récent en premier)
            await using var cmd = new NpgsqlCommand("SELECT * FROM orders WHERE user_id = @userId ORDER BY created_at DESC", conn);
            cmd.Parameters.AddWithValue("userId", userId);

            await using var reader = await cmd.ExecuteReaderAsync();
            while (await reader.ReadAsync())
            {
                orders.Add(MapOrder(reader));
            }
            return orders;
        }

        /// <summary>
        /// Cree une nouvelle commande.
        /// </summary>
        /// <param name="order">Commande a creer.</param>
        /// <returns>Commande avec ID genere.</returns>
        public async Task<Order> CreateAsync(Order order)
        {
            await using var conn = GetConnection();
            var sql = "INSERT INTO orders (user_id, total, status, created_at) VALUES (@userId, @total, @status, @created) RETURNING id";
            await using var cmd = new NpgsqlCommand(sql, conn);

            cmd.Parameters.AddWithValue("userId", order.UserId);
            cmd.Parameters.AddWithValue("total", order.TotalPrice);
            cmd.Parameters.AddWithValue("status", order.Status);
            cmd.Parameters.AddWithValue("created", DateTime.UtcNow);

            order.Id = (int)await cmd.ExecuteScalarAsync();
            return order;
        }

        /// <summary>
        /// Met a jour une commande.
        /// </summary>
        /// <param name="order">Commande modifiee.</param>
        public async Task UpdateAsync(Order order)
        {
            await using var conn = GetConnection();
            var sql = "UPDATE orders SET total = @total, status = @status WHERE id = @id";
            await using var cmd = new NpgsqlCommand(sql, conn);

            cmd.Parameters.AddWithValue("id", order.Id);
            cmd.Parameters.AddWithValue("total", order.TotalPrice);
            cmd.Parameters.AddWithValue("status", order.Status);

            await cmd.ExecuteNonQueryAsync();
        }

        /// <summary>
        /// Supprime une commande.
        /// </summary>
        /// <param name="id">ID a supprimer.</param>
        public async Task DeleteAsync(int id)
        {
            await using var conn = GetConnection();
            // S'assurer que les items sont supprimés (CASCADE ou manuellement)
            // Supposons CASCADE
            await using var cmd = new NpgsqlCommand("DELETE FROM orders WHERE id = @id", conn);
            cmd.Parameters.AddWithValue("id", id);
            await cmd.ExecuteNonQueryAsync();
        }

        /// <summary>
        /// Mappe un reader vers une Order.
        /// </summary>
        /// <param name="reader">Reader Npgsql.</param>
        /// <returns>Entite Order.</returns>
        private Order MapOrder(NpgsqlDataReader reader)
        {
            return new Order
            {
                Id = reader.GetInt32(reader.GetOrdinal("id")),
                UserId = reader.GetInt32(reader.GetOrdinal("user_id")),
                TotalPrice = reader.GetDecimal(reader.GetOrdinal("total")),
                Status = reader.GetString(reader.GetOrdinal("status")),
                CreatedAt = reader.GetDateTime(reader.GetOrdinal("created_at"))
            };
        }
    }
}
