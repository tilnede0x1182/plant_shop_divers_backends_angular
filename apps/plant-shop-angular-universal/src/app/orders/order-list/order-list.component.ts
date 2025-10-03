// # Importations
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ApiService, Commande } from '../../services/api.service';

// # Fonctions utilitaires
/** Formate la date ISO en locale */
function formaterDate(dateIso: string): string {
  return new Date(dateIso).toLocaleString();
}

// # Fonctions principales
@Component({
  standalone: true,
  selector: 'app-order-list',
  imports: [CommonModule, RouterModule],
  templateUrl: './order-list.component.html',
  styleUrls: ['./order-list.component.css'],
})
export class OrderListComponent implements OnInit {
  private api = inject(ApiService);
  protected commandes: Commande[] = [];

  /** Charge les commandes de l’utilisateur courant */
  ngOnInit() {
    this.api.listerCommandes().subscribe((donnees: Commande[]) => {
      this.commandes = donnees;
    });
  }

  protected formater(dateIso: string) {
    return formaterDate(dateIso);
  }
}
