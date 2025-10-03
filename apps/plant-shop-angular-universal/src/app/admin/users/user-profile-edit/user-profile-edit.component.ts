// # Importations
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { ApiService, Utilisateur } from '../../../services/api.service';

// # Données
@Component({
  standalone: true,
  selector: 'app-user-edit',
  imports: [CommonModule, FormsModule],
  templateUrl: './user-profile-edit.component.html',
  styleUrls: ['./user-profile-edit.component.css'],
})
export class AdminUserEditComponent {
  private api = inject(ApiService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  protected user: Utilisateur = { id: 0, email: '', name: '', admin: false };
  protected erreurs: string[] = [];

  /**
   * Charge l'utilisateur ciblé par l'URL
   */
  ngOnInit() {
    const userId = Number(this.route.snapshot.paramMap.get('id'));
    if (!userId) {
      this.erreurs = ['ID utilisateur invalide'];
      return;
    }
    this.api.unUtilisateur(userId).subscribe({
      next: (data) => (this.user = data),
      error: () => (this.erreurs = ['Chargement utilisateur impossible']),
    });
  }

  /**
   * Soumet les changements (admin peut cocher/décocher admin)
   */
  enregistrer() {
    const payload: Partial<Utilisateur> = {
      name: this.user.name,
      email: this.user.email,
      admin: this.user.admin,
    };
    this.api.majUtilisateurAdmin(this.user.id, payload).subscribe({
      next: () => this.router.navigate(['/admin/users']),
      error: () => (this.erreurs = ['Erreur lors de la mise à jour']),
    });
  }
}
