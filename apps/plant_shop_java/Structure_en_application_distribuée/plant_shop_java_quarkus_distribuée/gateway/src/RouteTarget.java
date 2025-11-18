public record RouteTarget(String service, String path) {
    public static RouteTarget resolve(String path) {
        if (path.startsWith("/api/auth")) {
            return new RouteTarget("auth", path);
        }
        if (path.startsWith("/api/plants") || path.startsWith("/api/admin/plants")) {
            return new RouteTarget("catalog", path);
        }
        if (path.startsWith("/api/orders") || path.startsWith("/api/admin/orders")) {
            return new RouteTarget("order", path);
        }
        if (path.startsWith("/api/users") || path.startsWith("/api/admin/users")) {
            return new RouteTarget("user", path);
        }
        return null;
    }
}
