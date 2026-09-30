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
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/pages/dashboard-page.component')
          .then(m => m.DashboardPageComponent)
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
      }
    ]
  }
];
