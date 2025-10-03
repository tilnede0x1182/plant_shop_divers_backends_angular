// # Importations
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ApiService } from '../../../services/api.service';

/**
 * AdminPlantsListComponent – Liste des plantes (admin)
 * Affiche un tableau CRUD des plantes (comme Rails / Next)
 */
@Component({
  selector: 'app-admin-plants-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './plants-list.component.html',
  styleUrls: ['./plants-list.component.css'],
})
export class AdminPlantsListComponent implements OnInit {
  private api: ApiService = inject(ApiService);

  protected plantes: any[] = [];
  protected message = '';

  ngOnInit(): void {
    this.chargerPlantes();
  }

  /**
   * Charge toutes les plantes depuis l’API
   */
  chargerPlantes(): void {
    this.api.listerPlantesAdmin().subscribe({
      next: (plants) => (this.plantes = plants),
      error: () => (this.message = '❌ Erreur lors du chargement des plantes'),
    });
	}

  /**
   * Supprime une plante (avec confirmation)
   * @id identifiant de la plante
   */
  supprimerPlante(id: number): void {
    if (!confirm('Supprimer cette plante ?')) return;
    this.api.supprimerPlanteAdmin(id).subscribe({
      next: () => {
        this.message = 'Plante supprimée ✅';
        this.chargerPlantes();
      },
      error: () => (this.message = '❌ Erreur lors de la suppression'),
    });
  }
}
