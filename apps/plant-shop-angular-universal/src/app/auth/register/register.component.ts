// # Importations
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

// # Composant Register
@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css'],
})
export class RegisterComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  name = '';
  email = '';
  password = '';
  message = '';
  passwordConfirmation: string = '';

  onSubmit() {
    if (this.password !== this.passwordConfirmation) {
      this.message = '❌ Les mots de passe ne correspondent pas';
      window.console.warn(
        '[Register] mismatch',
        this.password,
        this.passwordConfirmation
      );
      return;
    }

    this.auth.register(this.email, this.password, this.name).subscribe({
      next: () => {
        this.message = 'Compte créé ✅ – veuillez vous connecter';
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.message = '❌ Erreur lors de la création du compte';
        window.console.error('[Register] erreur', err);
      },
    });
  }
}
