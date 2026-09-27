/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import type { Role } from '@/api/types'

/** Miroir des règles @PreAuthorize du backend (le backend reste l'autorité). */
export const DROITS = {
  patientsLecture: ['ADMIN', 'MEDECIN', 'INFIRMIER', 'AGENT_ADMISSION', 'PHARMACIEN'],
  patientsEcriture: ['ADMIN', 'MEDECIN', 'AGENT_ADMISSION'],
  patientsSuppression: ['ADMIN'],
  dossiersLecture: ['MEDECIN', 'INFIRMIER'],
  dossiersEcriture: ['MEDECIN'],
  admissionsLecture: ['ADMIN', 'MEDECIN', 'INFIRMIER', 'AGENT_ADMISSION'],
  admissionsEcriture: ['ADMIN', 'MEDECIN', 'AGENT_ADMISSION'],
  prescriptionsLecture: ['MEDECIN', 'INFIRMIER', 'PHARMACIEN'],
  prescriptionsEcriture: ['MEDECIN'],
  crise: ['ADMIN', 'PILOTE_CRISE', 'RESP_TECHNIQUE', 'RESP_COMMUNICATION', 'SECRETAIRE_CRISE'],
  pilote: ['PILOTE_CRISE'],
  modeDegrade: ['PILOTE_CRISE', 'RESP_TECHNIQUE'],
  decisions: ['PILOTE_CRISE', 'SECRETAIRE_CRISE'],
  seuils: ['ADMIN', 'PILOTE_CRISE', 'RESP_TECHNIQUE'],
  technique: ['ADMIN', 'RESP_TECHNIQUE'],
  admin: ['ADMIN'],
} satisfies Record<string, Role[]>

export type Droit = keyof typeof DROITS

export const LIBELLES_ROLES: Record<Role, string> = {
  ADMIN: 'Administrateur SI',
  MEDECIN: 'Médecin',
  INFIRMIER: 'Infirmier·e',
  AGENT_ADMISSION: "Agent d'admission",
  PHARMACIEN: 'Pharmacien·ne',
  PILOTE_CRISE: 'Pilote de crise',
  RESP_TECHNIQUE: 'Responsable technique',
  RESP_COMMUNICATION: 'Responsable communication',
  SECRETAIRE_CRISE: 'Secrétaire de crise',
}

/** Page d'accueil selon le rôle. */
export function accueil(role: Role): string {
  if ((DROITS.crise as Role[]).includes(role)) return '/crise'
  if ((DROITS.patientsLecture as Role[]).includes(role)) return '/patients'
  return '/prescriptions'
}
