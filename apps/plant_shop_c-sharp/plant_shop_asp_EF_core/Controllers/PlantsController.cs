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

        public PlantsController(AppDbContext context)
        {
            _context = context;
        }

        // GET: api/plants
        [HttpGet("api/plants")]
        public async Task<ActionResult<IEnumerable<Plant>>> GetPlants()
        {
            // Trié par nom, stocks > 0 (comme Angular)
            return await _context.Plants
                .Where(p => p.Stock > 0)
                .OrderBy(p => p.Name)
                .ToListAsync();
        }

        // GET: api/plants/5
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

        // --- Routes Admin ---

        // GET: api/admin/plants
        [HttpGet("api/admin/plants")]
        [Authorize(Roles = "Admin")]
        public async Task<ActionResult<IEnumerable<Plant>>> GetAdminPlants()
        {
            // La liste admin voit tout, trié par nom
            return await _context.Plants
                .OrderBy(p => p.Name)
                .ToListAsync();
        }

        // POST: api/admin/plants
        [HttpPost("api/admin/plants")]
        [Authorize(Roles = "Admin")]
        public async Task<ActionResult<Plant>> PostPlant(Plant plant)
        {
            _context.Plants.Add(plant);
            await _context.SaveChangesAsync();

            return CreatedAtAction(nameof(GetPlant), new { id = plant.Id }, plant);
        }

        // PATCH: api/admin/plants/5
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

        // DELETE: api/admin/plants/5
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

    // DTO pour PATCH
    public class PlantUpdateDto
    {
        public string? Name { get; set; }
        public string? Description { get; set; }
        public decimal? Price { get; set; }
        public int? Stock { get; set; }
    }
}
