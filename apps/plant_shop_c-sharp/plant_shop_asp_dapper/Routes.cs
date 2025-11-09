namespace plant_shop_asp_dapper
{
    // Classe statique pour les constantes de route (pour la cohérence)
    public static class Routes
    {
        // Public
        public const string PlantsList = "api/plants";
        public const string PlantDetail = "api/plants/{id}";

        // Auth
        public const string AuthRegister = "api/auth/register";
        public const string AuthLogin = "api/auth/login";
        public const string AuthLogout = "api/auth/logout";
        public const string AuthMe = "api/auth/me";

        // User (self)
        public const string UserDetail = "api/users/{id}";
        public const string UserUpdate = "api/users/{id}";

        // Orders (self)
        public const string OrdersList = "api/orders";
        public const string OrderCreate = "api/orders";
        public const string OrderDetail = "api/orders/{id}";

        // Admin Plants
        public const string AdminPlantsList = "api/admin/plants";
        public const string AdminPlantCreate = "api/admin/plants";
        public const string AdminPlantUpdate = "api/admin/plants/{id}";
        public const string AdminPlantDelete = "api/admin/plants/{id}";

        // Admin Users
        public const string AdminUsersList = "api/admin/users";
        public const string AdminUserDetail = "api/admin/users/{id}";
        public const string AdminUserUpdate = "api/admin/users/{id}";
        public const string AdminUserDelete = "api/admin/users/{id}";

        // Admin Orders (Squelette Dapper a OrderController vs OrdersController)
        public const string AdminOrderUpdate = "api/admin/orders/{id}";
        public const string AdminOrderDelete = "api/admin/orders/{id}";
    }
}
