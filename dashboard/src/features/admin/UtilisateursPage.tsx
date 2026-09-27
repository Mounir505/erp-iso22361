/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { useState, type FormEvent } from 'react'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { api } from '@/api/client'
import { useAction, useRoles, useUtilisateurs } from '@/api/hooks'
import type { Role, Utilisateur } from '@/api/types'
import { LIBELLES_ROLES } from '@/auth/roles'
import { PageHeader } from '@/components/PageHeader'
import { Chargement } from '@/components/EtatVide'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'

/** Gestion des comptes et des rôles (normaux et de crise). Tout changement de rôle est tracé en WARNING. */
export function UtilisateursPage() {
  const utilisateurs = useUtilisateurs()
  const [edition, setEdition] = useState<Utilisateur | 'nouveau' | null>(null)
  const supprimer = useAction((id: number) => api.delete(`/api/utilisateurs/${id}`), { succes: 'Utilisateur supprimé', invalider: [['utilisateurs']] })

  return (
    <>
      <PageHeader titre="Utilisateurs et rôles" description="Rôles métier et rôles de la cellule de crise (livrable 1.4)."
        actions={<Button onClick={() => setEdition('nouveau')}><Plus /> Nouvel utilisateur</Button>} />
      <Card>
        <CardContent>
          {utilisateurs.isPending ? <Chargement /> : (
            <Table>
              <TableHeader>
                <TableRow><TableHead>Nom</TableHead><TableHead>Identifiant</TableHead><TableHead>Rôle</TableHead><TableHead className="w-20"><span className="sr-only">Actions</span></TableHead></TableRow>
              </TableHeader>
              <TableBody>
                {utilisateurs.data?.map((u) => (
                  <TableRow key={u.id}>
                    <TableCell className="font-medium">{u.nom}</TableCell>
                    <TableCell className="font-mono text-xs">{u.login}</TableCell>
                    <TableCell>
                      <span className="flex items-center gap-2">{LIBELLES_ROLES[u.role]}{u.estRoleDeCrise && <Badge variant="outline">Cellule de crise</Badge>}</span>
                    </TableCell>
                    <TableCell className="whitespace-nowrap">
                      <Button variant="ghost" size="icon" aria-label="Modifier" onClick={() => setEdition(u)}><Pencil /></Button>
                      <Button variant="ghost" size="icon" aria-label="Supprimer" onClick={() => confirm(`Supprimer le compte ${u.login} ?`) && supprimer.mutate(u.id)}><Trash2 /></Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
      {edition && <UtilisateurForm utilisateur={edition === 'nouveau' ? undefined : edition} onFermer={() => setEdition(null)} />}
    </>
  )
}

function UtilisateurForm({ utilisateur, onFermer }: { utilisateur?: Utilisateur; onFermer: () => void }) {
  const roles = useRoles()
  const [nom, setNom] = useState(utilisateur?.nom ?? '')
  const [login, setLogin] = useState(utilisateur?.login ?? '')
  const [motDePasse, setMotDePasse] = useState('')
  const [role, setRole] = useState<Role>(utilisateur?.role ?? 'INFIRMIER')
  const enregistrer = useAction(
    () => {
      const corps = { nom, login, motDePasse: motDePasse || null, role }
      return utilisateur ? api.put(`/api/utilisateurs/${utilisateur.id}`, corps) : api.post('/api/utilisateurs', corps)
    },
    { succes: 'Utilisateur enregistré', invalider: [['utilisateurs']], onSuccess: onFermer },
  )

  function soumettre(e: FormEvent) {
    e.preventDefault()
    enregistrer.mutate(undefined)
  }

  return (
    <Dialog open onOpenChange={(o) => !o && onFermer()}>
      <DialogContent>
        <form onSubmit={soumettre} className="grid gap-4">
          <DialogHeader>
            <DialogTitle>{utilisateur ? 'Modifier l’utilisateur' : 'Nouvel utilisateur'}</DialogTitle>
            <DialogDescription>Le mot de passe est stocké haché (BCrypt), jamais en clair.</DialogDescription>
          </DialogHeader>
          <Field label="Nom" htmlFor="u-nom"><Input id="u-nom" value={nom} onChange={(e) => setNom(e.target.value)} required /></Field>
          <Field label="Identifiant" htmlFor="u-login"><Input id="u-login" value={login} onChange={(e) => setLogin(e.target.value)} required pattern="[a-zA-Z0-9._\-]+" minLength={3} /></Field>
          <Field label={utilisateur ? 'Nouveau mot de passe (laisser vide pour conserver)' : 'Mot de passe (8 caractères min.)'} htmlFor="u-mdp">
            <Input id="u-mdp" type="password" autoComplete="new-password" value={motDePasse} onChange={(e) => setMotDePasse(e.target.value)} required={!utilisateur} minLength={8} />
          </Field>
          <Field label="Rôle" htmlFor="u-role">
            <NativeSelect id="u-role" value={role} onChange={(e) => setRole(e.target.value as Role)}>
              {roles.data?.map((r) => <option key={r.id} value={r.libelle}>{LIBELLES_ROLES[r.libelle]}{r.estRoleDeCrise ? ' (crise)' : ''}</option>)}
            </NativeSelect>
          </Field>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onFermer}>Annuler</Button>
            <Button type="submit" disabled={enregistrer.isPending}>Enregistrer</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
