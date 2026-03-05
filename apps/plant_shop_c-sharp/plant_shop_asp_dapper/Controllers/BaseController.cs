using Microsoft.AspNetCore.Mvc;

namespace plant_shop_asp_dapper.Controllers
{
    /// <summary>
    /// Controleur de base avec helpers communs.
    /// </summary>    // Ce BaseController est implicitement [ApiController]
    public abstract class BaseController : ControllerBase
    {
        // Les helpers (SendError, etc.) sont déjà fournis par ControllerBase
        // (par ex: Ok(), NotFound(), BadRequest(), Forbid())
    }
}
