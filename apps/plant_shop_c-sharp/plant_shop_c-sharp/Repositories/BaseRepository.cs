using Npgsql;

namespace plant_shop_c_sharp.Repositories
{
    public abstract class BaseRepository
    {
        protected readonly NpgsqlDataSource DataSource;

        protected BaseRepository(NpgsqlDataSource dataSource)
        {
            DataSource = dataSource;
        }

        // Fournit une connexion gérée par le pool
        protected NpgsqlConnection GetConnection() => DataSource.OpenConnection();
    }
}
