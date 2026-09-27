/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
/** Types miroirs des DTO du backend erp-core. */

export type Niveau = 'INFO' | 'WARNING' | 'CRITICAL'
export type StatutPatient = 'HOSPITALISE' | 'AMBULATOIRE' | 'SORTI'
export type StatutAdmission = 'EN_COURS' | 'CLOTUREE' | 'ANNULEE'

export type Role =
  | 'ADMIN' | 'MEDECIN' | 'INFIRMIER' | 'AGENT_ADMISSION' | 'PHARMACIEN'
  | 'PILOTE_CRISE' | 'RESP_TECHNIQUE' | 'RESP_COMMUNICATION' | 'SECRETAIRE_CRISE'

export interface Utilisateur { id: number; nom: string; login: string; role: Role; estRoleDeCrise: boolean }
export interface RoleDto { id: number; libelle: Role; estRoleDeCrise: boolean }
export interface LoginResponse { token: string; expiration: string; utilisateur: Utilisateur }

export interface ApiErrorBody {
  timestamp: string; status: number; erreur: string; message: string; chemin: string
  champs?: Record<string, string>
}

export interface Patient { id: number; nom: string; dateNaissance: string; statut: StatutPatient; hospitalise: boolean }
export interface DossierMedical {
  id: number; patientId: number; patientNom: string; patientHospitalise: boolean
  antecedents: string | null; observations: string | null; lectureSeule: boolean
}
export interface Admission { id: number; patientId: number; patientNom: string; dateEntree: string; service: string; statut: StatutAdmission }
export interface Prescription {
  id: number; admissionId: number; patientId: number; patientNom: string
  medicament: string; posologie: string; date: string
}

/** Ligne du journal au format du contrat d'interface 2.1. */
export interface EvenementJournal {
  id: number | null; timestamp: string; level: Niveau; event_type: string; source: string
  user: string | null; message: string; details: Record<string, unknown> | null
}

export interface Sante {
  status: 'UP' | 'DOWN'; timestamp: string
  database: { status: 'UP' | 'DOWN'; latencyMs: number; erreur: string | null }
  degradedMode: boolean | null; crisisActive: boolean | null; journalEnAttente: number
  simulation: boolean; indisponibleDepuis: string | null
}

export interface ModeDegrade { actif: boolean; lectureSeule: boolean; perimetre: string; patientsProteges: number }
export interface Cellule { id: number; statut: 'ACTIVE' | 'CLOTUREE'; dateActivation: string }
export interface Decision { id: number; celluleId: number; libelle: string; auteur: string; timestamp: string }
export interface EvenementSeuil { id: number; niveau: Niveau; type: string; timestamp: string; journalId: number }

export interface EtatDetecteur {
  seuilEchecsAuth: number; fenetre: number; echecsAuthDansFenetre: number
  seuilIndisponibiliteSecondes: number; seuilAlterationsIntegrite: number
  seuilAccesMasseDossiers: number; fenetreAccesMasseSecondes: number; maxAccesDossiersParUtilisateur: number
}

export interface Indicateurs {
  premierSignal: string; declenchement: string; activation: string; premiereDecision: string | null
  cloture: string | null; typeDeclenchement: string; tempsDetectionSecondes: number; tempsActivationMs: number
  tempsPremiereDecisionSecondes: number | null; dureeCriseSecondes: number; dureeModeDegradeSecondes: number
  nombreDecisions: number; enCours: boolean
}

export interface Situation {
  niveau: 'NORMAL' | 'ALERTE' | 'CRISE'; celluleActive: Cellule | null; indicateurs: Indicateurs | null
  derniereCrise: Indicateurs | null; modeDegradeActif: boolean; detecteur: EtatDetecteur
  evenementsWarning24h: number; evenementsCritical24h: number
}

export interface Integrite { derniereVerification: string | null; fichiersSurveilles: number; alterations: string[] }

export interface LigneChronologie {
  debut: string; fin: string; niveau: Niveau; type: string; source: string; user: string | null
  message: string; occurrences: number
}
export interface RapportRex {
  genereLe: string; generePar: string; cellule: Cellule; indicateurs: Indicateurs
  chronologie: LigneChronologie[]; decisions: Decision[]; leconsAutomatiques: string[]; leconsEquipe: string[]
}
