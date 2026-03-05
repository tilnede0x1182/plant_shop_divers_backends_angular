using Npgsql;
using System;
using System.Data;

namespace plant_shop_asp_dapper.Data
{
    /// <summary>
    /// Factory de connexions PostgreSQL.
    /// </summary>    // Gère la création de connexions DB
    public class DbConnectionFactory
    {
        private readonly string _connectionString;

        /// <summary>
        /// Constructeur avec chaine de connexion.
        /// </summary>
        /// <param name="connectionString">Chaine de connexion PostgreSQL.</param>
        public DbConnectionFactory(string connectionString)
        {
            _connectionString = connectionString ?? throw new ArgumentNullException(nameof(connectionString));
        }

        public IDbConnection CreateConnection()
        {
            return new NpgsqlConnection(_connectionString);
        }
    }
}
