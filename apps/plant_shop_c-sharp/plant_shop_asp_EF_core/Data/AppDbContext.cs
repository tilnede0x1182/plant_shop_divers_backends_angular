using Microsoft.EntityFrameworkCore;
using plant_shop_asp_EF_core.Models;

namespace plant_shop_asp_EF_core.Data
{
    /// <summary>
    /// Contexte de base de donnees Entity Framework.
    /// </summary>
    public class AppDbContext : DbContext
    {
        /// <summary>
        /// Constructeur du contexte.
        /// </summary>
        /// <param name="options">Options de configuration</param>
        public AppDbContext(DbContextOptions<AppDbContext> options) : base(options)
        {
        }

        public DbSet<User> Users { get; set; }
        public DbSet<Plant> Plants { get; set; }
        public DbSet<Order> Orders { get; set; }
        public DbSet<OrderItem> OrderItems { get; set; }

        /// <summary>
        /// Configure le modele de donnees.
        /// </summary>
        /// <param name="modelBuilder">Builder du modele</param>
        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            base.OnModelCreating(modelBuilder);

            // Configuration des index (bonnes pratiques)
            modelBuilder.Entity<User>()
                .HasIndex(u => u.Email)
                .IsUnique();

            modelBuilder.Entity<Plant>()
                .HasIndex(p => p.Name);

            // Configuration des relations
            modelBuilder.Entity<Order>()
                .HasOne(o => o.User)
                .WithMany(u => u.Orders)
                .HasForeignKey(o => o.UserId);

            modelBuilder.Entity<OrderItem>()
                .HasOne(oi => oi.Order)
                .WithMany(o => o.OrderItems)
                .HasForeignKey(oi => oi.OrderId);

            modelBuilder.Entity<OrderItem>()
                .HasOne(oi => oi.Plant)
                .WithMany() // Pas de collection de OrderItems dans Plant
                .HasForeignKey(oi => oi.PlantId);
        }
    }
}
