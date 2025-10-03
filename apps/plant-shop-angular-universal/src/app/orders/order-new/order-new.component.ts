// # Importations
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { CartService } from '../../cart/cart.service';
import { ApiService } from '../../services/api.service';
import { RouterModule } from '@angular/router';

// # Composant OrderNew
@Component({
  selector: 'app-order-new',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './order-new.component.html',
  styleUrls: ['./order-new.component.css'],
})
export class OrderNewComponent {
  protected cart = inject(CartService);
  private api = inject(ApiService);
  private router = inject(Router);

  protected message = '';
  protected total = 0;

  ngOnInit() {
    // calcul total dès l’affichage
    const items = this.cart.getAll();
    this.total = items.reduce((sum, i) => sum + i.price * i.quantity, 0);
  }

  passerCommande() {
    const items = this.cart.toOrderPayload();

    if (items.length === 0) {
      this.message = '❌ Panier vide';
      return;
    }

    // userId est injecté côté backend via le JWT
    this.api.creerCommande({ items }).subscribe({
      next: (res) => {
        this.message = `✅ Commande #${res.id} créée`;
        this.cart.clear();
        // redirection vers la page des commandes
        this.router.navigate(['/orders']);
      },
      error: () => {
        this.message = '❌ Erreur lors de la commande';
      },
    });
  }
}
