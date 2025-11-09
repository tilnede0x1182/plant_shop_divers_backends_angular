using System.Data;
using plant_shop_asp_dapper.Data;

namespace plant_shop_asp_dapper.Repositories
{
    public abstract class BaseRepository
    {
        protected readonly DbConnectionFactory _factory;

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
