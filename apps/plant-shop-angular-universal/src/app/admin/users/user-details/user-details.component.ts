// # Importations
import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ApiService, Utilisateur } from '../../../services/api.service';
import { AuthService } from '../../../auth/auth.service';

// # Composant
@Component({
  selector: 'app-user-details',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './user-details.component.html',
  styleUrls: ['./user-details.component.css'],
})
export class UserDetailsComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private auth = inject(AuthService);

  protected utilisateur: Utilisateur | null = null;
  protected erreurs: string[] = [];
  protected est_admin = false;
  protected chargement = true;

  private destroy$ = new Subject<void>();

  /**
   * Récupère l'ID depuis l'URL.
   */
  private idDepuisUrl(): number | null {
    const valeur = Number(this.route.snapshot.paramMap.get('id'));
    return Number.isFinite(valeur) && valeur > 0 ? valeur : null;
  }

  /**
   * Charge l'utilisateur par ID.
   */
  private chargerUtilisateur(id: number): void {
    this.api
      .unUtilisateur(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (donnees) => {
          this.utilisateur = donnees;
          this.chargement = false;
        },
        error: () => {
          this.erreurs = ['Utilisateur introuvable'];
          this.chargement = false;
        },
      });
  }

  /**
   * Redirige vers la liste admin.
   */
  allerListe(): void {
    this.router.navigate(['/admin/users']);
  }

  /**
   * Supprime l’utilisateur puis revient à la liste.
   */
  supprimer(): void {
    if (!this.utilisateur) return;
    if (!confirm('Supprimer cet utilisateur ?')) return;
    this.api
      .supprimerUtilisateurAdmin(this.utilisateur.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.allerListe(),
        error: () => (this.erreurs = ['Suppression impossible']),
      });
  }

  /**
   * Initialisation : récupère l'état admin et charge l'utilisateur.
   */
  ngOnInit(): void {
    this.auth.user$
      .pipe(takeUntil(this.destroy$))
      .subscribe(
        (utilisateurCourant) => (this.est_admin = !!utilisateurCourant?.admin)
      );

    const identifiant = this.idDepuisUrl();
    if (!identifiant) {
      this.erreurs = ['ID utilisateur invalide'];
      this.chargement = false;
      return;
    }
    this.chargerUtilisateur(identifiant);
  }

  /**
   * Nettoyage des abonnements.
   */
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
