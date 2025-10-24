package controller;

import com.sun.net.httpserver.HttpHandler;
import java.sql.Connection;

/**
 * Classe de base pour tous les contrôleurs.
 * Pour l'instant, elle ne fait que garantir que chaque contrôleur est un HttpHandler,
 * mais elle pourrait être étendue pour partager de la logique commune (ex: gestion des sessions, connexion DB).
 */
public abstract class BaseController implements HttpHandler {
    // On pourrait ajouter ici des dépendances communes à tous les contrôleurs si nécessaire.
}
