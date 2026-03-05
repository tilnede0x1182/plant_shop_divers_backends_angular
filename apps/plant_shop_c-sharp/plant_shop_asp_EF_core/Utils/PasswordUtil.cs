namespace plant_shop_asp_EF_core.Utils
{
    /// <summary>
    /// Utilitaire de hachage de mots de passe.
    /// </summary>
    public static class PasswordUtil
    {
        /// <summary>
        /// Hache un mot de passe avec BCrypt.
        /// </summary>
        /// <param name="password">Mot de passe en clair</param>
        /// <returns>Hash BCrypt</returns>
        public static string HashPassword(string password)
        {
            return BCrypt.Net.BCrypt.HashPassword(password);
        }

        /// <summary>
        /// Verifie un mot de passe contre un hash.
        /// </summary>
        /// <param name="password">Mot de passe en clair</param>
        /// <param name="hashedPassword">Hash BCrypt</param>
        /// <returns>true si correspondance</returns>
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
