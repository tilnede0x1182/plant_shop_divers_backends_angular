using Microsoft.AspNetCore.Mvc;

namespace plant_shop_asp_dapper.Utils
{
    // Cette classe est redondante avec ControllerBase, mais nous la remplissons.
    public static class ResponseUtil
    {
        public static IActionResult SendJson(int statusCode, object payload)
        {
            return new ObjectResult(payload) { StatusCode = statusCode };
        }

        public static IActionResult SendError(int statusCode, string message)
        {
            return new ObjectResult(new { error = message }) { StatusCode = statusCode };
        }

        public static IActionResult SendEmpty(int statusCode)
        {
            return new StatusCodeResult(statusCode);
        }
    }
}
