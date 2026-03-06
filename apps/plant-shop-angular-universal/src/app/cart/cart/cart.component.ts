// # Importations
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { CartService, CartItem } from '../cart.service';
import { FormsModule } from '@angular/forms';

// # Composant Cart
@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.css'],
})
export class CartComponent {
  private timers: Record<number, any> = {};
  private cart = inject(CartService);
  protected items: CartItem[] = [];
  private totalValue = 0;
  private totalTimer: any;
  private lineTotals: Record<number, number> = {};
  private lineTimers: Record<number, any> = {};

  /** Initialise le composant et charge le panier */
  ngOnInit(): void {
    this.refresh(true);
  }

  /** Programme la mise à jour du total après délai */
  private scheduleTotalUpdate() {
    clearTimeout(this.totalTimer);
    this.totalTimer = setTimeout(() => {
      this.totalValue = this.items.reduce(
        (sum, i) => sum + i.price * i.quantity,
        0
      );
    }, 300);
  }

  /**
   * Récupère le total d une ligne
   * @param id Identifiant de la plante
   */
  getLineTotal(id: number): number {
    const val = this.lineTotals[id] ?? 0;
    return Math.round(val * 100) / 100;
  }

  /**
   * Programme la mise à jour du total ligne après délai
   * @param item Article du panier
   */
  private scheduleLineTotalUpdate(item: CartItem) {
    clearTimeout(this.lineTimers[item.id]);
    this.lineTimers[item.id] = setTimeout(() => {
      this.lineTotals[item.id] = item.price * item.quantity;
    }, 300);
  }

  /**
   * Rafraîchit l affichage du panier
   * @param initial true si premier chargement
   */
  refresh(initial: boolean = false) {
    this.items = this.cart.getAll();

    if (initial) {
      // premier affichage → calcul instantané
      this.totalValue = this.items.reduce(
        (sum, i) => sum + i.price * i.quantity,
        0
      );
      for (const item of this.items) {
        this.lineTotals[item.id] = item.price * item.quantity;
      }
    } else {
      // sinon → application du délai de 300 ms
      this.scheduleTotalUpdate();
      for (const item of this.items) {
        this.scheduleLineTotalUpdate(item);
      }
    }
  }

  /**
   * Incrémente la quantité d un article
   * @param item Article du panier
   */
  increment(item: CartItem) {
    this.cart.update(item.id, item.quantity + 1);
    this.refresh();
  }

  /**
   * Décrémente la quantité d un article
   * @param item Article du panier
   */
  decrement(item: CartItem) {
    if (item.quantity > 1) {
      this.cart.update(item.id, item.quantity - 1);
      this.refresh();
    }
  }

  /**
   * Met à jour la quantité après délai
   * @param id Identifiant de la plante
   * @param value Nouvelle quantité
   */
  delayedUpdate(id: number, value: any) {
    clearTimeout(this.timers[id]);
    const num = Number(value);
    this.timers[id] = setTimeout(() => {
      const corrected = this.cart.update(id, num);
      const item = this.items.find((i) => i.id === id);
      if (item) {
        item.quantity = corrected;
        this.scheduleLineTotalUpdate(item);
      }
      this.refresh();
    }, 300);
  }

  /**
   * Supprime un article du panier
   * @param id Identifiant de la plante
   */
  remove(id: number) {
    this.cart.remove(id);
    this.refresh();
  }

  /** Vide le panier */
  clear() {
    this.cart.clear();
    this.refresh();
  }

  /** Calcule le total du panier */
  total(): number {
    return Math.round(this.totalValue * 100) / 100;
  }
}
