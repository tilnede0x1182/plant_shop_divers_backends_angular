using Microsoft.AspNetCore.Mvc;
using plant_shop_asp_dapper.Repositories;
using plant_shop_asp_dapper.Models;

namespace plant_shop_asp_dapper.Controllers
{
    /// <summary>
    /// Controleur CRUD pour les plantes (routes standards).
    /// </summary>
    [ApiController]
    public class PlantsController : BaseController
    {
        private readonly PlantRepository _plantRepo;

        /// <summary>
        /// Constructeur avec injection du repository.
        /// </summary>
        /// <param name="plantRepo">Repository plantes.</param>
        public PlantsController(PlantRepository plantRepo)
        {
            _plantRepo = plantRepo;
        }

        /// <summary>
        /// Liste les plantes disponibles.
        /// </summary>
        /// <returns>Liste des plantes.</returns>
        [HttpGet(Routes.PlantsList)]
        public async Task<ActionResult<IEnumerable<Plant>>> GetPlants()
        {
            // Liste publique (stock > 0)
            var plants = await _plantRepo.FindAllAsync(includeOutOfStock: false);
            return Ok(plants);
        }

        /// <summary>
        /// Recupere une plante par ID.
        /// </summary>
        /// <param name="id">ID de la plante.</param>
        /// <returns>Plante ou NotFound.</returns>
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
