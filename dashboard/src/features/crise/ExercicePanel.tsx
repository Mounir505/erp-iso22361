/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { FlaskConical } from 'lucide-react'
import { api } from '@/api/client'
import { useAction, useExerciceStatut } from '@/api/hooks'
import { useAuth } from '@/auth/AuthContext'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

/**
 * Panneau de l'animateur d'exercice — visible uniquement si CRISIS_EXERCISE_ENABLED=true
 * et pour le rôle ADMIN. Injecte des signaux SIMULÉS (aucune attaque réelle).
 */
const SCENARIOS = [
  { chemin: 'echecs-auth', libelle: 'Rafale d’échecs d’authentification', effet: 'WARNING' },
  { chemin: 'indisponibilite', libelle: 'Panne ERP simulée (> 2 min)', effet: 'WARNING' },
  { chemin: 'indisponibilite/fin', libelle: 'Fin de la panne simulée', effet: 'INFO' },
  { chemin: 'acces-masse', libelle: 'Accès en masse aux dossiers', effet: 'CRITICAL' },
  { chemin: 'integrite', libelle: 'Altération de fichiers (rançongiciel)', effet: 'CRITICAL' },
] as const

export function ExercicePanel() {
  const { peut } = useAuth()
  const statut = useExerciceStatut()
  const injecter = useAction((chemin: string) => api.post(`/api/exercice/${chemin}`), {
    succes: 'Signal d’exercice injecté', invalider: [['situation'], ['events'], ['detecteur']],
  })
  if (!peut('admin') || !statut.data?.enabled) return null

  return (
    <Card className="border-dashed">
      <CardHeader>
        <div>
          <CardTitle className="flex items-center gap-2"><FlaskConical className="size-4" /> Animateur d’exercice</CardTitle>
          <CardDescription>Faiblesse contrôlée — signaux simulés, tracés « exercise_injection ».</CardDescription>
        </div>
      </CardHeader>
      <CardContent className="grid gap-2">
        {SCENARIOS.map((s) => (
          <Button key={s.chemin} variant="outline" className="justify-between" disabled={injecter.isPending} onClick={() => injecter.mutate(s.chemin)}>
            {s.libelle}<span className="text-xs text-muted-foreground">{s.effet}</span>
          </Button>
        ))}
      </CardContent>
    </Card>
  )
}
