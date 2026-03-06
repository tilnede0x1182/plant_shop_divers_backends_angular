// # Importations
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ApiService } from '../../../services/api.service';

/**
 * AdminUsersListComponent – Liste des utilisateurs (admin)
 * Affiche un tableau CRUD des utilisateurs (comme Rails / Next)
 */
@Component({
  selector: 'app-admin-users-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './users-list.component.html',
  styleUrls: ['./users-list.component.css'],
})
export class AdminUsersListComponent implements OnInit {
  private api: ApiService = inject(ApiService);

  protected utilisateurs: any[] = [];
  protected message = '';

  /** Initialise le composant et charge la liste */
  ngOnInit(): void {
    this.chargerUtilisateurs();
  }

  /**
   * Charge tous les utilisateurs depuis l’API
   */
  chargerUtilisateurs(): void {
    this.api.listerUtilisateursAdmin().subscribe({
      next: (data) =>
        (this.utilisateurs = data.sort((a, b) => {
          if (a.admin === b.admin) {
            return a.name.localeCompare(b.name);
          }
          return a.admin ? -1 : 1; // admins d’abord
        })),
      error: () =>
        (this.message = '❌ Erreur lors du chargement des utilisateurs'),
    });
  }

  /**
   * Supprime un utilisateur (avec confirmation)
   * @param id Identifiant de l'utilisateur à supprimer
   */
  supprimerUtilisateur(id: number): void {
    if (!confirm('Supprimer cet utilisateur ?')) return;
    this.api.supprimerUtilisateurAdmin(id).subscribe({
      next: () => {
        this.message = 'Utilisateur supprimé ✅';
        this.chargerUtilisateurs();
      },
      error: () => (this.message = '❌ Erreur lors de la suppression'),
    });
  }
}
