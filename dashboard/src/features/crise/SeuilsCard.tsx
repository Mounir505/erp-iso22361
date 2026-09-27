/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { useState, type FormEvent } from 'react'
import { FileCheck2, Settings2, SlidersHorizontal } from 'lucide-react'
import { api } from '@/api/client'
import { useAction, useDetecteur, useIntegrite } from '@/api/hooks'
import { useAuth } from '@/auth/AuthContext'
import { fmtHeure } from '@/lib/format'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'

/** Compteurs du détecteur de seuils (livrable 1.3) et contrôle d'intégrité. */
export function SeuilsCard() {
  const { peut } = useAuth()
  const det = useDetecteur()
  const integ = useIntegrite()
  const [config, setConfig] = useState(false)
  const nouvelleRef = useAction(() => api.post('/api/crise/integrite/reference'), {
    succes: 'Nouvelle référence d’intégrité enregistrée', invalider: [['integrite']],
  })
  const d = det.data

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle className="flex items-center gap-2"><SlidersHorizontal className="size-4" /> Détecteur de seuils</CardTitle>
          <CardDescription>Compteurs en fenêtre glissante.</CardDescription>
        </div>
        {peut('seuils') && <Button variant="ghost" size="icon" aria-label="Configurer les seuils" onClick={() => setConfig((c) => !c)}><Settings2 /></Button>}
      </CardHeader>
      <CardContent className="grid gap-4">
        {d && (
          <>
            <Jauge libelle={`Échecs d’authentification / ${d.fenetre} s`} valeur={d.echecsAuthDansFenetre} seuil={d.seuilEchecsAuth} consequence="Alerte (WARNING)" />
            <Jauge libelle={`Accès dossiers par utilisateur / ${d.fenetreAccesMasseSecondes} s`} valeur={d.maxAccesDossiersParUtilisateur} seuil={d.seuilAccesMasseDossiers} consequence="Crise (CRITICAL)" />
            <p className="text-xs text-muted-foreground">Indisponibilité &gt; {d.seuilIndisponibiliteSecondes} s → incident · altération ≥ {d.seuilAlterationsIntegrite} fichier(s) → crise</p>
          </>
        )}
        {config && d && <ConfigSeuils seuil={d.seuilEchecsAuth} fenetre={d.fenetre} onFermer={() => setConfig(false)} />}

        <div className="grid gap-1.5 border-t pt-3 text-sm">
          <div className="flex items-center justify-between gap-2">
            <span className="flex items-center gap-1.5 font-medium"><FileCheck2 className="size-4" /> Intégrité des documents</span>
            <span className={cn('text-xs font-semibold',
              !integ.data?.derniereVerification ? 'text-muted-foreground' : integ.data.alterations.length ? 'text-ink-critical' : 'text-ink-ok')}>
              {!integ.data?.derniereVerification ? 'Première vérification en attente'
                : integ.data.alterations.length ? `${integ.data.alterations.length} altération(s)` : 'Intègre'}
            </span>
          </div>
          {integ.data?.alterations.slice(0, 4).map((a) => <div key={a} className="truncate font-mono text-xs text-muted-foreground">{a}</div>)}
          <div className="text-xs text-muted-foreground">{integ.data?.fichiersSurveilles ?? 0} fichiers surveillés · vérifié à {fmtHeure(integ.data?.derniereVerification)}</div>
          {peut('technique') && integ.data?.alterations.length ? (
            <Button size="sm" variant="outline" className="mt-1 w-fit" disabled={nouvelleRef.isPending}
              onClick={() => confirm('Les fichiers ont-ils été restaurés depuis une source saine ?') && nouvelleRef.mutate(undefined)}>
              Fixer une nouvelle référence
            </Button>
          ) : null}
        </div>
      </CardContent>
    </Card>
  )
}

/** Jauge de progression vers un seuil — le libellé textuel porte la valeur, la couleur n'est qu'un renfort. */
function Jauge({ libelle, valeur, seuil, consequence }: { libelle: string; valeur: number; seuil: number; consequence: string }) {
  const ratio = Math.min(1, valeur / seuil)
  const couleur = ratio >= 1 ? 'bg-level-critical' : ratio >= 0.6 ? 'bg-level-warning' : 'bg-level-info'
  return (
    <div className="grid gap-1.5">
      <div className="flex items-baseline justify-between gap-2 text-sm">
        <span className="text-muted-foreground">{libelle}</span>
        <span className="tabular font-medium">{valeur} / {seuil}</span>
      </div>
      <div className="h-2 overflow-hidden rounded-full bg-muted" role="meter" aria-valuenow={valeur} aria-valuemin={0} aria-valuemax={seuil} aria-label={libelle}>
        <div className={cn('h-full rounded-full transition-all', couleur)} style={{ width: `${Math.max(ratio * 100, valeur > 0 ? 3 : 0)}%` }} />
      </div>
      <div className="text-[11px] text-muted-foreground">Au-delà de {seuil} : {consequence}</div>
    </div>
  )
}

function ConfigSeuils({ seuil, fenetre, onFermer }: { seuil: number; fenetre: number; onFermer: () => void }) {
  const [s, setS] = useState(seuil)
  const [f, setF] = useState(fenetre)
  const enregistrer = useAction(() => api.put('/api/crise/detecteur', { seuilEchecsAuth: s, fenetre: f }), {
    succes: 'Seuils mis à jour (modification tracée)', invalider: [['detecteur']], onSuccess: onFermer,
  })
  function soumettre(e: FormEvent) {
    e.preventDefault()
    enregistrer.mutate(undefined)
  }
  return (
    <form onSubmit={soumettre} className="grid gap-3 rounded-md border bg-muted/30 p-3">
      <div className="grid grid-cols-2 gap-3">
        <Field label="Seuil d’échecs" htmlFor="seuil"><Input id="seuil" type="number" min={1} max={10000} value={s} onChange={(e) => setS(Number(e.target.value))} /></Field>
        <Field label="Fenêtre (s)" htmlFor="fenetre"><Input id="fenetre" type="number" min={5} max={86400} value={f} onChange={(e) => setF(Number(e.target.value))} /></Field>
      </div>
      <div className="flex justify-end gap-2">
        <Button type="button" size="sm" variant="ghost" onClick={onFermer}>Annuler</Button>
        <Button type="submit" size="sm" disabled={enregistrer.isPending}>Appliquer</Button>
      </div>
    </form>
  )
}
