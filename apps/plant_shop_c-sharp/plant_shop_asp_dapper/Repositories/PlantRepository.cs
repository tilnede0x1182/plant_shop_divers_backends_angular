using Dapper;
using plant_shop_asp_dapper.Data;
using plant_shop_asp_dapper.Models;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace plant_shop_asp_dapper.Repositories
{
    public class PlantRepository : BaseRepository
    {
        public PlantRepository(DbConnectionFactory factory) : base(factory) { }

        private const string SelectSql = @"
            SELECT id AS Id,
                   name AS Name,
                   description AS Description,
                   price AS Price,
                   stock AS Stock,
                   created_at AS CreatedAt
            FROM plants";

        public async Task<Plant?> FindByIdAsync(int id)
        {
            using var connection = CreateConnection();
            return await connection.QuerySingleOrDefaultAsync<Plant>(
                $"{SelectSql} WHERE id = @Id", new { Id = id });
        }

        public async Task<IEnumerable<Plant>> FindAllAsync(bool includeOutOfStock = false)
        {
            using var connection = CreateConnection();
            var sql = includeOutOfStock
                ? $"{SelectSql} ORDER BY name ASC"
                : $"{SelectSql} WHERE stock > 0 ORDER BY name ASC";

            return await connection.QueryAsync<Plant>(sql);
        }

        public async Task<Plant> CreateAsync(Plant plant)
        {
            using var connection = CreateConnection();
            var sql = @"
                INSERT INTO plants (name, description, price, stock, created_at)
                VALUES (@Name, @Description, @Price, @Stock, @CreatedAt)
                RETURNING id";

            plant.CreatedAt = DateTime.UtcNow;
            plant.Id = await connection.ExecuteScalarAsync<int>(sql, plant);
            return plant;
        }

        public async Task UpdateAsync(Plant plant)
        {
            using var connection = CreateConnection();
            var sql = @"
                UPDATE plants
                SET name = @Name,
                    description = @Description,
                    price = @Price,
                    stock = @Stock
                WHERE id = @Id";
            await connection.ExecuteAsync(sql, plant);
        }

        public async Task<int> UpdateStockAsync(int id, int newStock)
        {
             using var connection = CreateConnection();
             return await connection.ExecuteAsync(
                "UPDATE plants SET stock = @Stock WHERE id = @Id",
                new { Id = id, Stock = newStock });
        }


        public async Task DeleteAsync(int id)
        {
            using var connection = CreateConnection();
            await connection.ExecuteAsync("DELETE FROM plants WHERE id = @Id", new { Id = id });
        }
    }
}
