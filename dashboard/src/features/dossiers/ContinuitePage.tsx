/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { Link } from 'react-router'
import { BedDouble, Lock } from 'lucide-react'
import { useDossiersHospitalises, useModeDegrade } from '@/api/hooks'
import { PageHeader } from '@/components/PageHeader'
import { Chargement, EtatVide } from '@/components/EtatVide'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

/**
 * Vue de continuité des soins (livrable 1.5) : dossiers de tous les patients hospitalisés,
 * consultables en un seul écran — l'usage prévu pendant le mode dégradé.
 */
export function ContinuitePage() {
  const dossiers = useDossiersHospitalises()
  const mode = useModeDegrade()

  return (
    <>
      <PageHeader
        titre="Dossiers des patients hospitalisés"
        description="Vue de continuité des soins : consultable en toutes circonstances, y compris en mode dégradé."
        actions={mode.data?.actif && <Badge variant="warning"><Lock /> Lecture seule</Badge>}
      />
      {dossiers.isPending ? <Chargement /> : dossiers.data?.length === 0 ? <EtatVide>Aucun patient hospitalisé.</EtatVide> : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {dossiers.data?.map((d) => (
            <Card key={d.id} className="gap-3">
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <BedDouble className="size-4 text-muted-foreground" />
                  <Link to={`/patients/${d.patientId}`} className="hover:underline">{d.patientNom}</Link>
                </CardTitle>
                {d.lectureSeule && <Lock className="size-4 text-ink-warning" aria-label="Lecture seule" />}
              </CardHeader>
              <CardContent className="grid gap-2 text-sm">
                <div><span className="text-muted-foreground">Antécédents : </span>{d.antecedents || '—'}</div>
                <div><span className="text-muted-foreground">Observations : </span>{d.observations || '—'}</div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </>
  )
}
