using Npgsql;
using plant_shop_c_sharp.Models;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace plant_shop_c_sharp.Repositories
{
    /// <summary>
    /// Repository CRUD pour les plantes.
    /// </summary>
    public class PlantRepository : BaseRepository
    {
        /// <summary>
        /// Constructeur avec injection de la source de donnees.
        /// </summary>
        /// <param name="dataSource">Source Npgsql.</param>
        public PlantRepository(NpgsqlDataSource dataSource) : base(dataSource) { }

        /// <summary>
        /// Trouve une plante par ID.
        /// </summary>
        /// <param name="id">ID a rechercher.</param>
        /// <returns>Plante ou null.</returns>
        public async Task<Plant?> FindByIdAsync(int id)
        {
            await using var conn = GetConnection();
            await using var cmd = new NpgsqlCommand("SELECT * FROM plants WHERE id = @id", conn);
            cmd.Parameters.AddWithValue("id", id);

            await using var reader = await cmd.ExecuteReaderAsync();
            if (await reader.ReadAsync())
            {
                return MapPlant(reader);
            }
            return null;
        }

        /// <summary>
        /// Liste toutes les plantes.
        /// </summary>
        /// <returns>Liste des plantes.</returns>
        public async Task<List<Plant>> FindAllAsync()
        {
            var plants = new List<Plant>();
            await using var conn = GetConnection();
            // Trié par nom par défaut pour la liste publique
            await using var cmd = new NpgsqlCommand("SELECT * FROM plants ORDER BY name ASC", conn);

            await using var reader = await cmd.ExecuteReaderAsync();
            while (await reader.ReadAsync())
            {
                plants.Add(MapPlant(reader));
            }
            return plants;
        }

        /// <summary>
        /// Cree une nouvelle plante.
        /// </summary>
        /// <param name="plant">Plante a creer.</param>
        /// <returns>Plante avec ID genere.</returns>
        public async Task<Plant> CreateAsync(Plant plant)
        {
            await using var conn = GetConnection();
            var sql = "INSERT INTO plants (name, description, price, stock, created_at) VALUES (@name, @desc, @price, @stock, @created) RETURNING id";
            await using var cmd = new NpgsqlCommand(sql, conn);

            cmd.Parameters.AddWithValue("name", plant.Name);
            cmd.Parameters.AddWithValue("desc", (object)plant.Description ?? DBNull.Value);
            cmd.Parameters.AddWithValue("price", plant.Price);
            cmd.Parameters.AddWithValue("stock", plant.Stock);
            cmd.Parameters.AddWithValue("created", DateTime.UtcNow);

            plant.Id = (int)await cmd.ExecuteScalarAsync();
            return plant;
        }

        /// <summary>
        /// Met a jour une plante.
        /// </summary>
        /// <param name="plant">Plante modifiee.</param>
        public async Task UpdateAsync(Plant plant)
        {
            await using var conn = GetConnection();
            var sql = "UPDATE plants SET name = @name, description = @desc, price = @price, stock = @stock WHERE id = @id";
            await using var cmd = new NpgsqlCommand(sql, conn);

            cmd.Parameters.AddWithValue("id", plant.Id);
            cmd.Parameters.AddWithValue("name", plant.Name);
            cmd.Parameters.AddWithValue("desc", (object)plant.Description ?? DBNull.Value);
            cmd.Parameters.AddWithValue("price", plant.Price);
            cmd.Parameters.AddWithValue("stock", plant.Stock);

            await cmd.ExecuteNonQueryAsync();
        }

        /// <summary>
        /// Supprime une plante.
        /// </summary>
        /// <param name="id">ID a supprimer.</param>
        public async Task DeleteAsync(int id)
        {
            await using var conn = GetConnection();
            await using var cmd = new NpgsqlCommand("DELETE FROM plants WHERE id = @id", conn);
            cmd.Parameters.AddWithValue("id", id);
            await cmd.ExecuteNonQueryAsync();
        }

        /// <summary>
        /// Mappe un reader vers une Plant.
        /// </summary>
        /// <param name="reader">Reader Npgsql.</param>
        /// <returns>Entite Plant.</returns>
        private Plant MapPlant(NpgsqlDataReader reader)
        {
            return new Plant
            {
                Id = reader.GetInt32(reader.GetOrdinal("id")),
                Name = reader.GetString(reader.GetOrdinal("name")),
                Description = reader.IsDBNull(reader.GetOrdinal("description")) ? null : reader.GetString(reader.GetOrdinal("description")),
                Price = reader.GetDecimal(reader.GetOrdinal("price")),
                Stock = reader.GetInt32(reader.GetOrdinal("stock")),
                CreatedAt = reader.GetDateTime(reader.GetOrdinal("created_at"))
            };
        }
    }
}
