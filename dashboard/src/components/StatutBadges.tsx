/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import type { StatutAdmission, StatutPatient } from '@/api/types'
import { Badge } from '@/components/ui/badge'

export const LIBELLE_STATUT_PATIENT: Record<StatutPatient, string> = { HOSPITALISE: 'Hospitalisé', AMBULATOIRE: 'Ambulatoire', SORTI: 'Sorti' }
export const LIBELLE_STATUT_ADMISSION: Record<StatutAdmission, string> = { EN_COURS: 'En cours', CLOTUREE: 'Clôturée', ANNULEE: 'Annulée' }

export function StatutPatientBadge({ statut }: { statut: StatutPatient }) {
  return <Badge variant={statut === 'HOSPITALISE' ? 'info' : 'secondary'}>{LIBELLE_STATUT_PATIENT[statut]}</Badge>
}

export function StatutAdmissionBadge({ statut }: { statut: StatutAdmission }) {
  return <Badge variant={statut === 'EN_COURS' ? 'info' : 'outline'}>{LIBELLE_STATUT_ADMISSION[statut]}</Badge>
}
