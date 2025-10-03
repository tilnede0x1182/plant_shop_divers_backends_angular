// # Importations
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

// # Données
export type CartItem = {
  id: number;
  name: string;
  price: number;
  quantity: number;
  stock: number;
};

@Injectable({ providedIn: 'root' })
export class CartService {
  private storageKey = 'cart';
  private items: Record<number, CartItem> = {};
  cartCount$ = new BehaviorSubject<number>(0);

  constructor() {
    this.load();
    this.updateCount();
  }

  /** Ajouter une plante */
  add(id: number, name: string, price: number, stock: number) {
    if (!this.items[id]) {
      this.items[id] = { id, name, price, quantity: 0, stock };
    }
    if (this.items[id].quantity >= stock) {
      this.showStockAlert(name, stock); // Modale custom
      setTimeout(() => {
        // Attente 300ms
        this.items[id].quantity = stock; // Clamp au stock
        this.save();
        this.updateCount(); // Persistance + compteur
      }, 300);
      return;
    }
    this.items[id].quantity++; // Incrément normal
    this.save();
    this.updateCount(); // Persistance + compteur
  }

  /** Mettre à jour quantité */
  update(id: number, quantity: number): number {
    if (!this.items[id]) return 0;
    const stock = this.items[id].stock;
    const corrected = Math.min(Math.max(quantity, 1), stock);

    // on garde la valeur brute immédiatement (comme en Rails)
    this.items[id].quantity = corrected;
    this.save();

    // compteur et total mis à jour après 300ms
    setTimeout(() => {
      this.updateCount();
    }, 300);

    return corrected;
  }

  /** Supprimer un produit */
  remove(id: number) {
    delete this.items[id];
    this.save();
    this.updateCount();
  }

  /** Vider le panier */
  clear() {
    this.items = {};
    localStorage.removeItem(this.storageKey);
    this.updateCount();
  }

  /** Récupérer le contenu du panier */
  getAll(): CartItem[] {
    return Object.values(this.items);
  }

  /** Total général */
  getTotal(): number {
    return this.getAll().reduce(
      (sum, item) => sum + item.price * item.quantity,
      0
    );
  }

  /** Payload API */
  toOrderPayload() {
    return this.getAll().map((item) => ({
      plantId: item.id,
      quantity: item.quantity,
    }));
  }

  /** Charger depuis localStorage */
  private load() {
    const raw = localStorage.getItem(this.storageKey);
    this.items = raw ? JSON.parse(raw) : {};
  }

  /** Sauvegarder dans localStorage */
  private save() {
    localStorage.setItem(this.storageKey, JSON.stringify(this.items));
  }

  /** Modale stock insuffisant (alignée sur l'implémentation Rails) */
  private showStockAlert(name: string, stock: number) {
    if (typeof window === 'undefined' || !document?.body) return;
    const alert = document.createElement('div');
    alert.className =
      'alert alert-warning fade position-absolute top-0 start-50 translate-middle-x mt-3 shadow';
    alert.setAttribute('role', 'alert');
    alert.setAttribute(
      'style',
      'z-index:1055;max-width:600px;pointer-events:none'
    );
    alert.append(
      document.createTextNode('Stock insuffisant pour cette plante ('),
      (() => {
        const strong = document.createElement('strong');
        strong.textContent = name;
        return strong;
      })(),
      document.createTextNode(`), actuellement, il en reste ${stock}.`)
    );
    document.body.append(alert);
    setTimeout(() => alert.classList.add('show'), 10); // animation d'apparition
    setTimeout(() => {
      // disparition + cleanup
      alert.classList.remove('show');
      alert.classList.add('fade');
      setTimeout(() => alert.remove(), 300);
    }, 3000);
  }

  /** Mettre à jour compteur */
  private updateCount() {
    const total = this.getAll().reduce((sum, item) => sum + item.quantity, 0);
    this.cartCount$.next(total);
  }
}
