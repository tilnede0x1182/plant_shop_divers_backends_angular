public record SessionContext(boolean authenticated, int userId, boolean admin) {
    public static SessionContext anonymous() {
        return new SessionContext(false, -1, false);
    }
}
