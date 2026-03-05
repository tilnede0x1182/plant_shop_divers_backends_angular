using System.Data;
using plant_shop_asp_dapper.Data;

namespace plant_shop_asp_dapper.Repositories
{
    /// <summary>
    /// Repository de base avec Dapper.
    /// </summary>
    public abstract class BaseRepository
    {
        protected readonly DbConnectionFactory _factory;

        /// <summary>
        /// Constructeur avec injection de la factory.
        /// </summary>
        /// <param name="factory">Factory de connexions.</param>
        protected BaseRepository(DbConnectionFactory factory)
        {
            _factory = factory;
        }

        // Helper pour ouvrir une connexion
        protected IDbConnection CreateConnection()
        {
            return _factory.CreateConnection();
        }
    }
}
