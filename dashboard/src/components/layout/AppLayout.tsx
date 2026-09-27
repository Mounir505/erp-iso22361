/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { NavLink, Outlet, useNavigate } from 'react-router'
import {
  Activity, BedDouble, ClipboardList, FileText, LogOut, Moon, Pill, ShieldAlert, Sun, Users, Lock,
} from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { LIBELLES_ROLES, type Droit } from '@/auth/roles'
import { useModeDegrade, useSituation } from '@/api/hooks'
import { useTheme } from '@/lib/theme'
import { cn } from '@/lib/utils'
import { COPYRIGHT } from '@/lib/copyright'
import { Button } from '@/components/ui/button'

const NAVIGATION: { to: string; libelle: string; icone: typeof Activity; droit: Droit; groupe: 'crise' | 'metier' | 'admin' }[] = [
  { to: '/crise', libelle: 'Tableau de crise', icone: ShieldAlert, droit: 'crise', groupe: 'crise' },
  { to: '/rex', libelle: "Retour d'expérience", icone: FileText, droit: 'crise', groupe: 'crise' },
  { to: '/patients', libelle: 'Patients', icone: Users, droit: 'patientsLecture', groupe: 'metier' },
  { to: '/continuite', libelle: 'Dossiers hospitalisés', icone: BedDouble, droit: 'dossiersLecture', groupe: 'metier' },
  { to: '/admissions', libelle: 'Admissions', icone: ClipboardList, droit: 'admissionsLecture', groupe: 'metier' },
  { to: '/prescriptions', libelle: 'Prescriptions', icone: Pill, droit: 'prescriptionsLecture', groupe: 'metier' },
  { to: '/utilisateurs', libelle: 'Utilisateurs', icone: Activity, droit: 'admin', groupe: 'admin' },
]

const GROUPES = { crise: 'Gestion de crise', metier: 'Activité hospitalière', admin: 'Administration' }

export function AppLayout() {
  const { utilisateur, deconnecter, peut } = useAuth()
  const navigate = useNavigate()
  const [theme, basculerTheme] = useTheme()
  const mode = useModeDegrade()
  const liens = NAVIGATION.filter((n) => peut(n.droit))

  return (
    <div className="flex min-h-svh flex-col md:flex-row">
      <aside className="flex shrink-0 flex-col border-b bg-card md:w-60 md:border-r md:border-b-0">
        <div className="flex items-center gap-2.5 px-4 py-4">
          <img src="/favicon.svg" alt="" className="size-8" />
          <div className="leading-tight">
            <div className="text-sm font-semibold">ERP Hospitalier</div>
            <div className="text-xs text-muted-foreground">Atelier ISO 22361 · lab</div>
          </div>
        </div>
        <nav className="flex gap-1 overflow-x-auto px-2 pb-2 md:flex-1 md:flex-col md:overflow-visible">
          {(Object.keys(GROUPES) as (keyof typeof GROUPES)[]).map((g) => {
            const items = liens.filter((l) => l.groupe === g)
            if (items.length === 0) return null
            return (
              <div key={g} className="flex gap-1 md:mb-3 md:flex-col">
                <div className="hidden px-2 pb-1 text-[11px] font-medium tracking-wide text-muted-foreground uppercase md:block">{GROUPES[g]}</div>
                {items.map(({ to, libelle, icone: Icone }) => (
                  <NavLink
                    key={to}
                    to={to}
                    className={({ isActive }) =>
                      cn(
                        'flex items-center gap-2 rounded-md px-2.5 py-1.5 text-sm whitespace-nowrap transition-colors hover:bg-accent',
                        isActive && 'bg-accent font-medium text-accent-foreground',
                      )
                    }
                  >
                    <Icone className="size-4" />
                    {libelle}
                  </NavLink>
                ))}
              </div>
            )
          })}
        </nav>
        <div className="hidden border-t p-3 md:block">
          <div className="text-sm font-medium">{utilisateur?.nom}</div>
          <div className="text-xs text-muted-foreground">{utilisateur && LIBELLES_ROLES[utilisateur.role]}</div>
          <p className="mt-3 text-[10px] leading-snug text-muted-foreground">{COPYRIGHT}</p>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between gap-3 border-b bg-card/60 px-4 py-2.5 backdrop-blur md:px-6">
          {peut('crise') ? <SituationPastille /> : <span className="text-sm text-muted-foreground">Données fictives — environnement de formation</span>}
          <div className="flex items-center gap-1">
            <Button variant="ghost" size="icon" onClick={basculerTheme} aria-label="Changer de thème">
              {theme === 'dark' ? <Sun /> : <Moon />}
            </Button>
            <Button variant="ghost" size="sm" onClick={async () => { await deconnecter(); navigate('/login') }}>
              <LogOut /> <span className="hidden sm:inline">Déconnexion</span>
            </Button>
          </div>
        </header>

        {mode.data?.actif && (
          <div role="status" className="flex items-center gap-2 border-b border-level-warning/50 bg-level-warning/12 px-4 py-2 text-sm md:px-6">
            <Lock className="size-4 shrink-0 text-ink-warning" />
            <span>
              <strong className="text-ink-warning">Mode dégradé actif</strong> — {mode.data.perimetre.toLowerCase()} en lecture seule
              ({mode.data.patientsProteges} patients). La consultation reste possible.
            </span>
          </div>
        )}

        <main className="flex-1 px-4 py-5 md:px-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

/** Pastille de situation globale (visible des rôles de crise). */
function SituationPastille() {
  const { data } = useSituation()
  const niveau = data?.niveau ?? 'NORMAL'
  const style = {
    NORMAL: 'border-level-ok/40 bg-level-ok/10 text-ink-ok',
    ALERTE: 'border-level-warning/50 bg-level-warning/12 text-ink-warning',
    CRISE: 'border-level-critical/50 bg-level-critical/12 text-ink-critical animate-pulse',
  }[niveau]
  const libelle = { NORMAL: 'Situation normale', ALERTE: 'Alerte en cours', CRISE: 'CRISE — cellule activée' }[niveau]
  return (
    <NavLink to="/crise" className={cn('inline-flex items-center gap-2 rounded-full border px-3 py-1 text-xs font-semibold', style)}>
      <span className="size-2 rounded-full bg-current" aria-hidden />
      {libelle}
    </NavLink>
  )
}
