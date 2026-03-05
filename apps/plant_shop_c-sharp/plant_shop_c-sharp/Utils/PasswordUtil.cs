namespace plant_shop_c_sharp.Utils
{
    /// <summary>
    /// Utilitaire de hachage BCrypt.
    /// </summary>
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

        /// <summary>
        /// Verifie un mot de passe contre son hash.
        /// </summary>
        /// <param name="password">Mot de passe en clair.</param>
        /// <param name="hashedPassword">Hash BCrypt.</param>
        /// <returns>True si valide.</returns>
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
