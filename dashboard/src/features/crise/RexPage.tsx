/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { useEffect, useState } from 'react'
import { FileDown, FileJson } from 'lucide-react'
import { toast } from 'sonner'
import { api, messageErreur } from '@/api/client'
import { useCellules, useRex } from '@/api/hooks'
import { fmtDateHeure, fmtDuree, fmtHeure } from '@/lib/format'
import { PageHeader } from '@/components/PageHeader'
import { Chargement, EtatVide } from '@/components/EtatVide'
import { NiveauBadge } from '@/components/NiveauBadge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { NativeSelect } from '@/components/ui/native-select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import { IndicateursCrise } from './IndicateursCrise'

/**
 * Retour d'expérience (ISO 22361 5.3.7 amélioration continue, 9.6 évaluation / apprentissage).
 * Le rapport est calculé à partir du journal ; l'équipe y ajoute ses leçons avant export.
 */
export function RexPage() {
  const cellules = useCellules()
  const [celluleId, setCelluleId] = useState<number>()
  const rex = useRex(celluleId)
  const [lecons, setLecons] = useState('')
  const [export_, setExport] = useState<'pdf' | 'json' | null>(null)

  useEffect(() => {
    if (celluleId == null && cellules.data?.length) setCelluleId(cellules.data[0].id)
  }, [cellules.data, celluleId])

  async function exporter(format: 'pdf' | 'json') {
    setExport(format)
    try {
      const liste = lecons.split('\n').map((l) => l.trim()).filter(Boolean)
      await api.telecharger(`/api/rex/${celluleId}?format=${format}`, { lecons: liste }, `rex-crise-${celluleId}.${format}`)
      toast.success('Rapport exporté (génération tracée dans le journal)')
    } catch (e) {
      toast.error(messageErreur(e))
    } finally {
      setExport(null)
    }
  }

  const r = rex.data
  return (
    <>
      <PageHeader
        titre="Retour d’expérience (REX)"
        description="Chronologie, temps de réponse, décisions et leçons — généré depuis le journal d’audit."
        actions={cellules.data?.length ? (
          <div className="w-64">
            <NativeSelect value={celluleId ?? ''} onChange={(e) => setCelluleId(Number(e.target.value))} aria-label="Crise">
              {cellules.data.map((c) => <option key={c.id} value={c.id}>Crise n°{c.id} · {fmtDateHeure(c.dateActivation)} · {c.statut === 'ACTIVE' ? 'en cours' : 'clôturée'}</option>)}
            </NativeSelect>
          </div>
        ) : null}
      />
      {cellules.isPending || (celluleId != null && rex.isPending) ? <Chargement /> : !r ? <EtatVide>Aucune crise enregistrée pour l’instant.</EtatVide> : (
        <div className="grid gap-5">
          <IndicateursCrise ind={r.indicateurs} />
          <p className="-mt-2 text-xs text-muted-foreground">Durée en mode dégradé : {fmtDuree(r.indicateurs.dureeModeDegradeSecondes)}</p>

          <div className="grid gap-5 xl:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
            <Card>
              <CardHeader><div><CardTitle>Chronologie</CardTitle><CardDescription>Signaux et actions de crise ; les rafales identiques sont regroupées.</CardDescription></div></CardHeader>
              <CardContent>
                <Table>
                  <TableHeader><TableRow><TableHead>Heure</TableHead><TableHead>Niveau</TableHead><TableHead>Événement</TableHead></TableRow></TableHeader>
                  <TableBody>
                    {r.chronologie.map((l, i) => (
                      <TableRow key={i}>
                        <TableCell className="tabular align-top text-xs whitespace-nowrap">{fmtHeure(l.debut)}{l.occurrences > 1 && <div className="text-muted-foreground">→ {fmtHeure(l.fin)}</div>}</TableCell>
                        <TableCell className="align-top"><NiveauBadge niveau={l.niveau} /></TableCell>
                        <TableCell>
                          {l.message}{l.occurrences > 1 && <span className="ml-1 font-medium">×{l.occurrences}</span>}
                          <div className="font-mono text-[11px] text-muted-foreground">{l.type} · {l.source}{l.user && ` · @${l.user}`}</div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>

            <div className="grid content-start gap-5">
              <Card>
                <CardHeader><CardTitle>Décisions ({r.decisions.length})</CardTitle></CardHeader>
                <CardContent>
                  {r.decisions.length === 0 ? <EtatVide>Aucune décision enregistrée.</EtatVide> : (
                    <ol className="grid gap-2">
                      {r.decisions.map((d) => (
                        <li key={d.id} className="text-sm"><span className="tabular text-xs text-muted-foreground">{fmtHeure(d.timestamp)} · {d.auteur}</span><div>{d.libelle}</div></li>
                      ))}
                    </ol>
                  )}
                </CardContent>
              </Card>
              <Card>
                <CardHeader><div><CardTitle>Leçons</CardTitle><CardDescription>Constats automatiques + leçons de l’équipe (une par ligne).</CardDescription></div></CardHeader>
                <CardContent className="grid gap-3">
                  <ul className="grid list-disc gap-1 pl-5 text-sm">{r.leconsAutomatiques.map((l) => <li key={l}>{l}</li>)}</ul>
                  <Textarea rows={5} value={lecons} onChange={(e) => setLecons(e.target.value)}
                    placeholder={'Ex. : Former les soignants à la procédure dégradée\nPré-rédiger les messages d’attente (holding statements)'} />
                  <div className="flex flex-wrap gap-2">
                    <Button onClick={() => exporter('pdf')} disabled={export_ != null}><FileDown /> Exporter en PDF</Button>
                    <Button variant="outline" onClick={() => exporter('json')} disabled={export_ != null}><FileJson /> Exporter en JSON</Button>
                  </div>
                </CardContent>
              </Card>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
