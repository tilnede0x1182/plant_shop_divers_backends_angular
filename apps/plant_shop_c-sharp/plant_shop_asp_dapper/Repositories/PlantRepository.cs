using Dapper;
using plant_shop_asp_dapper.Data;
using plant_shop_asp_dapper.Models;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace plant_shop_asp_dapper.Repositories
{
    /// <summary>
    /// Repository CRUD pour les plantes.
    /// </summary>
    public class PlantRepository : BaseRepository
    {
        /// <summary>
        /// Constructeur avec injection de la factory.
        /// </summary>
        /// <param name="factory">Factory de connexions.</param>
        public PlantRepository(DbConnectionFactory factory) : base(factory) { }

        private const string SelectSql = @"
            SELECT id AS Id,
                   name AS Name,
                   description AS Description,
                   price AS Price,
                   stock AS Stock,
                   created_at AS CreatedAt
            FROM plants";

        /// <summary>
        /// Trouve une plante par ID.
        /// </summary>
        /// <param name="id">ID a rechercher.</param>
        /// <returns>Plante ou null.</returns>
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

        /// <summary>
        /// Cree une nouvelle plante.
        /// </summary>
        /// <param name="plant">Plante a creer.</param>
        /// <returns>Plante avec ID genere.</returns>
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

        /// <summary>
        /// Met a jour une plante.
        /// </summary>
        /// <param name="plant">Plante modifiee.</param>
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


        /// <summary>
        /// Supprime une plante.
        /// </summary>
        /// <param name="id">ID a supprimer.</param>
        public async Task DeleteAsync(int id)
        {
            using var connection = CreateConnection();
            await connection.ExecuteAsync("DELETE FROM plants WHERE id = @Id", new { Id = id });
        }
    }
}
