// # Importations
import { Route } from '@angular/router';
import { AuthGuard } from './auth/auth.guard';
import { AdminGuard } from './auth/admin.guard';

export const appRoutes: Route[] = [
  // 🌿 Produits
  {
    path: 'plants',
    loadComponent: () =>
      import('./plants/plants-list/plants-list.component').then(
        (m) => m.PlantsListComponent
      ),
  },
  {
    path: 'admin/plants/new',
    loadComponent: () =>
      import('./admin/plants/plant-new/plant-new.component').then(
        (m) => m.PlantNewComponent
      ),
    canActivate: [AdminGuard],
  },
  {
    path: 'plants/:id',
    loadComponent: () =>
      import('./plants/plant-detail/plant-detail.component').then(
        (m) => m.PlantDetailComponent
      ),
  },

  // 👤 Auth
  {
    path: 'login',
    loadComponent: () =>
      import('./auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./auth/register/register.component').then(
        (m) => m.RegisterComponent
      ),
  },

  // 🛒 Panier
  {
    path: 'cart',
    loadComponent: () =>
      import('./cart/cart/cart.component').then((m) => m.CartComponent),
  },

  // 👤 Profil
  {
    path: 'profile',
    loadComponent: () =>
      import('./users/user-profile/user-profile.component').then(
        (m) => m.UserProfileComponent
      ),
    canActivate: [AuthGuard],
  },
  {
    path: 'profile/edit/:id',
    loadComponent: () =>
      import('./users/user-profile-edit/user-profile-edit.component').then(
        (m) => m.UserProfileEditComponent
      ),
    canActivate: [AdminGuard],
  },

  // 📦 Commandes
  {
    path: 'orders/new',
    loadComponent: () =>
      import('./orders/order-new/order-new.component').then(
        (m) => m.OrderNewComponent
      ),
    canActivate: [AuthGuard],
  },
  {
    path: 'orders',
    loadComponent: () =>
      import('./orders/order-list/order-list.component').then(
        (m) => m.OrderListComponent
      ),
    canActivate: [AuthGuard],
  },
  // ⚙️ Admin
  {
    path: 'admin/users',
    loadComponent: () =>
      import('./admin/users/users-list/users-list.component').then(
        (m) => m.AdminUsersListComponent
      ),
    canActivate: [AdminGuard],
  },
  {
    path: 'admin/users/edit/:id',
    loadComponent: () =>
      import(
        './admin/users/user-profile-edit/user-profile-edit.component'
      ).then((m) => m.AdminUserEditComponent),
    canActivate: [AdminGuard],
  },
  {
    path: 'admin/users/:id',
    loadComponent: () =>
      import('./admin/users/user-details/user-details.component').then(
        (m) => m.UserDetailsComponent
      ),
    canActivate: [AdminGuard],
  },
  {
    path: 'admin/plants',
    loadComponent: () =>
      import('./admin/plants/plants-list/plants-list.component').then(
        (m) => m.AdminPlantsListComponent
      ),
    canActivate: [AdminGuard],
  },
  {
    path: 'admin/plants/:id/edit',
    loadComponent: () =>
      import('./admin/plants/plants-edit/plants-edit.component').then(
        (m) => m.PlantsEditComponent
      ),
    canActivate: [AdminGuard],
  },

  // Route par défaut
  { path: '', redirectTo: '/plants', pathMatch: 'full' },

  // 404
  { path: '**', redirectTo: '/plants' },
];
