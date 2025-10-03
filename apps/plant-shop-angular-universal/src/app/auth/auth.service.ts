// # Importations
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';

// # Service d'authentification
@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = '/api/auth'; // proxy -> backend Nest
  user$ = new BehaviorSubject<any | null>(null);

  constructor(private http: HttpClient) {}

  /** Enregistrer un utilisateur */
  register(email: string, password: string, name?: string): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/register`,
      { email, password, name },
      { withCredentials: true }
    );
  }

  /** Login utilisateur */
  login(email: string, password: string): Observable<any> {
    return this.http
      .post<any>(
        `${this.apiUrl}/login`,
        { email, password },
        { withCredentials: true }
      )
      .pipe(tap(() => this.refreshUser()));
  }

  /** Logout */
  logout(): Observable<any> {
    return this.http
      .post(`${this.apiUrl}/logout`, {}, { withCredentials: true })
      .pipe(tap(() => this.user$.next(null)));
  }

  /** Récupérer l’utilisateur courant (via cookie httpOnly) */
  getCurrentUser(): Observable<any> {
    return this.http
      .get<any>(`${this.apiUrl}/me`, { withCredentials: true })
      .pipe(tap((user) => this.user$.next(user)));
  }

  /** Utilitaire interne : recharge l’utilisateur après login/register */
  private refreshUser(): void {
    this.getCurrentUser().subscribe({
      next: (user) => this.user$.next(user),
      error: () => this.user$.next(null),
    });
  }
}
