// # Importations
import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService, Utilisateur } from '../../services/api.service';
import { AuthService } from '../../auth/auth.service';
import { RouterModule } from '@angular/router';

@Component({
  standalone: true,
  selector: 'app-user-profile',
  imports: [CommonModule, RouterModule],
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.css'],
})
export class UserProfileComponent implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);

  protected user: Utilisateur | null = null;

  ngOnInit(): void {
    this.auth.getCurrentUser().subscribe({
      next: (currentUser) => {
        if (!currentUser) {
          this.user = null;
          return;
        }

        this.api.unUtilisateur(currentUser.id).subscribe((data) => {
          this.user = data;
        });
      },
      error: () => {
        this.user = null;
      },
    });
  }
}
