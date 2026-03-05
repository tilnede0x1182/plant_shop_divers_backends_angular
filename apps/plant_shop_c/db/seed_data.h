/**
 * @file seed_data.h
 * @brief Donnees statiques pour le peuplement de la base de donnees.
 *
 * Contient les tableaux de noms de plantes, prenoms, noms de famille
 * et domaines email utilises pour generer des donnees de test.
 */
#ifndef SEED_DATA_H
#define SEED_DATA_H

/**
 * Tableau des noms de plantes (50 elements).
 */

static const char* PLANT_NAMES[] = {
    "Rose","Tulipe","Lavande","Orchidée","Basilic","Menthe","Pivoine","Tournesol",
    "Cactus (Echinopsis)","Bambou","Camomille (Matricaria recutita)","Sauge (Salvia officinalis)",
    "Romarin (Rosmarinus officinalis)","Thym (Thymus vulgaris)","Laurier-rose (Nerium oleander)",
    "Aloe vera","Jasmin (Jasminum officinale)","Hortensia (Hydrangea macrophylla)",
    "Marguerite (Leucanthemum vulgare)","Géranium (Pelargonium graveolens)",
    "Fuchsia (Fuchsia magellanica)","Anémone (Anemone coronaria)","Azalée (Rhododendron simsii)",
    "Chrysanthème (Chrysanthemum morifolium)","Digitale pourpre (Digitalis purpurea)",
    "Glaïeul (Gladiolus hortulanus)","Lys (Lilium candidum)","Violette (Viola odorata)",
    "Muguet (Convallaria majalis)","Iris (Iris germanica)","Lavandin (Lavandula intermedia)",
    "Érable du Japon (Acer palmatum)","Citronnelle (Cymbopogon citratus)","Pin parasol (Pinus pinea)",
    "Cyprès (Cupressus sempervirens)","Olivier (Olea europaea)","Papyrus (Cyperus papyrus)",
    "Figuier (Ficus carica)","Eucalyptus (Eucalyptus globulus)","Acacia (Acacia dealbata)",
    "Bégonia (Begonia semperflorens)","Calathea (Calathea ornata)","Dieffenbachia (Dieffenbachia seguine)",
    "Ficus elastica","Sansevieria (Sansevieria trifasciata)","Philodendron (Philodendron scandens)",
    "Yucca (Yucca elephantipes)","Zamioculcas zamiifolia","Monstera deliciosa",
    "Pothos (Epipremnum aureum)" // 50 noms
};

/**
 * Tableau des prenoms pour la generation d'utilisateurs (12 elements).
 */
static const char* FIRST[] = {
    "Jean","Marie","Luc","Sophie","Pierre","Camille","Thomas","Julie","Louis","Élise", "Nicolas","Chloé"
};

/**
 * Tableau des noms de famille pour la generation d'utilisateurs (10 elements).
 */
static const char* LAST[] = {
    "Dupont","Durand","Martin","Bernard","Petit","Robert","Richard","Garcia","Leroy","Moreau"
};

/**
 * Tableau des domaines email pour la generation d'adresses (3 elements).
 */
static const char* EMAIL_DOMAINS[] = {"gmail.com", "yahoo.fr", "hotmail.com"};

#endif // SEED_DATA_H
