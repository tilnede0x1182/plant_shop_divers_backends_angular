#include "utils.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "mongoose/mongoose.h"

void read_db_env(char* url, char* user, char* pass) {
    FILE* f = fopen(".env", "r");
    if (!f) {
        perror("Impossible d'ouvrir .env");
        exit(1);
    }
    char line[256];
    while (fgets(line, sizeof(line), f)) {
        char* eq = strchr(line, '=');
        if (!eq) continue;
        *eq = '\0';
        char* val = eq + 1;
        val[strcspn(val, "\r\n")] = '\0'; // Supprime newline

        if (strcmp(line, "DATABASE_URL") == 0) {
            strncpy(url, val, 127);
            url[127] = '\0';
        } else if (strcmp(line, "DATABASE_USER") == 0) {
            strncpy(user, val, 63);
            user[63] = '\0';
        } else if (strcmp(line, "DATABASE_PASS") == 0) {
            strncpy(pass, val, 63);
            pass[63] = '\0';
        }
    }
    fclose(f);
}

void read_server_env(char* port, char* jwt_secret) {
    FILE* f = fopen(".env", "r");
    if (!f) {
        perror("Impossible d'ouvrir .env");
        exit(1);
    }
    char line[256];
    while (fgets(line, sizeof(line), f)) {
        char* eq = strchr(line, '=');
        if (!eq) continue;
        *eq = '\0';
        char* val = eq + 1;
        val[strcspn(val, "\r\n")] = '\0'; // Supprime newline

        if (strcmp(line, "SERVER_ADDRESS") == 0) {
            strncpy(port, val, 15);
            port[15] = '\0';
        } else if (strcmp(line, "JWT_SECRET") == 0) {
            strncpy(jwt_secret, val, 127);
            jwt_secret[127] = '\0';
        }
    }
    fclose(f);
}

int get_cookie_manual(struct mg_http_message* hm,
                      const char* name, char* out, size_t sz) {
    printf("[COOKIE] get_cookie_manual called\n");
    struct mg_str* hdr = mg_http_get_header(hm, "Cookie");
    if (!hdr) {
        printf("[COOKIE] no Cookie header\n");
        return 0;
    }
    printf("[COOKIE] header length = %zu\n", hdr->len);

    size_t len = hdr->len;
    char buf[len + 1];
    memcpy(buf, hdr->buf, len);
    buf[len] = '\0';
    printf("[COOKIE] header content = \"%s\"\n", buf);

    char* tok = strtok(buf, ";");
    while (tok) {
        char* start = tok;
        while (*start == ' ') start++;
        printf("[COOKIE] token = \"%s\"\n", start);

        size_t keylen = strlen(name);
        if (strncmp(start, name, keylen) == 0 && start[keylen] == '=') {
            const char* val = start + keylen + 1;
            printf("[COOKIE] found %s = \"%s\"\n", name, val);
            strncpy(out, val, sz - 1);
            out[sz - 1] = '\0';
            return 1;
        }
        tok = strtok(NULL, ";");
    }

    printf("[COOKIE] %s not found\n", name);
    return 0;
}

/* get_current_user_id avec parsing manuel */
int get_current_user_id(struct mg_http_message *hm) {
		printf("Appel de la fonction : get_current_user_id\n");
    char jwt_val[32];
    if (!get_cookie_manual(hm, "plant_shop_c_backend", jwt_val, sizeof(jwt_val)))
        return 0;
    int uid = atoi(jwt_val);
    return uid > 0 ? uid : 0;
}
