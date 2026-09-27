# Correspondance fonctionnalités ↔ ISO 22361:2022

> Les clauses sont citées d'après la synthèse *ISO22361_Contenu_Officiel.pdf* de l'atelier
> (structure officielle de la norme). Pour une citation mot à mot, se référer à l'exemplaire ISO / IMANOR.

| Fonctionnalité de l'ERP | Où dans le code | Clause ISO 22361 | Justification |
|---|---|---|---|
| Détecteur de seuils (échecs d'auth., indisponibilité, intégrité, accès en masse) | `crise/service/DetecteurDeSeuilService`, `SondeDisponibilite`, `ControleIntegrite` | **5.3.1** Anticipation · principe **C** (management des risques, surveillance continue) | Détecter les signaux faibles et qualifier le passage incident → crise (livrable 1.3). |
| Cartographie des actifs / scénario rançongiciel pris en compte dans les seuils | `application.yml` (`erp.crise.seuils`), V1/V2 Flyway | **5.3.2** Appréciation · **5.3.3** Prévention et atténuation | Les seuils traduisent l'appréciation des risques du livrable 1.2. |
| Journal d'audit JSON horodaté (base + `events.log`) | `crise/service/JournalAuditService`, `EvenementJournalDto` | **5.3.4.3** Gestion de l'information | Information fiable, horodatée (UTC), non perdue même si la base tombe (file d'attente). |
| Tableau de bord temps réel (`/health` 5 s, `/events` 2 s, situation, timeline, indicateurs) | `dashboard/src/features/crise/*`, `supervision/SupervisionController` | **5.3.4.4** Conscience partagée de la situation · principe **D** (décision fondée sur l'information) | Tous les membres de la CDC voient la même image de la situation. |
| Rôles de crise (pilote, technique, communication, secrétaire) et droits associés | `security/Roles`, V2 Flyway, `@PreAuthorize` | **5.2** Cadre (structure, chaîne d'autorité) · **5.3.4** composition et responsabilités de la CDC · principe **A** (gouvernance) | Rôles clairs, responsabilités désignées, moindre privilège. |
| Activation de la cellule de crise (automatique sur CRITICAL, ou manuelle par le pilote) | `CelluleDeCriseService.activerSurEvenement / activerManuellement` | **5.3.5** Réponse · **5.3.4.2** plan générique | L'activation manuelle permet de répondre à une crise non prévue par les seuils (plan non limité à des scénarios). |
| Registre de décisions horodatées | `Decision`, `CelluleDeCriseService.ajouterDecision`, `DecisionsCard` | **Art. 7** Prise de décision stratégique · principe **D** | Traçabilité de qui a décidé quoi et quand, y compris sous incertitude. |
| Auteur / porte-parole des décisions de communication | rôle `RESP_COMMUNICATION`, décisions consignées | **Art. 8** Communication de crise · principe **E** | Les messages (holding statements) sont décidés et tracés dans le registre. |
| Mode dégradé (lecture seule des dossiers des patients hospitalisés) | `ModeDegradeService`, `/status/degraded`, `ContinuitePage` | **5.3.5** Réponse (continuité) · principe **F** (éthique : sécurité des patients d'abord) | Les soins continuent : consultation garantie, écriture suspendue (livrable 1.5). |
| Sauvegarde / restauration (métier ou complète), contrôle SHA-256, reprise < 30 min | `scripts/backup.sh`, `scripts/restore.sh`, `docs/politique-sauvegarde.md` | **5.3.6** Rétablissement | Retour à la normale sur une base saine, en conservant les preuves de la crise. |
| Nouvelle référence d'intégrité après restauration, clôture de la crise | `ControleIntegrite.reinitialiserReference`, `CelluleDeCriseService.cloturer` | **5.3.6** Rétablissement (sortie de crise) | La clôture est une décision explicite du pilote. |
| Module REX (chronologie, temps de réponse, décisions, leçons ; PDF / JSON) | `rex/RexService`, `RexPdfExporter`, `RexPage` | **5.3.7** Amélioration continue · **9.6** Évaluation / apprentissage · principe **G** | Tirer les enseignements de l'exercice à partir de faits journalisés. |
| Simulateur d'exercice (drapeau désactivé par défaut) | `exercice/ExerciceController` | **Art. 9** Formation, validation par l'exercice | Déclencher un exercice réaliste sans attaque réelle. |
