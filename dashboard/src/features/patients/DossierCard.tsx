/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { useEffect, useState } from 'react'
import { Lock, Save } from 'lucide-react'
import { api } from '@/api/client'
import { useAction, useDossier } from '@/api/hooks'
import { useAuth } from '@/auth/AuthContext'
import { Chargement } from '@/components/EtatVide'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Field } from '@/components/ui/field'
import { Textarea } from '@/components/ui/textarea'

/**
 * Dossier médical d'un patient. Chaque ouverture est journalisée (medical_record_access).
 * En mode dégradé, le dossier d'un patient hospitalisé est en lecture seule (HTTP 423 côté API).
 */
export function DossierCard({ patientId }: { patientId: number }) {
  const { peut } = useAuth()
  const dossier = useDossier(patientId)
  const [antecedents, setAntecedents] = useState('')
  const [observations, setObservations] = useState('')

  useEffect(() => {
    if (dossier.data) {
      setAntecedents(dossier.data.antecedents ?? '')
      setObservations(dossier.data.observations ?? '')
    }
  }, [dossier.data])

  const enregistrer = useAction(
    () => api.put(`/api/patients/${patientId}/dossier`, { antecedents, observations }),
    { succes: 'Dossier médical enregistré', invalider: [['dossier', patientId]] },
  )

  if (dossier.isPending) return <Card><CardContent><Chargement lignes={3} /></CardContent></Card>
  if (!dossier.data) return null
  const lectureSeule = dossier.data.lectureSeule || !peut('dossiersEcriture')
  const modifie = antecedents !== (dossier.data.antecedents ?? '') || observations !== (dossier.data.observations ?? '')

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Dossier médical</CardTitle>
          <CardDescription>Donnée vitale — chaque consultation est tracée dans le journal d'audit.</CardDescription>
        </div>
        {peut('dossiersEcriture') && (
          <Button onClick={() => enregistrer.mutate(undefined)} disabled={lectureSeule || !modifie || enregistrer.isPending}>
            <Save /> Enregistrer
          </Button>
        )}
      </CardHeader>
      <CardContent className="grid gap-4">
        {dossier.data.lectureSeule && (
          <Alert variant="warning">
            <Lock />
            <AlertTitle>Lecture seule — mode dégradé actif</AlertTitle>
            <AlertDescription>Le dossier reste consultable pour la continuité des soins ; les modifications sont suspendues par la cellule de crise.</AlertDescription>
          </Alert>
        )}
        <Field label="Antécédents" htmlFor="antecedents">
          <Textarea id="antecedents" value={antecedents} onChange={(e) => setAntecedents(e.target.value)} readOnly={lectureSeule} rows={3} />
        </Field>
        <Field label="Observations" htmlFor="observations">
          <Textarea id="observations" value={observations} onChange={(e) => setObservations(e.target.value)} readOnly={lectureSeule} rows={5} />
        </Field>
      </CardContent>
    </Card>
  )
}
