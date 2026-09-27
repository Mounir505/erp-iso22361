/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import { useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { KeyRound, ShieldCheck } from 'lucide-react'
import { useAuth } from '@/auth/AuthContext'
import { accueil } from '@/auth/roles'
import { messageErreur } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Field } from '@/components/ui/field'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { COPYRIGHT } from '@/lib/copyright'

/** Comptes de démonstration (données fictives, mot de passe commun). */
const COMPTES = [
  ['pilote', 'Pilote de crise'],
  ['tech', 'Responsable technique'],
  ['com', 'Responsable communication'],
  ['secretaire', 'Secrétaire de crise'],
  ['dr.benali', 'Médecin'],
  ['inf.amrani', 'Infirmière'],
  ['adm.idrissi', "Agent d'admission"],
  ['pharma.tazi', 'Pharmacienne'],
  ['admin', 'Administrateur / animateur'],
] as const

export function LoginPage() {
  const { connecter } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [login, setLogin] = useState('')
  const [motDePasse, setMotDePasse] = useState('')
  const [erreur, setErreur] = useState<string | null>(null)
  const [envoi, setEnvoi] = useState(false)

  async function soumettre(e: FormEvent) {
    e.preventDefault()
    setEnvoi(true)
    setErreur(null)
    try {
      const u = await connecter(login.trim(), motDePasse)
      const depuis = (location.state as { depuis?: string } | null)?.depuis
      navigate(depuis ?? accueil(u.role), { replace: true })
    } catch (err) {
      setErreur(messageErreur(err))
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <div className="flex min-h-svh flex-col items-center justify-center gap-6 bg-muted/40 p-4">
      <div className="grid w-full max-w-4xl gap-6 md:grid-cols-[1fr_1fr]">
        <Card>
          <CardHeader className="flex-col">
            <div className="flex items-center gap-2.5">
              <img src="/favicon.svg" alt="" className="size-9" />
              <div>
                <CardTitle>ERP Hospitalier</CardTitle>
                <CardDescription className="mt-1">Gestion de crise ISO 22361 — lab isolé</CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <form onSubmit={soumettre} className="grid gap-4">
              <Field label="Identifiant" htmlFor="login">
                <Input id="login" autoComplete="username" value={login} onChange={(e) => setLogin(e.target.value)} required autoFocus />
              </Field>
              <Field label="Mot de passe" htmlFor="mdp">
                <Input id="mdp" type="password" autoComplete="current-password" value={motDePasse} onChange={(e) => setMotDePasse(e.target.value)} required />
              </Field>
              {erreur && (
                <Alert variant="critical">
                  <AlertDescription className="text-foreground">{erreur}</AlertDescription>
                </Alert>
              )}
              <Button type="submit" disabled={envoi}>
                <KeyRound /> {envoi ? 'Connexion…' : 'Se connecter'}
              </Button>
              <p className="text-xs text-muted-foreground">
                Chaque tentative est journalisée. Au-delà de 20 échecs par minute, le détecteur de seuils émet une alerte.
              </p>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex-col">
            <CardTitle className="flex items-center gap-2"><ShieldCheck className="size-4" /> Comptes de démonstration</CardTitle>
            <CardDescription>Mot de passe commun : <code className="rounded bg-muted px-1.5 py-0.5 font-mono text-foreground">Lab2026!</code></CardDescription>
          </CardHeader>
          <CardContent>
            <ul className="grid gap-1">
              {COMPTES.map(([l, r]) => (
                <li key={l}>
                  <button
                    type="button"
                    onClick={() => { setLogin(l); setMotDePasse('Lab2026!') }}
                    className="flex w-full items-center justify-between rounded-md px-2.5 py-1.5 text-left text-sm hover:bg-accent"
                  >
                    <span className="font-mono">{l}</span>
                    <span className="text-muted-foreground">{r}</span>
                  </button>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      </div>
      <footer className="text-center text-xs text-muted-foreground">{COPYRIGHT} · Données entièrement fictives</footer>
    </div>
  )
}
