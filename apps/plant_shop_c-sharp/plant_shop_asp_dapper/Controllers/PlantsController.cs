using Microsoft.AspNetCore.Mvc;
using plant_shop_asp_dapper.Repositories;
using plant_shop_asp_dapper.Models;

namespace plant_shop_asp_dapper.Controllers
{
    [ApiController]
    public class PlantsController : BaseController
    {
        private readonly PlantRepository _plantRepo;

        public PlantsController(PlantRepository plantRepo)
        {
            _plantRepo = plantRepo;
        }

        // GET: api/plants
        [HttpGet(Routes.PlantsList)]
        public async Task<ActionResult<IEnumerable<Plant>>> GetPlants()
        {
            // Liste publique (stock > 0)
            var plants = await _plantRepo.FindAllAsync(includeOutOfStock: false);
            return Ok(plants);
        }

        // GET: api/plants/5
        [HttpGet(Routes.PlantDetail)]
        public async Task<ActionResult<Plant>> GetPlant(int id)
        {
            var plant = await _plantRepo.FindByIdAsync(id);
            if (plant == null)
            {
                return NotFound();
            }
            return Ok(plant);
        }
    }
}
