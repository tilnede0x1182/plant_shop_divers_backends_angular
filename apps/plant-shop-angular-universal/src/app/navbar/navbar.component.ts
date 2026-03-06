// # Importations
import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { CommonModule } from '@angular/common';
import { CartService } from '../cart/cart.service';

/**
 * Navbar – calquée sur Next (libellés/ordre identiques)
 */
@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterModule, CommonModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css'],
})
export class NavbarComponent implements OnInit {
  private auth = inject(AuthService);
  private router = inject(Router);
  private cartService = inject(CartService);

  nombreArticles = 0;
  userName = '';
  userId: number | null = null;
  estAdmin = false;

  /**
   * Initialise le composant : compteur panier, utilisateur courant, écoute des changements.
   */
  ngOnInit(): void {
    // compteur panier (inchangé)
    this.cartService.cartCount$.subscribe(
      (count) => (this.nombreArticles = count)
    );

    // récupérer l'utilisateur courant au chargement (important pour pages publiques)
    this.auth.getCurrentUser().subscribe({
      error: () => this.auth.user$.next(null),
    });

    // écoute des changements utilisateur
    this.auth.user$.subscribe((u) => {
      this.userName = this.capitalizeName(String(u?.name || ''));
      this.userId = u?.id ?? null;
      this.estAdmin = u?.admin === true;
    });
  }

  /**
   * Déconnecte l'utilisateur et redirige vers l'accueil.
   */
  logout(): void {
    this.auth.logout().subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: () => {
        this.router.navigate(['/']);
      },
    });
  }

  /**
   * Met en majuscule la première lettre de chaque mot du nom.
   * @param name Nom à formater
   * @return Nom formaté avec majuscules
   */
  private capitalizeName(name: string): string {
    return name
      .split(' ')
      .filter(Boolean)
      .map((m) => m.charAt(0).toUpperCase() + m.slice(1))
      .join(' ');
  }

  /**
   * Indique si l'utilisateur est connecté.
   * @return true si connecté, false sinon
   */
  get estConnecte(): boolean {
    return this.userId !== null;
  }
}
