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

  ngOnInit(): void {
    this.refresh(true);
  }

  private scheduleTotalUpdate() {
    clearTimeout(this.totalTimer);
    this.totalTimer = setTimeout(() => {
      this.totalValue = this.items.reduce(
        (sum, i) => sum + i.price * i.quantity,
        0
      );
    }, 300);
  }

  getLineTotal(id: number): number {
    return this.lineTotals[id] ?? 0;
  }

  private scheduleLineTotalUpdate(item: CartItem) {
    clearTimeout(this.lineTimers[item.id]);
    this.lineTimers[item.id] = setTimeout(() => {
      this.lineTotals[item.id] = item.price * item.quantity;
    }, 300);
  }

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

  increment(item: CartItem) {
    this.cart.update(item.id, item.quantity + 1);
    this.refresh();
  }

  decrement(item: CartItem) {
    if (item.quantity > 1) {
      this.cart.update(item.id, item.quantity - 1);
      this.refresh();
    }
  }

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

  remove(id: number) {
    this.cart.remove(id);
    this.refresh();
  }

  clear() {
    this.cart.clear();
    this.refresh();
  }

  total(): number {
    return this.totalValue;
  }
}
