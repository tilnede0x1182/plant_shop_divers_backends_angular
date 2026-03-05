using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using plant_shop_asp_EF_core.Data;
using plant_shop_asp_EF_core.Models;
using Microsoft.AspNetCore.Authorization;

namespace plant_shop_asp_EF_core.Controllers
{
    [ApiController]
    public class PlantsController : ControllerBase
    {
        private readonly AppDbContext _context;

        /// <summary>
        /// Constructeur du controleur des plantes.
        /// </summary>
        /// <param name="context">Contexte de base de donnees</param>
        public PlantsController(AppDbContext context)
        {
            _context = context;
        }

        /// <summary>
        /// Liste toutes les plantes en stock.
        /// </summary>
        /// <returns>Liste des plantes</returns>
        [HttpGet("api/plants")]
        public async Task<ActionResult<IEnumerable<Plant>>> GetPlants()
        {
            // Trié par nom, stocks > 0 (comme Angular)
            return await _context.Plants
                .Where(p => p.Stock > 0)
                .OrderBy(p => p.Name)
                .ToListAsync();
        }

        /// <summary>
        /// Recupere une plante par son ID.
        /// </summary>
        /// <param name="id">Identifiant de la plante</param>
        /// <returns>Plante trouvee</returns>
        [HttpGet("api/plants/{id}")]
        public async Task<ActionResult<Plant>> GetPlant(int id)
        {
            var plant = await _context.Plants.FindAsync(id);

            if (plant == null)
            {
                return NotFound();
            }

            return plant;
        }

        /// <summary>
        /// Liste toutes les plantes (admin).
        /// </summary>
        /// <returns>Liste complete des plantes</returns>
        [HttpGet("api/admin/plants")]
        [Authorize(Roles = "Admin")]
        public async Task<ActionResult<IEnumerable<Plant>>> GetAdminPlants()
        {
            // La liste admin voit tout, trié par nom
            return await _context.Plants
                .OrderBy(p => p.Name)
                .ToListAsync();
        }

        /// <summary>
        /// Cree une nouvelle plante (admin).
        /// </summary>
        /// <param name="plant">Donnees de la plante</param>
        /// <returns>Plante creee</returns>
        [HttpPost("api/admin/plants")]
        [Authorize(Roles = "Admin")]
        public async Task<ActionResult<Plant>> PostPlant(Plant plant)
        {
            _context.Plants.Add(plant);
            await _context.SaveChangesAsync();

            return CreatedAtAction(nameof(GetPlant), new { id = plant.Id }, plant);
        }

        /// <summary>
        /// Met a jour une plante (admin).
        /// </summary>
        /// <param name="id">Identifiant de la plante</param>
        /// <param name="dto">Donnees de mise a jour</param>
        /// <returns>Plante mise a jour</returns>
        [HttpPatch("api/admin/plants/{id}")]
        [Authorize(Roles = "Admin")]
        public async Task<IActionResult> PatchPlant(int id, [FromBody] PlantUpdateDto dto)
        {
            var plant = await _context.Plants.FindAsync(id);
            if (plant == null)
            {
                return NotFound();
            }

            // Mise à jour partielle
            if (dto.Name != null) plant.Name = dto.Name;
            if (dto.Description != null) plant.Description = dto.Description;
            if (dto.Price.HasValue) plant.Price = dto.Price.Value;
            if (dto.Stock.HasValue) plant.Stock = dto.Stock.Value;

            try
            {
                await _context.SaveChangesAsync();
            }
            catch (DbUpdateConcurrencyException)
            {
                if (!_context.Plants.Any(e => e.Id == id))
                {
                    return NotFound();
                }
                else
                {
                    throw;
                }
            }

            return Ok(plant);
        }

        /// <summary>
        /// Supprime une plante (admin).
        /// </summary>
        /// <param name="id">Identifiant de la plante</param>
        /// <returns>OK si supprime</returns>
        [HttpDelete("api/admin/plants/{id}")]
        [Authorize(Roles = "Admin")]
        public async Task<IActionResult> DeletePlant(int id)
        {
            var plant = await _context.Plants.FindAsync(id);
            if (plant == null)
            {
                return NotFound();
            }

            _context.Plants.Remove(plant);
            await _context.SaveChangesAsync();

            return Ok(); // 200 OK (comme le test Java)
        }
    }

    /// <summary>
    /// DTO pour la mise a jour partielle d une plante.
    /// </summary>
    public class PlantUpdateDto
    {
        public string? Name { get; set; }
        public string? Description { get; set; }
        public decimal? Price { get; set; }
        public int? Stock { get; set; }
    }
}
