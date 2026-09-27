/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { useState } from 'react'
import { Lock, LockOpen } from 'lucide-react'
import { api } from '@/api/client'
import { useAction, useModeDegrade } from '@/api/hooks'
import { useAuth } from '@/auth/AuthContext'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'

/** Bascule du mode dégradé (POST /status/degraded) — pilote et responsable technique. */
export function ModeDegradeCard() {
  const { peut } = useAuth()
  const mode = useModeDegrade()
  const [motif, setMotif] = useState('')
  const basculer = useAction((actif: boolean) => api.post('/status/degraded', { actif, motif: motif || null }), {
    succes: 'Mode dégradé mis à jour',
    invalider: [['mode-degrade'], ['situation'], ['decisions'], ['events']],
    onSuccess: () => setMotif(''),
  })
  const actif = mode.data?.actif ?? false

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle className="flex items-center gap-2">{actif ? <Lock className="size-4" /> : <LockOpen className="size-4" />} Mode dégradé</CardTitle>
          <CardDescription>Continuité : dossiers des patients hospitalisés en lecture seule.</CardDescription>
        </div>
        <Badge variant={actif ? 'warning' : 'outline'}>{actif ? 'ACTIF' : 'Inactif'}</Badge>
      </CardHeader>
      <CardContent className="grid gap-2">
        <p className="text-sm text-muted-foreground">
          Périmètre : {mode.data?.patientsProteges ?? '—'} patient(s) hospitalisé(s). Consultation toujours possible.
        </p>
        {peut('modeDegrade') && (
          <>
            <Input placeholder="Motif (tracé dans le journal)" value={motif} onChange={(e) => setMotif(e.target.value)} maxLength={500} />
            <Button variant={actif ? 'outline' : 'default'} disabled={basculer.isPending} onClick={() => basculer.mutate(!actif)}>
              {actif ? <><LockOpen /> Revenir au mode nominal</> : <><Lock /> Activer le mode dégradé</>}
            </Button>
          </>
        )}
      </CardContent>
    </Card>
  )
}
