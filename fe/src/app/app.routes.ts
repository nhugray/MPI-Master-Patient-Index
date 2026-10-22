import { Routes } from '@angular/router';
import { LayoutComponent } from './layout/layout.component';
import { authGuard } from './core/guards/auth.guard';
import { noAuthGuard } from './core/guards/no-auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/pages/login-page/login-page.component')
      .then(m => m.LoginPageComponent),
    canActivate: [noAuthGuard]
  },

  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: '',
        redirectTo: 'patients',
        pathMatch: 'full'
      },
      {
        path: 'patients',
        loadComponent: () => import('./features/patient/pages/patients-page.component')
          .then(m => m.PatientsPageComponent)
      },
      {
        path: 'patient-import',
        loadComponent: () => import('./features/patient-import/pages/patient-import-page.component')
          .then(m => m.PatientImportPageComponent)
      },
      {
        path: 'patient-masters',
        loadComponent: () => import('./features/patient-master/pages/patient-master-page.component')
          .then(m => m.PatientMasterPageComponent)
      },
      {
        path: 'patient-masters/:id',
        loadComponent: () => import('./features/patient-master/pages/patient-master-detail-page/patient-master-detail-page.component')
          .then(m => m.PatientMasterDetailPageComponent)
      },
      {
        path: 'review-queue',
        loadComponent: () => import('./features/review-queue/pages/review-queue-page.component')
          .then(m => m.ReviewQueuePageComponent)
      },
      {
        path: 'review-queue/:id',
        loadComponent: () => import('./features/review-queue/pages/review-queue-compare-page/review-queue-compare-page.component')
          .then(m => m.ReviewQueueComparePageComponent)
      },
      {
        path: 'algorithm-config',
        loadComponent: () => import('./features/algorithm-config/pages/algorithm-config-page.component')
          .then(m => m.AlgorithmConfigPageComponent)
      },
      {
        path: 'facilities',
        loadComponent: () => import('./features/facility/pages/facilities-page.component')
          .then(m => m.FacilitiesPageComponent)
      },
      {
        path: 'source-systems',
        loadComponent: () => import('./features/source-system/pages/source-systems-page.component')
          .then(m => m.SourceSystemsPageComponent)
      },
      {
        path: 'users',
        loadComponent: () => import('./features/user/pages/users-page/users-page.component')
          .then(m => m.UsersPageComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/user/pages/user-profile-page/user-profile-page.component')
          .then(m => m.UserProfilePageComponent)
      }
    ]
  },

  {
    path: '**',
    redirectTo: 'login'
  }
];
