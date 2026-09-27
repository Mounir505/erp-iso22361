/* © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE. */
import * as React from 'react'
import { Tabs as T } from 'radix-ui'
import { cn } from '@/lib/utils'

const Tabs = ({ className, ...p }: React.ComponentProps<typeof T.Root>) => <T.Root className={cn('flex flex-col gap-3', className)} {...p} />
const TabsList = ({ className, ...p }: React.ComponentProps<typeof T.List>) => (
  <T.List className={cn('inline-flex h-9 w-fit items-center rounded-lg bg-muted p-[3px] text-muted-foreground', className)} {...p} />
)
const TabsTrigger = ({ className, ...p }: React.ComponentProps<typeof T.Trigger>) => (
  <T.Trigger
    className={cn(
      'inline-flex h-full items-center gap-1.5 rounded-md px-3 text-sm font-medium whitespace-nowrap transition-colors',
      'data-[state=active]:bg-background data-[state=active]:text-foreground data-[state=active]:shadow-sm dark:data-[state=active]:bg-input/40',
      className,
    )}
    {...p}
  />
)
const TabsContent = ({ className, ...p }: React.ComponentProps<typeof T.Content>) => <T.Content className={cn('outline-none', className)} {...p} />

export { Tabs, TabsList, TabsTrigger, TabsContent }
