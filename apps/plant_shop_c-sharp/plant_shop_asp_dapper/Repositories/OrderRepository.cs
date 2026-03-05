using Dapper;
using plant_shop_asp_dapper.Data;
using plant_shop_asp_dapper.Models;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace plant_shop_asp_dapper.Repositories
{
    /// <summary>
    /// Repository CRUD pour les commandes.
    /// </summary>
    public class OrderRepository : BaseRepository
    {
        /// <summary>
        /// Constructeur avec injection de la factory.
        /// </summary>
        /// <param name="factory">Factory de connexions.</param>
        public OrderRepository(DbConnectionFactory factory) : base(factory) { }

        private const string SelectSql = @"
            SELECT id AS Id,
                   user_id AS UserId,
                   total AS TotalPrice,
                   status AS Status,
                   created_at AS CreatedAt
            FROM orders";

        /// <summary>
        /// Trouve une commande par ID.
        /// </summary>
        /// <param name="id">ID a rechercher.</param>
        /// <returns>Commande ou null.</returns>
        public async Task<Order?> FindByIdAsync(int id)
        {
            using var connection = CreateConnection();
            return await connection.QuerySingleOrDefaultAsync<Order>(
                $"{SelectSql} WHERE id = @Id", new { Id = id });
        }

        /// <summary>
        /// Liste les commandes d un utilisateur.
        /// </summary>
        /// <param name="userId">ID de l utilisateur.</param>
        /// <returns>Liste des commandes.</returns>
        public async Task<IEnumerable<Order>> FindByUserIdAsync(int userId)
        {
            using var connection = CreateConnection();
            return await connection.QueryAsync<Order>(
                $"{SelectSql} WHERE user_id = @UserId ORDER BY created_at DESC",
                new { UserId = userId });
        }

        public async Task<IEnumerable<Order>> FindAllAsync()
        {
            using var connection = CreateConnection();
            return await connection.QueryAsync<Order>(
                $"{SelectSql} ORDER BY created_at DESC");
        }

        /// <summary>
        /// Cree une nouvelle commande.
        /// </summary>
        /// <param name="order">Commande a creer.</param>
        /// <returns>Commande avec ID genere.</returns>
        public async Task<Order> CreateAsync(Order order)
        {
            using var connection = CreateConnection();
            var sql = @"
                INSERT INTO orders (user_id, total, status, created_at)
                VALUES (@UserId, @TotalPrice, @Status, @CreatedAt)
                RETURNING id";

            order.CreatedAt = DateTime.UtcNow;
            order.Id = await connection.ExecuteScalarAsync<int>(sql, order);
            return order;
        }

        /// <summary>
        /// Met a jour une commande.
        /// </summary>
        /// <param name="order">Commande modifiee.</param>
        public async Task UpdateAsync(Order order)
        {
            using var connection = CreateConnection();
            var sql = @"
                UPDATE orders
                SET total = @TotalPrice,
                    status = @Status
                WHERE id = @Id";
            await connection.ExecuteAsync(sql, order);
        }

        /// <summary>
        /// Supprime une commande.
        /// </summary>
        /// <param name="id">ID a supprimer.</param>
        public async Task DeleteAsync(int id)
        {
            using var connection = CreateConnection();
            // Supposons CASCADE DELETE sur order_items
            await connection.ExecuteAsync("DELETE FROM orders WHERE id = @Id", new { Id = id });
        }
    }
}
