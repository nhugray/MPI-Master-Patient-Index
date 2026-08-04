import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './styles/globals.css'
import { ToastProvider } from './shared/components/toast/ToastProvider'
import { PatientsPage } from './features/patients/pages/PatientsPage'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ToastProvider>
      <PatientsPage />
    </ToastProvider>
  </StrictMode>,
)
