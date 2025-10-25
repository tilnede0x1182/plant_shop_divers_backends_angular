#include "jwt.h"
#include "mongoose/mongoose.h"
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <openssl/hmac.h>
#include <openssl/evp.h>
#include <openssl/bio.h>
#include <openssl/buffer.h>
#include <cjson/cJSON.h>

// ========================================
// CLÉ SECRÈTE DEPUIS .ENV
// ========================================
extern char JWT_SECRET[128];

// Fonction helper : Base64 URL encode
static char* base64url_encode(const unsigned char* input, int length) {
    BIO *bio, *b64;
    BUF_MEM *buffer_ptr;

    b64 = BIO_new(BIO_f_base64());
    bio = BIO_new(BIO_s_mem());
    bio = BIO_push(b64, bio);

    BIO_set_flags(bio, BIO_FLAGS_BASE64_NO_NL);
    BIO_write(bio, input, length);
    BIO_flush(bio);
    BIO_get_mem_ptr(bio, &buffer_ptr);

    char* output = malloc(buffer_ptr->length + 1);
    memcpy(output, buffer_ptr->data, buffer_ptr->length);
    output[buffer_ptr->length] = '\0';

    BIO_free_all(bio);

    // Convertir Base64 standard en Base64URL
    for (size_t i = 0; output[i]; i++) {
        if (output[i] == '+') output[i] = '-';
        if (output[i] == '/') output[i] = '_';
        if (output[i] == '=') { output[i] = '\0'; break; }
    }

    return output;
}

// Fonction helper : Base64 URL decode
static unsigned char* base64url_decode(const char* input, int* out_len) {
    // Convertir Base64URL en Base64 standard
    size_t input_len = strlen(input);
    char* b64 = malloc(input_len + 4);
    strcpy(b64, input);

    for (size_t i = 0; b64[i]; i++) {
        if (b64[i] == '-') b64[i] = '+';
        if (b64[i] == '_') b64[i] = '/';
    }

    // Ajouter padding '='
    while (strlen(b64) % 4 != 0) {
        strcat(b64, "=");
    }

    BIO *bio, *b64io;

    b64io = BIO_new(BIO_f_base64());
    bio = BIO_new_mem_buf(b64, -1);
    bio = BIO_push(b64io, bio);

    BIO_set_flags(bio, BIO_FLAGS_BASE64_NO_NL);

    unsigned char* output = malloc(input_len);
    *out_len = BIO_read(bio, output, input_len);

    BIO_free_all(bio);
    free(b64);

    return output;
}

// Génère un token JWT
bool jwt_generate_token(int user_id, const char* email, char* token_out, size_t token_size) {
    // Vérification de la clé secrète
    if (strlen(JWT_SECRET) == 0) {
        fprintf(stderr, "❌ [JWT] JWT_SECRET non initialisé !\n");
        return false;
    }

    // Header
    cJSON* header = cJSON_CreateObject();
    cJSON_AddStringToObject(header, "alg", "HS256");
    cJSON_AddStringToObject(header, "typ", "JWT");
    char* header_str = cJSON_PrintUnformatted(header);
    char* header_b64 = base64url_encode((unsigned char*)header_str, strlen(header_str));

    // Payload
    cJSON* payload = cJSON_CreateObject();
    cJSON_AddNumberToObject(payload, "user_id", user_id);
    cJSON_AddStringToObject(payload, "email", email);
    cJSON_AddNumberToObject(payload, "exp", time(NULL) + 86400); // 24h
    char* payload_str = cJSON_PrintUnformatted(payload);
    char* payload_b64 = base64url_encode((unsigned char*)payload_str, strlen(payload_str));

    // Message à signer : header.payload
    char message[1024];
    snprintf(message, sizeof(message), "%s.%s", header_b64, payload_b64);

    // Signature HMAC-SHA256
    unsigned char signature[EVP_MAX_MD_SIZE];
    unsigned int sig_len;

    HMAC(EVP_sha256(), JWT_SECRET, strlen(JWT_SECRET),
         (unsigned char*)message, strlen(message),
         signature, &sig_len);

    char* signature_b64 = base64url_encode(signature, sig_len);

    // Token final : header.payload.signature
    snprintf(token_out, token_size, "%s.%s.%s", header_b64, payload_b64, signature_b64);

    // Debug
    fprintf(stderr, "✅ [JWT] Token généré pour user_id=%d | Clé: %.20s...\n",
            user_id, JWT_SECRET);

    // Nettoyage
    free(header_str);
    free(payload_str);
    free(header_b64);
    free(payload_b64);
    free(signature_b64);
    cJSON_Delete(header);
    cJSON_Delete(payload);

    return true;
}

// Vérifie et décode un token JWT
bool jwt_verify_token(const char* token, int* user_id_out, char* email_out, size_t email_size) {
    // Vérification de la clé secrète
    if (strlen(JWT_SECRET) == 0) {
        fprintf(stderr, "❌ [JWT] JWT_SECRET non initialisé !\n");
        return false;
    }

    // Séparer les 3 parties
    char token_copy[512];
    strncpy(token_copy, token, sizeof(token_copy) - 1);

    char* header_b64 = strtok(token_copy, ".");
    char* payload_b64 = strtok(NULL, ".");
    char* signature_b64 = strtok(NULL, ".");

    if (!header_b64 || !payload_b64 || !signature_b64) {
        fprintf(stderr, "❌ [JWT] Format invalide\n");
        return false;
    }

    // Recalculer la signature
    char message[1024];
    snprintf(message, sizeof(message), "%s.%s", header_b64, payload_b64);

    unsigned char expected_sig[EVP_MAX_MD_SIZE];
    unsigned int sig_len;

    HMAC(EVP_sha256(), JWT_SECRET, strlen(JWT_SECRET),
         (unsigned char*)message, strlen(message),
         expected_sig, &sig_len);

    char* expected_sig_b64 = base64url_encode(expected_sig, sig_len);

    // Comparer les signatures
    bool sig_valid = (strcmp(signature_b64, expected_sig_b64) == 0);
    free(expected_sig_b64);

    if (!sig_valid) {
        fprintf(stderr, "❌ [JWT] Signature invalide\n");
        return false;
    }

    // Décoder le payload
    int payload_len;
    unsigned char* payload_json = base64url_decode(payload_b64, &payload_len);

    cJSON* payload = cJSON_ParseWithLength((char*)payload_json, payload_len);
    free(payload_json);

    if (!payload) {
        fprintf(stderr, "❌ [JWT] Payload JSON invalide\n");
        return false;
    }

    // Vérifier l'expiration
    cJSON* exp_item = cJSON_GetObjectItem(payload, "exp");
    if (exp_item && cJSON_IsNumber(exp_item)) {
        time_t exp = (time_t)exp_item->valuedouble;
        if (time(NULL) > exp) {
            fprintf(stderr, "❌ [JWT] Token expiré\n");
            cJSON_Delete(payload);
            return false;
        }
    }

    // Extraire les données
    cJSON* user_id_item = cJSON_GetObjectItem(payload, "user_id");
    cJSON* email_item = cJSON_GetObjectItem(payload, "email");

    if (user_id_item && cJSON_IsNumber(user_id_item)) {
        *user_id_out = (int)user_id_item->valuedouble;
    }

    if (email_item && cJSON_IsString(email_item)) {
        strncpy(email_out, email_item->valuestring, email_size - 1);
        email_out[email_size - 1] = '\0';
    }

    fprintf(stderr, "✅ [JWT] Token valide pour user_id=%d\n", *user_id_out);

    cJSON_Delete(payload);
    return true;
}

int extract_user_id_from_cookie(struct mg_http_message* hm) {
    struct mg_str *cookie_hdr = mg_http_get_header(hm, "Cookie");
    if (!cookie_hdr) return 0;
    char jwt[512];
    if (mg_http_get_var(cookie_hdr, "jwt", jwt, sizeof jwt) <= 0) return 0;
    int uid = 0; char email[128];
    if (!jwt_verify_token(jwt, &uid, email, sizeof email)) return 0;
    return uid;
}
