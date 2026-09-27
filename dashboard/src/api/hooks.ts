/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { useMutation, useQuery, useQueryClient, type QueryKey } from '@tanstack/react-query'
import { toast } from 'sonner'
import { api, messageErreur } from './client'
import type {
  Admission, Cellule, Decision, DossierMedical, EtatDetecteur, EvenementJournal, EvenementSeuil, Integrite,
  ModeDegrade, Patient, Prescription, RapportRex, RoleDto, Sante, Situation, StatutPatient, Utilisateur,
} from './types'

/** Fréquences de polling du contrat d'interface (livrable 2.4). */
export const POLL_HEALTH_MS = 5_000
export const POLL_EVENTS_MS = 2_000

// ------------------------------------------------------------- supervision --

/** GET /health toutes les 5 s. Un 503 reste une réponse exploitable (status DOWN). */
export function useSante() {
  return useQuery({
    queryKey: ['health'],
    queryFn: async (): Promise<Sante | null> => {
      try {
        const res = await fetch('/health')
        return (await res.json()) as Sante
      } catch {
        return null // ERP injoignable (conteneur arrêté, réseau coupé)
      }
    },
    refetchInterval: POLL_HEALTH_MS,
    refetchIntervalInBackground: true,
  })
}

/**
 * GET /events toutes les 2 s, en mode incrémental (?apresId=dernier id reçu) :
 * seules les nouvelles lignes transitent ; le flux est conservé côté client.
 */
const MAX_LIGNES_FLUX = 500
// Tampon unique du flux (partagé par tous les composants abonnés à ['events'])
let flux: EvenementJournal[] = []
let dernierId = 0

/** À appeler à la déconnexion : le flux d'un utilisateur n'est pas visible du suivant. */
export function reinitialiserFlux() {
  flux = []
  dernierId = 0
}

export function useFluxEvenements() {
  return useQuery({
    queryKey: ['events'],
    queryFn: async () => {
      const nouveaux = await api.get<EvenementJournal[]>(`/events?apresId=${dernierId}&limite=200`)
      if (nouveaux.length > 0) {
        dernierId = Math.max(dernierId, ...nouveaux.map((e) => e.id ?? 0))
        flux = [...nouveaux, ...flux].slice(0, MAX_LIGNES_FLUX) // plus récent d'abord
      }
      return flux
    },
    refetchInterval: POLL_EVENTS_MS,
    refetchIntervalInBackground: true,
  })
}

export const useSituation = () =>
  useQuery({ queryKey: ['situation'], queryFn: () => api.get<Situation>('/api/crise/situation'), refetchInterval: POLL_EVENTS_MS })

export const useModeDegrade = () =>
  useQuery({ queryKey: ['mode-degrade'], queryFn: () => api.get<ModeDegrade>('/status/degraded'), refetchInterval: POLL_HEALTH_MS })

export const useCellules = () => useQuery({ queryKey: ['cellules'], queryFn: () => api.get<Cellule[]>('/api/crise/cellules'), refetchInterval: POLL_HEALTH_MS })

export const useDecisions = (celluleId?: number) =>
  useQuery({
    queryKey: ['decisions', celluleId],
    queryFn: () => api.get<Decision[]>(`/api/crise/cellules/${celluleId}/decisions`),
    enabled: celluleId != null,
    refetchInterval: POLL_EVENTS_MS,
  })

export const useEvenementsSeuil = () =>
  useQuery({ queryKey: ['evenements-seuil'], queryFn: () => api.get<EvenementSeuil[]>('/api/crise/evenements'), refetchInterval: POLL_HEALTH_MS })

export const useDetecteur = () => useQuery({ queryKey: ['detecteur'], queryFn: () => api.get<EtatDetecteur>('/api/crise/detecteur'), refetchInterval: POLL_EVENTS_MS })

export const useIntegrite = () => useQuery({ queryKey: ['integrite'], queryFn: () => api.get<Integrite>('/api/crise/integrite'), refetchInterval: POLL_HEALTH_MS })

export const useExerciceStatut = () =>
  useQuery({ queryKey: ['exercice'], queryFn: () => api.get<{ enabled: boolean }>('/api/exercice/statut'), staleTime: Infinity })

export const useRex = (celluleId?: number) =>
  useQuery({ queryKey: ['rex', celluleId], queryFn: () => api.get<RapportRex>(`/api/rex/${celluleId}`), enabled: celluleId != null })

// ------------------------------------------------------------------ métier --

export const usePatients = (q: string, statut: StatutPatient | '') =>
  useQuery({
    queryKey: ['patients', q, statut],
    queryFn: () => api.get<Patient[]>(`/api/patients?${new URLSearchParams({ ...(q && { q }), ...(statut && { statut }) })}`),
  })

export const usePatient = (id: number) => useQuery({ queryKey: ['patient', id], queryFn: () => api.get<Patient>(`/api/patients/${id}`) })

export const useDossier = (patientId: number, enabled = true) =>
  useQuery({ queryKey: ['dossier', patientId], queryFn: () => api.get<DossierMedical>(`/api/patients/${patientId}/dossier`), enabled })

export const useDossiersHospitalises = () =>
  useQuery({ queryKey: ['dossiers-hospitalises'], queryFn: () => api.get<DossierMedical[]>('/api/dossiers/hospitalises') })

export const useAdmissions = (patientId?: number, enabled = true) =>
  useQuery({
    queryKey: ['admissions', patientId ?? 'toutes'],
    queryFn: () => api.get<Admission[]>(`/api/admissions${patientId ? `?patientId=${patientId}` : ''}`),
    enabled,
  })

export const usePrescriptions = (admissionId?: number) =>
  useQuery({
    queryKey: ['prescriptions', admissionId ?? 'toutes'],
    queryFn: () => api.get<Prescription[]>(`/api/prescriptions${admissionId ? `?admissionId=${admissionId}` : ''}`),
  })

export const useUtilisateurs = () => useQuery({ queryKey: ['utilisateurs'], queryFn: () => api.get<Utilisateur[]>('/api/utilisateurs') })
export const useRoles = () => useQuery({ queryKey: ['roles'], queryFn: () => api.get<RoleDto[]>('/api/roles'), staleTime: Infinity })

// --------------------------------------------------------------- mutations --

/**
 * Mutation générique : toast de succès / d'erreur et invalidation des requêtes concernées.
 */
export function useAction<TVars, TRes = unknown>(
  fn: (v: TVars) => Promise<TRes>,
  opts: { succes?: string; invalider?: QueryKey[]; onSuccess?: (r: TRes) => void } = {},
) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: (r) => {
      if (opts.succes) toast.success(opts.succes)
      opts.invalider?.forEach((k) => qc.invalidateQueries({ queryKey: k }))
      opts.onSuccess?.(r)
    },
    onError: (e) => toast.error(messageErreur(e)),
  })
}
