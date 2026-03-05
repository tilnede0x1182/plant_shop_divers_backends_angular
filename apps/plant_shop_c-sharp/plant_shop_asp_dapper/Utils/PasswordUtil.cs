namespace plant_shop_asp_dapper.Utils
{
    /// <summary>
    /// Utilitaire de hachage BCrypt.
    /// </summary>    // (Identique au Projet 2)
    public static class PasswordUtil
    {
        /// <summary>
        /// Hache un mot de passe avec BCrypt.
        /// </summary>
        /// <param name="password">Mot de passe en clair.</param>
        /// <returns>Hash BCrypt.</returns>
        public static string HashPassword(string password)
        {
            return BCrypt.Net.BCrypt.HashPassword(password);
        }

        public static bool CheckPassword(string password, string hashedPassword)
        {
            try
            {
                return BCrypt.Net.BCrypt.Verify(password, hashedPassword);
            }
            catch
            {
                return false;
            }
        }
    }
}
