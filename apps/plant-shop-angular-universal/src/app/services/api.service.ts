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
  listerPlantes(): Observable<Plante[]> {
    return this.http.get<Plante[]>(`${this.base}/plants`);
  }
  unePlante(id: number): Observable<Plante> {
    return this.http.get<Plante>(`${this.base}/plants/${id}`);
  }
  creerPlante(data: Partial<Plante>): Observable<Plante> {
    return this.http.post<Plante>(`${this.base}/plants`, data);
  }
  creerPlanteAdmin(data: Partial<Plante>): Observable<Plante> {
    return this.http.post<Plante>(`${this.base}/admin/plants`, data);
  }
  majPlanteAdmin(id: number, data: Partial<Plante>): Observable<Plante> {
    return this.http.patch<Plante>(`${this.base}/admin/plants/${id}`, data);
  }
  listerPlantesAdmin(): Observable<Plante[]> {
    return this.http.get<Plante[]>(`${this.base}/admin/plants`);
  }
  supprimerPlanteAdmin(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/admin/plants/${id}`);
  }

  /* ---------- Utilisateurs ---------- */
  listerUtilisateurs(): Observable<Utilisateur[]> {
    return this.http.get<Utilisateur[]>(`${this.base}/users`);
  }
  unUtilisateur(id: number): Observable<Utilisateur> {
    return this.http.get<Utilisateur>(`${this.base}/users/${id}`);
  }
  majProfile(id: number, data: Partial<Utilisateur>): Observable<Utilisateur> {
    return this.http.patch<Utilisateur>(`${this.base}/users/${id}`, data);
  }
  majUtilisateurAdmin(
    id: number,
    data: Partial<Utilisateur>
  ): Observable<Utilisateur> {
    return this.http.patch<Utilisateur>(`${this.base}/admin/users/${id}`, data);
  }
  listerUtilisateursAdmin(): Observable<Utilisateur[]> {
    return this.http.get<Utilisateur[]>(`${this.base}/admin/users`);
  }
  supprimerUtilisateurAdmin(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/admin/users/${id}`);
  }

  /* ---------- Commandes ---------- */
  listerCommandes(): Observable<Commande[]> {
    return this.http.get<Commande[]>(`${this.base}/orders`);
  }
  uneCommande(id: number): Observable<Commande> {
    return this.http.get<Commande>(`${this.base}/orders/${id}`);
  }
  creerCommande(data: any): Observable<Commande> {
    return this.http.post<Commande>(`${this.base}/orders`, data);
  }
  majCommande(id: number, data: any): Observable<Commande> {
    return this.http.patch<Commande>(`${this.base}/orders/${id}`, data);
  }
  supprimerCommande(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/orders/${id}`);
  }
}
