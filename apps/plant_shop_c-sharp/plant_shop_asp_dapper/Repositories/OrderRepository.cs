using Dapper;
using plant_shop_asp_dapper.Data;
using plant_shop_asp_dapper.Models;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace plant_shop_asp_dapper.Repositories
{
    public class OrderRepository : BaseRepository
    {
        public OrderRepository(DbConnectionFactory factory) : base(factory) { }

        private const string SelectSql = @"
            SELECT id AS Id,
                   user_id AS UserId,
                   total_price AS TotalPrice,
                   status AS Status,
                   created_at AS CreatedAt
            FROM orders";

        public async Task<Order?> FindByIdAsync(int id)
        {
            using var connection = CreateConnection();
            return await connection.QuerySingleOrDefaultAsync<Order>(
                $"{SelectSql} WHERE id = @Id", new { Id = id });
        }

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

        public async Task<Order> CreateAsync(Order order)
        {
            using var connection = CreateConnection();
            var sql = @"
                INSERT INTO orders (user_id, total_price, status, created_at)
                VALUES (@UserId, @TotalPrice, @Status, @CreatedAt)
                RETURNING id";

            order.CreatedAt = DateTime.UtcNow;
            order.Id = await connection.ExecuteScalarAsync<int>(sql, order);
            return order;
        }

        public async Task UpdateAsync(Order order)
        {
            using var connection = CreateConnection();
            var sql = @"
                UPDATE orders
                SET total_price = @TotalPrice,
                    status = @Status
                WHERE id = @Id";
            await connection.ExecuteAsync(sql, order);
        }

        public async Task DeleteAsync(int id)
        {
            using var connection = CreateConnection();
            // Supposons CASCADE DELETE sur order_items
            await connection.ExecuteAsync("DELETE FROM orders WHERE id = @Id", new { Id = id });
        }
    }
}
