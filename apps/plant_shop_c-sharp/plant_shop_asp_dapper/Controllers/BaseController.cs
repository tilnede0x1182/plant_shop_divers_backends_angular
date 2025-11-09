using Microsoft.AspNetCore.Mvc;

namespace plant_shop_asp_dapper.Controllers
{
    // Ce BaseController est implicitement [ApiController]
    public abstract class BaseController : ControllerBase
    {
        // Les helpers (SendError, etc.) sont déjà fournis par ControllerBase
        // (par ex: Ok(), NotFound(), BadRequest(), Forbid())
    }
}
