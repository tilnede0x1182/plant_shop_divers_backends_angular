// # Importations
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { ApiService, Utilisateur } from '../../services/api.service';

// # Données
@Component({
  standalone: true,
  selector: 'app-user-profile-edit',
  imports: [CommonModule, FormsModule],
  templateUrl: './user-profile-edit.component.html',
  styleUrls: ['./user-profile-edit.component.css'],
})
export class UserProfileEditComponent {
  private api = inject(ApiService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  protected user: Utilisateur = { id: 0, email: '', name: '', admin: false };
  protected erreurs: string[] = [];

  /**
   * Charge le profil à éditer (utilisateur courant via :id de l'URL)
   */
  ngOnInit() {
    const userId = Number(this.route.snapshot.paramMap.get('id'));
    if (!userId) {
      this.erreurs = ['ID utilisateur invalide'];
      return;
    }
    this.api.unUtilisateur(userId).subscribe({
      next: (data) => (this.user = data),
      error: () => (this.erreurs = ['Impossible de charger vos données']),
    });
  }

  /**
   * Soumet Nom/Email uniquement (pas de case Admin côté non-admin)
   */
  enregistrer() {
    const payload: Partial<Utilisateur> = {
      name: this.user.name,
      email: this.user.email,
    };
    this.api.majProfile(this.user.id, payload).subscribe({
      next: () => this.router.navigate(['/profile']),
      error: () => (this.erreurs = ['Erreur lors de la mise à jour']),
    });
  }
}
