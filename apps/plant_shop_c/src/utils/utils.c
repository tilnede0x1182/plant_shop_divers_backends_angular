#include "utils.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

void read_env(char* url, char* user, char* pass) {
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
