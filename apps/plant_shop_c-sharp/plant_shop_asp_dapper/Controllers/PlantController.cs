using Microsoft.AspNetCore.Mvc;
using plant_shop_asp_dapper.Repositories;
using plant_shop_asp_dapper.Models;
using Microsoft.AspNetCore.Authorization;

namespace plant_shop_asp_dapper.Controllers
{
    [ApiController]
    [Authorize(Roles = "Admin")] // Routes admin sécurisées
    public class PlantController : BaseController
    {
        private readonly PlantRepository _plantRepo;

        public PlantController(PlantRepository plantRepo)
        {
            _plantRepo = plantRepo;
        }

        // GET: api/admin/plants
        [HttpGet(Routes.AdminPlantsList)]
        public async Task<ActionResult<IEnumerable<Plant>>> GetAdminPlants()
        {
            // Liste admin (tout voir)
            var plants = await _plantRepo.FindAllAsync(includeOutOfStock: true);
            return Ok(plants);
        }

        // POST: api/admin/plants
        [HttpPost(Routes.AdminPlantCreate)]
        public async Task<ActionResult<Plant>> PostPlant(Plant plant)
        {
            var createdPlant = await _plantRepo.CreateAsync(plant);
            return CreatedAtAction(nameof(PlantsController.GetPlant), "Plants", new { id = createdPlant.Id }, createdPlant);
        }

        // PATCH: api/admin/plants/5
        [HttpPatch(Routes.AdminPlantUpdate)]
        public async Task<IActionResult> PatchPlant(int id, [FromBody] PlantUpdateDto dto)
        {
            var plant = await _plantRepo.FindByIdAsync(id);
            if (plant == null)
            {
                return NotFound();
            }

            // Mise à jour partielle
            plant.Name = dto.Name ?? plant.Name;
            plant.Description = dto.Description ?? plant.Description;
            plant.Price = dto.Price ?? plant.Price;
            plant.Stock = dto.Stock ?? plant.Stock;

            await _plantRepo.UpdateAsync(plant);
            return Ok(plant);
        }

        // DELETE: api/admin/plants/5
        [HttpDelete(Routes.AdminPlantDelete)]
        public async Task<IActionResult> DeletePlant(int id)
        {
            var plant = await _plantRepo.FindByIdAsync(id);
            if (plant == null)
            {
                return NotFound();
            }
            await _plantRepo.DeleteAsync(id);
            return Ok(); // 200 OK (Test Java)
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
