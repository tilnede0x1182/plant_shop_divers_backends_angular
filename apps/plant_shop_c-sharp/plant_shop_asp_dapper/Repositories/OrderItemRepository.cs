using Dapper;
using plant_shop_asp_dapper.Data;
using plant_shop_asp_dapper.Models;
using System.Collections.Generic;
using System.Data;
using System.Threading.Tasks;

namespace plant_shop_asp_dapper.Repositories
{
    public class OrderItemRepository : BaseRepository
    {
        public OrderItemRepository(DbConnectionFactory factory) : base(factory) { }

        private const string SelectSql = @"
            SELECT id AS Id,
                   order_id AS OrderId,
                   plant_id AS PlantId,
                   quantity AS Quantity,
                   price AS Price
            FROM order_items";

        public async Task<OrderItem?> FindByIdAsync(int id)
        {
            using var connection = CreateConnection();
            return await connection.QuerySingleOrDefaultAsync<OrderItem>(
                $"{SelectSql} WHERE id = @Id", new { Id = id });
        }

        public async Task<IEnumerable<OrderItem>> FindByOrderIdAsync(int orderId)
        {
            using var connection = CreateConnection();

            // Jointure pour récupérer les infos de la plante (comme dans le test)
            var sql = @"
                SELECT
                    oi.id AS Id,
                    oi.order_id AS OrderId,
                    oi.plant_id AS PlantId,
                    oi.quantity AS Quantity,
                    oi.price AS Price,
                    p.id AS Id,
                    p.name AS Name,
                    p.description AS Description,
                    p.price AS Price,
                    p.stock AS Stock,
                    p.created_at AS CreatedAt
                FROM order_items oi
                JOIN plants p ON oi.plant_id = p.id
                WHERE oi.order_id = @OrderId";

            var items = await connection.QueryAsync<OrderItem, Plant, OrderItem>(
                sql,
                (orderItem, plant) =>
                {
                    orderItem.Plant = plant;
                    return orderItem;
                },
                new { OrderId = orderId },
                splitOn: "Id" // Dapper split sur la colonne "Id" (la 2ème)
            );
            return items;
        }

        public async Task<OrderItem> CreateAsync(OrderItem item, IDbConnection? connection = null, IDbTransaction? transaction = null)
        {
            var ownsConnection = connection == null;
            connection ??= CreateConnection();

            var sql = @"
                INSERT INTO order_items (order_id, plant_id, quantity, price)
                VALUES (@OrderId, @PlantId, @Quantity, @Price)
                RETURNING id";

            try
            {
                item.Id = await connection.ExecuteScalarAsync<int>(sql, item, transaction);
            }
            finally
            {
                if (ownsConnection)
                {
                    connection.Dispose();
                }
            }

            return item;
        }

        public async Task DeleteByOrderIdAsync(int orderId)
        {
            using var connection = CreateConnection();
            await connection.ExecuteAsync(
                "DELETE FROM order_items WHERE order_id = @OrderId",
                new { OrderId = orderId });
        }
    }
}
