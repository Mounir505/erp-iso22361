/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { AuthProvider, useAuth } from '@/auth/AuthContext'
import { Protege } from '@/auth/Protege'
import { accueil } from '@/auth/roles'
import { AppLayout } from '@/components/layout/AppLayout'
import { LoginPage } from '@/features/auth/LoginPage'
import { PatientsPage } from '@/features/patients/PatientsPage'
import { PatientDetailPage } from '@/features/patients/PatientDetailPage'
import { ContinuitePage } from '@/features/dossiers/ContinuitePage'
import { AdmissionsPage } from '@/features/admissions/AdmissionsPage'
import { PrescriptionsPage } from '@/features/prescriptions/PrescriptionsPage'
import { UtilisateursPage } from '@/features/admin/UtilisateursPage'
import { Chargement } from '@/components/EtatVide'

// Pages de crise chargées à la demande (Recharts n'alourdit pas l'écran des soignants)
const DashboardCrisePage = lazy(() => import('@/features/crise/DashboardCrisePage').then((m) => ({ default: m.DashboardCrisePage })))
const RexPage = lazy(() => import('@/features/crise/RexPage').then((m) => ({ default: m.RexPage })))

function Accueil() {
  const { utilisateur } = useAuth()
  return <Navigate to={utilisateur ? accueil(utilisateur.role) : '/login'} replace />
}

export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<Protege><AppLayout /></Protege>}>
            <Route index element={<Accueil />} />
            <Route path="crise" element={<Protege droit="crise"><Suspense fallback={<Chargement />}><DashboardCrisePage /></Suspense></Protege>} />
            <Route path="rex" element={<Protege droit="crise"><Suspense fallback={<Chargement />}><RexPage /></Suspense></Protege>} />
            <Route path="patients" element={<Protege droit="patientsLecture"><PatientsPage /></Protege>} />
            <Route path="patients/:id" element={<Protege droit="patientsLecture"><PatientDetailPage /></Protege>} />
            <Route path="continuite" element={<Protege droit="dossiersLecture"><ContinuitePage /></Protege>} />
            <Route path="admissions" element={<Protege droit="admissionsLecture"><AdmissionsPage /></Protege>} />
            <Route path="prescriptions" element={<Protege droit="prescriptionsLecture"><PrescriptionsPage /></Protege>} />
            <Route path="utilisateurs" element={<Protege droit="admin"><UtilisateursPage /></Protege>} />
            <Route path="*" element={<Accueil />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
