import { Routes } from '@angular/router';
import { LayoutComponent } from './layout/layout.component';

export const routes: Routes = [
  {
    path: '',
    component: LayoutComponent,
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
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/pages/dashboard-page.component')
          .then(m => m.DashboardPageComponent)
      },
      {
        path: 'facilities',
        loadComponent: () => import('./features/facility/pages/facilities-page.component')
          .then(m => m.FacilitiesPageComponent)
      }
    ]
  }
];
