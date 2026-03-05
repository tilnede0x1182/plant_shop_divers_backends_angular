using Npgsql;
using plant_shop_c_sharp.Models;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace plant_shop_c_sharp.Repositories
{
    /// <summary>
    /// Repository CRUD pour les items de commande.
    /// </summary>
    public class OrderItemRepository : BaseRepository
    {
        /// <summary>
        /// Constructeur avec injection de la source de donnees.
        /// </summary>
        /// <param name="dataSource">Source Npgsql.</param>
        public OrderItemRepository(NpgsqlDataSource dataSource) : base(dataSource) { }

        /// <summary>
        /// Trouve un item par ID.
        /// </summary>
        /// <param name="id">ID a rechercher.</param>
        /// <returns>OrderItem ou null.</returns>
        public async Task<OrderItem?> FindByIdAsync(int id)
        {
            await using var conn = GetConnection();
            await using var cmd = new NpgsqlCommand("SELECT * FROM order_items WHERE id = @id", conn);
            cmd.Parameters.AddWithValue("id", id);

            await using var reader = await cmd.ExecuteReaderAsync();
            if (await reader.ReadAsync())
            {
                return MapOrderItem(reader);
            }
            return null;
        }

        /// <summary>
        /// Liste les items d une commande.
        /// </summary>
        /// <param name="orderId">ID de la commande.</param>
        /// <returns>Liste des items.</returns>
        public async Task<List<OrderItem>> FindByOrderIdAsync(int orderId)
        {
            var items = new List<OrderItem>();
            await using var conn = GetConnection();
            await using var cmd = new NpgsqlCommand("SELECT * FROM order_items WHERE order_id = @orderId", conn);
            cmd.Parameters.AddWithValue("orderId", orderId);

            await using var reader = await cmd.ExecuteReaderAsync();
            while (await reader.ReadAsync())
            {
                items.Add(MapOrderItem(reader));
            }
            return items;
        }

        /// <summary>
        /// Cree un nouvel item.
        /// </summary>
        /// <param name="item">Item a creer.</param>
        /// <returns>Item avec ID genere.</returns>
        public async Task<OrderItem> CreateAsync(OrderItem item)
        {
            await using var conn = GetConnection();
            var sql = "INSERT INTO order_items (order_id, plant_id, quantity, price) VALUES (@orderId, @plantId, @qty, @price) RETURNING id";
            await using var cmd = new NpgsqlCommand(sql, conn);

            cmd.Parameters.AddWithValue("orderId", item.OrderId);
            cmd.Parameters.AddWithValue("plantId", item.PlantId.HasValue ? item.PlantId.Value : DBNull.Value);
            cmd.Parameters.AddWithValue("qty", item.Quantity);
            cmd.Parameters.AddWithValue("price", item.Price);

            item.Id = (int)await cmd.ExecuteScalarAsync();
            return item;
        }

        /// <summary>
        /// Supprime tous les items d une commande.
        /// </summary>
        /// <param name="orderId">ID de la commande.</param>
        public async Task DeleteByOrderIdAsync(int orderId)
        {
            await using var conn = GetConnection();
            await using var cmd = new NpgsqlCommand("DELETE FROM order_items WHERE order_id = @orderId", conn);
            cmd.Parameters.AddWithValue("orderId", orderId);
            await cmd.ExecuteNonQueryAsync();
        }


        /// <summary>
        /// Mappe un reader vers un OrderItem.
        /// </summary>
        /// <param name="reader">Reader Npgsql.</param>
        /// <returns>Entite OrderItem.</returns>
        private OrderItem MapOrderItem(NpgsqlDataReader reader)
        {
            return new OrderItem
            {
                Id = reader.GetInt32(reader.GetOrdinal("id")),
                OrderId = reader.GetInt32(reader.GetOrdinal("order_id")),
                PlantId = reader.IsDBNull(reader.GetOrdinal("plant_id")) ? null : reader.GetInt32(reader.GetOrdinal("plant_id")),
                Quantity = reader.GetInt32(reader.GetOrdinal("quantity")),
                Price = reader.GetDecimal(reader.GetOrdinal("price"))
            };
        }
    }
}
