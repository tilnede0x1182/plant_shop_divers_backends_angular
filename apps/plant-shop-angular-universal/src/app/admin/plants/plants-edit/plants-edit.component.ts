// # Importations
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, Plante } from '../../../services/api.service';

@Component({
  selector: 'app-plants-edit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './plants-edit.component.html',
  styleUrls: ['./plants-edit.component.css'],
})
export class PlantsEditComponent implements OnInit {
  plante: Plante = { id: 0, name: '', price: 0, stock: 0, description: '' };
  erreurs: string[] = [];

  constructor(
    private route: ActivatedRoute,
    private api: ApiService,
    private router: Router
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.erreurs = ['ID de plante invalide.'];
      return;
    }
    this.api.unePlante(id).subscribe({
      next: (data: Plante) => (this.plante = data),
      error: () => (this.erreurs = ['Erreur de chargement de la plante.']),
    });
  }

  onSubmit() {
    this.api.majPlanteAdmin(this.plante.id, this.plante).subscribe({
      next: () => this.router.navigate(['/admin/plants']),
      error: (res: any) =>
        (this.erreurs = res.error?.errors || ['Erreur de mise à jour.']),
    });
  }
}
