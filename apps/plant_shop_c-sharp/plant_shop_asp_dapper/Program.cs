using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using plant_shop_asp_dapper.Data;
using plant_shop_asp_dapper.Repositories;
using plant_shop_asp_dapper.Utils;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using Npgsql;

const int ServerPort = 4100;

var builder = WebApplication.CreateBuilder(args);
builder.Logging.ClearProviders();
builder.Logging.AddConsole();

// 1. Configuration des services
var configuration = builder.Configuration;
var envConfig = LoadEnv(builder.Environment.ContentRootPath);
var connectionString = ResolveConnectionString(configuration, envConfig);

// Ajout de la factory de connexion (Singleton)
builder.Services.AddSingleton(new DbConnectionFactory(connectionString));

// Ajout des Repositories (Scoped)
builder.Services.AddScoped<UserRepository>();
builder.Services.AddScoped<PlantRepository>();
builder.Services.AddScoped<OrderRepository>();
builder.Services.AddScoped<OrderItemRepository>();

// Ajout des Utils
builder.Services.AddSingleton<JwtUtil>();

// Configuration du contrôleur
builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        // Ignorer les cycles de référence
        options.JsonSerializerOptions.ReferenceHandler = ReferenceHandler.IgnoreCycles;
        options.JsonSerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;
        options.JsonSerializerOptions.DictionaryKeyPolicy = JsonNamingPolicy.CamelCase;
    });

// 2. Configuration de l'authentification JWT
var jwtKey = Encoding.ASCII.GetBytes(configuration["Jwt:Key"] ?? throw new InvalidOperationException("Jwt:Key non configurée"));

builder.Services.AddAuthentication(options =>
{
    options.DefaultAuthenticateScheme = JwtBearerDefaults.AuthenticationScheme;
    options.DefaultChallengeScheme = JwtBearerDefaults.AuthenticationScheme;
})
.AddJwtBearer(options =>
{
    options.RequireHttpsMetadata = false; // true en prod
    options.SaveToken = true;
    options.TokenValidationParameters = new TokenValidationParameters
    {
        ValidateIssuerSigningKey = true,
        IssuerSigningKey = new SymmetricSecurityKey(jwtKey),
        ValidateIssuer = true,
        ValidIssuer = configuration["Jwt:Issuer"],
        ValidateAudience = true,
        ValidAudience = configuration["Jwt:Audience"],
        ValidateLifetime = true,
        ClockSkew = TimeSpan.Zero
    };

    // Lire le token depuis le cookie httpOnly
    options.Events = new JwtBearerEvents
    {
        OnMessageReceived = context =>
        {
            context.Token = context.Request.Cookies["jwt"];
            return Task.CompletedTask;
        }
    };
});

// 3. Configuration de Swagger (OpenAPI)
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new OpenApiInfo { Title = "Plant Shop API (Dapper)", Version = "v1" });
    c.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Description = "JWT Authorization header using the Bearer scheme (ou via le cookie httpOnly 'jwt')",
        Name = "Authorization",
        In = ParameterLocation.Header,
        Type = SecuritySchemeType.Http,
        Scheme = "bearer"
    });
    c.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        {
            new OpenApiSecurityScheme
            {
                Reference = new OpenApiReference { Type = ReferenceType.SecurityScheme, Id = "Bearer" }
            },
            Array.Empty<string>()
        }
    });
});

// 4. Configuration CORS (pour le frontend Angular)
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAngularApp", policy =>
    {
        policy.WithOrigins("http://localhost:4200") // Port de 'serve-ssr' Angular
              .AllowAnyHeader()
              .AllowAnyMethod()
              .AllowCredentials();
    });
});

builder.WebHost.ConfigureKestrel(options =>
{
    options.ListenLocalhost(ServerPort);
});

var app = builder.Build();

// 5. Pipeline Middleware
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseCors("AllowAngularApp");

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

app.Lifetime.ApplicationStarted.Register(() =>
{
    Console.WriteLine($"🚀 Serveur démarré sur http://localhost:{ServerPort}");
    Console.WriteLine($"   Routes API disponibles sur http://localhost:{ServerPort}/api");
});

app.Lifetime.ApplicationStopped.Register(() =>
{
    Console.WriteLine("Serveur ASP.NET Core arrêté.");
});

try
{
    app.Run();
}
catch (IOException ex) when (IsPortInUse(ex))
{
    Console.Error.WriteLine($"❌ Erreur : Le port {ServerPort} est déjà utilisé. Un autre serveur est peut-être en cours d'exécution.");
    Environment.ExitCode = 1;
}
catch (Exception ex)
{
    Console.Error.WriteLine($"❌ Erreur lors du démarrage du serveur : {ex.Message}");
    Environment.ExitCode = 2;
}

Dictionary<string, string> LoadEnv(string rootPath)
{
    var map = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
    var envPath = Path.Combine(rootPath, ".env");
    if (!File.Exists(envPath))
    {
        Console.WriteLine("⚠️  Fichier .env non trouvé. Utilisation des valeurs par défaut.");
        return map;
    }

    try
    {
        foreach (var line in File.ReadAllLines(envPath))
        {
            if (string.IsNullOrWhiteSpace(line) || line.TrimStart().StartsWith("#"))
                continue;
            var idx = line.IndexOf('=');
            if (idx <= 0) continue;
            var key = line[..idx].Trim();
            var value = line[(idx + 1)..].Trim();
            map[key] = value;
        }
    }
    catch (IOException ex)
    {
        Console.WriteLine($"⚠️  Lecture du .env impossible: {ex.Message}");
    }

    return map;
}

string ResolveConnectionString(IConfiguration config, IDictionary<string, string> env)
{
    if (env.TryGetValue("DATABASE_URL", out var rawUrl) && !string.IsNullOrWhiteSpace(rawUrl))
    {
        return NormalizeConnectionString(rawUrl, env);
    }

    return config.GetConnectionString("DefaultConnection") ?? "Host=localhost;Database=plant_shop_c-sharp";
}

string NormalizeConnectionString(string rawUrl, IDictionary<string, string> env)
{
    string normalized = rawUrl.Trim();

    if (normalized.StartsWith("jdbc:", StringComparison.OrdinalIgnoreCase))
    {
        normalized = normalized[5..];
    }

    if (normalized.StartsWith("postgresql://", StringComparison.OrdinalIgnoreCase) ||
        normalized.StartsWith("postgres://", StringComparison.OrdinalIgnoreCase))
    {
        var uri = new Uri(normalized);
        var builder = new NpgsqlConnectionStringBuilder
        {
            Host = uri.Host,
            Port = uri.IsDefaultPort ? 5432 : uri.Port,
            Database = uri.AbsolutePath.Trim('/').Length == 0 ? "plant_shop_c-sharp" : uri.AbsolutePath.Trim('/')
        };

        if (!string.IsNullOrEmpty(uri.UserInfo))
        {
            var parts = uri.UserInfo.Split(':', 2);
            builder.Username = parts[0];
            if (parts.Length > 1)
            {
                builder.Password = parts[1];
            }
        }

        if (env.TryGetValue("DATABASE_USER", out var envUser) && !string.IsNullOrWhiteSpace(envUser))
        {
            builder.Username = envUser;
        }

        if (env.TryGetValue("DATABASE_PASS", out var envPass) && !string.IsNullOrWhiteSpace(envPass))
        {
            builder.Password = envPass;
        }

        return builder.ConnectionString;
    }
    else
    {
        var builder = new NpgsqlConnectionStringBuilder(normalized);

        if (env.TryGetValue("DATABASE_USER", out var envUser) && !string.IsNullOrWhiteSpace(envUser))
        {
            builder.Username = envUser;
        }

        if (env.TryGetValue("DATABASE_PASS", out var envPass) && !string.IsNullOrWhiteSpace(envPass))
        {
            builder.Password = envPass;
        }

        if (string.IsNullOrWhiteSpace(builder.Database))
        {
            builder.Database = "plant_shop_c-sharp";
        }

        return builder.ConnectionString;
    }
}

bool IsPortInUse(IOException ex)
{
    if (ex.InnerException is Microsoft.AspNetCore.Connections.AddressInUseException)
    {
        return true;
    }

    if (ex.InnerException is System.Net.Sockets.SocketException socketEx &&
        socketEx.SocketErrorCode == System.Net.Sockets.SocketError.AddressAlreadyInUse)
    {
        return true;
    }

    return ex.Message.Contains("address already in use", StringComparison.OrdinalIgnoreCase);
}
