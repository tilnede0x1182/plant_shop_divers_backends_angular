// # Importations
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ApiService, Plante } from '../../services/api.service';
import { AuthService } from '../../auth/auth.service';
import { CartService } from '../../cart/cart.service';

// # Données
type Cart = {
  add: (id: number, name: string, price: number, stock: number) => void;
};

// # Fonctions utilitaires
/** Décoder un JWT (payload uniquement, sans vérification) */
function decodeJwt(token: string | null): any | null {
  try {
    if (!token) return null;
    const base = token.split('.')[1];
    return JSON.parse(atob(base));
  } catch {
    return null;
  }
}

// # Fonctions utilitaires principales
/** Récupère l'instance panier globale côté navigateur si disponible */
function getCartInstance(): Cart | null {
  const win = globalThis as unknown as { cartInstance?: Cart };
  return win?.cartInstance ?? null;
}

// # Fonctions principales
@Component({
  standalone: true,
  selector: 'app-plant-detail',
  imports: [CommonModule, RouterModule],
  templateUrl: './plant-detail.component.html',
  styleUrls: ['./plant-detail.component.css'],
})
export class PlantDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(ApiService);
  private authService = inject(AuthService);
  private cartService = inject(CartService);

  protected plante: Plante | null = null;
  protected est_admin = false;

  /** Charge la plante et l'état admin */
  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.unePlante(id).subscribe((plant: Plante) => (this.plante = plant));

    this.authService.getCurrentUser().subscribe({
      next: (user: any) => (this.est_admin = !!user?.admin),
      error: () => (this.est_admin = false),
    });
  }

  /** Ajoute la plante au panier (localStorage/instance globale) */
  ajouterAuPanier() {
    if (!this.plante) return;
    this.cartService.add(
      this.plante.id,
      this.plante.name,
      this.plante.price,
      this.plante.stock
    );
  }

  /** Supprime la plante (admin), puis redirige vers la liste */
  supprimerPlante() {
    if (!this.plante) return;
    if (!confirm('Supprimer cette plante ?')) return;
    this.api.supprimerPlanteAdmin(this.plante.id).subscribe({
      next: () => this.router.navigate(['/plants']),
    });
  }
}
