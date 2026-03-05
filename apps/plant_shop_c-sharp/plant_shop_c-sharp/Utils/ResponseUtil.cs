using System.Net;
using System.Text;
using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;

namespace plant_shop_c_sharp.Utils
{
    /// <summary>
    /// Utilitaire d envoi de reponses HTTP.
    /// </summary>
    public static class ResponseUtil
    {
        /// <summary>
        /// Envoie une reponse JSON.
        /// </summary>
        /// <param name="response">Reponse HTTP.</param>
        /// <param name="statusCode">Code HTTP.</param>
        /// <param name="payload">Objet a serialiser.</param>
        public static async Task SendJson(HttpListenerResponse response, int statusCode, object payload)
        {
            string json = JsonConvert.SerializeObject(payload, Formatting.Indented,
                new JsonSerializerSettings
                {
                    NullValueHandling = NullValueHandling.Ignore,
                    ContractResolver = new CamelCasePropertyNamesContractResolver()
                });

            response.ContentType = "application/json; charset=utf-8";
            response.StatusCode = statusCode;

            byte[] buffer = Encoding.UTF8.GetBytes(json);
            response.ContentLength64 = buffer.Length;
            await response.OutputStream.WriteAsync(buffer, 0, buffer.Length);
            response.OutputStream.Close();
        }

        /// <summary>
        /// Envoie une reponse vide.
        /// </summary>
        /// <param name="response">Reponse HTTP.</param>
        /// <param name="statusCode">Code HTTP.</param>
        public static void SendEmpty(HttpListenerResponse response, int statusCode)
        {
            response.StatusCode = statusCode;
            response.ContentLength64 = 0;
            response.OutputStream.Close();
        }

        /// <summary>
        /// Envoie une reponse d erreur JSON.
        /// </summary>
        /// <param name="response">Reponse HTTP.</param>
        /// <param name="statusCode">Code HTTP.</param>
        /// <param name="message">Message d erreur.</param>
        public static Task SendError(HttpListenerResponse response, int statusCode, string message)
        {
            return SendJson(response, statusCode, new { error = message });
        }
    }
}
