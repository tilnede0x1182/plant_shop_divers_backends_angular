using Npgsql;

namespace plant_shop_c_sharp.Repositories
{
    /// <summary>
    /// Repository de base avec source de donnees Npgsql.
    /// </summary>
    public abstract class BaseRepository
    {
        protected readonly NpgsqlDataSource DataSource;

        /// <summary>
        /// Constructeur avec injection de la source de donnees.
        /// </summary>
        /// <param name="dataSource">Source Npgsql.</param>
        protected BaseRepository(NpgsqlDataSource dataSource)
        {
            DataSource = dataSource;
        }

        /// <summary>
        /// Fournit une connexion geree par le pool.
        /// </summary>
        /// <returns>Connexion Npgsql.</returns>
        protected NpgsqlConnection GetConnection() => DataSource.OpenConnection();
    }
}
