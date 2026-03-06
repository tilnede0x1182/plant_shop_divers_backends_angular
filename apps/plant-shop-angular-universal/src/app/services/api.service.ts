// # Importations
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// # Types simples
export type Plante = {
  id: number;
  name: string;
  price: number;
  stock: number;
  description?: string;
};
export type Utilisateur = {
  id: number;
  email: string;
  name: string;
  admin: boolean;
};
export type Commande = {
  id: number;
  userId: number;
  status: string | null;
  totalPrice: number | null;
  createdAt: string;
  orderItems: {
    id: number;
    quantity: number;
    plant: Plante;
  }[];
};

// # Service API
@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private base = '/api'; // proxy vers backend Nest

  /* ---------- Plantes ---------- */
  /** Liste toutes les plantes */
  listerPlantes(): Observable<Plante[]> {
    return this.http.get<Plante[]>(`${this.base}/plants`);
  }
  /**
   * Récupère une plante par son ID
   * @param id Identifiant de la plante
   */
  unePlante(id: number): Observable<Plante> {
    return this.http.get<Plante>(`${this.base}/plants/${id}`);
  }
  /**
   * Crée une nouvelle plante
   * @param data Données de la plante
   */
  creerPlante(data: Partial<Plante>): Observable<Plante> {
    return this.http.post<Plante>(`${this.base}/plants`, data);
  }
  /**
   * Crée une plante (admin)
   * @param data Données de la plante
   */
  creerPlanteAdmin(data: Partial<Plante>): Observable<Plante> {
    return this.http.post<Plante>(`${this.base}/admin/plants`, data);
  }
  /**
   * Met à jour une plante (admin)
   * @param id Identifiant de la plante
   * @param data Données à mettre à jour
   */
  majPlanteAdmin(id: number, data: Partial<Plante>): Observable<Plante> {
    return this.http.patch<Plante>(`${this.base}/admin/plants/${id}`, data);
  }
  /** Liste toutes les plantes (admin) */
  listerPlantesAdmin(): Observable<Plante[]> {
    return this.http.get<Plante[]>(`${this.base}/admin/plants`);
  }
  /**
   * Supprime une plante (admin)
   * @param id Identifiant de la plante
   */
  supprimerPlanteAdmin(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/admin/plants/${id}`);
  }

  /* ---------- Utilisateurs ---------- */
  /** Liste tous les utilisateurs */
  listerUtilisateurs(): Observable<Utilisateur[]> {
    return this.http.get<Utilisateur[]>(`${this.base}/users`);
  }
  /**
   * Récupère un utilisateur par son ID
   * @param id Identifiant de l utilisateur
   */
  unUtilisateur(id: number): Observable<Utilisateur> {
    return this.http.get<Utilisateur>(`${this.base}/users/${id}`);
  }
  /**
   * Met à jour le profil utilisateur
   * @param id Identifiant de l utilisateur
   * @param data Données à mettre à jour
   */
  majProfile(id: number, data: Partial<Utilisateur>): Observable<Utilisateur> {
    return this.http.patch<Utilisateur>(`${this.base}/users/${id}`, data);
  }
  /**
   * Met à jour un utilisateur (admin)
   * @param id Identifiant de l utilisateur
   * @param data Données à mettre à jour
   */
  majUtilisateurAdmin(
    id: number,
    data: Partial<Utilisateur>
  ): Observable<Utilisateur> {
    return this.http.patch<Utilisateur>(`${this.base}/admin/users/${id}`, data);
  }
  /** Liste tous les utilisateurs (admin) */
  listerUtilisateursAdmin(): Observable<Utilisateur[]> {
    return this.http.get<Utilisateur[]>(`${this.base}/admin/users`);
  }
  /**
   * Supprime un utilisateur (admin)
   * @param id Identifiant de l utilisateur
   */
  supprimerUtilisateurAdmin(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/admin/users/${id}`);
  }

  /* ---------- Commandes ---------- */
  /** Liste les commandes de l utilisateur */
  listerCommandes(): Observable<Commande[]> {
    return this.http.get<Commande[]>(`${this.base}/orders`);
  }
  /**
   * Récupère une commande par son ID
   * @param id Identifiant de la commande
   */
  uneCommande(id: number): Observable<Commande> {
    return this.http.get<Commande>(`${this.base}/orders/${id}`);
  }
  /**
   * Crée une nouvelle commande
   * @param data Données de la commande
   */
  creerCommande(data: any): Observable<Commande> {
    return this.http.post<Commande>(`${this.base}/orders`, data);
  }
  /**
   * Met à jour une commande
   * @param id Identifiant de la commande
   * @param data Données à mettre à jour
   */
  majCommande(id: number, data: any): Observable<Commande> {
    return this.http.patch<Commande>(`${this.base}/orders/${id}`, data);
  }
  /**
   * Supprime une commande
   * @param id Identifiant de la commande
   */
  supprimerCommande(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/orders/${id}`);
  }
}
