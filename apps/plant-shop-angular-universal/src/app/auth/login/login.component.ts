// # Importations
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

// # Composant Login
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = '';
  password = '';
  message = '';

  onSubmit() {
    this.auth.login(this.email, this.password).subscribe({
      next: () => {
        this.message = 'Connexion réussie ✅';
        this.router.navigate(['/plants']);
      },

      error: () => (this.message = '❌ Erreur lors de la connexion'),
    });
  }
}
