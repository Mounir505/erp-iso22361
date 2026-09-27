/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import * as React from 'react'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

const alertVariants = cva(
  'relative grid w-full grid-cols-[0_1fr] items-start gap-y-0.5 rounded-lg border px-4 py-3 text-sm has-[>svg]:grid-cols-[calc(var(--spacing)*4)_1fr] has-[>svg]:gap-x-3 [&>svg]:size-4 [&>svg]:translate-y-0.5',
  {
    variants: {
      variant: {
        default: 'bg-card text-card-foreground',
        warning: 'border-level-warning/50 bg-level-warning/10 [&>svg]:text-ink-warning',
        critical: 'border-level-critical/50 bg-level-critical/10 [&>svg]:text-ink-critical',
      },
    },
    defaultVariants: { variant: 'default' },
  },
)

function Alert({ className, variant, ...props }: React.ComponentProps<'div'> & VariantProps<typeof alertVariants>) {
  return <div role="alert" className={cn(alertVariants({ variant }), className)} {...props} />
}
const AlertTitle = ({ className, ...p }: React.ComponentProps<'div'>) => <div className={cn('col-start-2 font-medium', className)} {...p} />
const AlertDescription = ({ className, ...p }: React.ComponentProps<'div'>) => (
  <div className={cn('col-start-2 text-sm text-muted-foreground', className)} {...p} />
)

export { Alert, AlertTitle, AlertDescription }
