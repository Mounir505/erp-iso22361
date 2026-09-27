# © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE.
# --- Étape 1 : compilation (nécessite Internet au moment du build uniquement) ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
# Les tests sont lancés à part (mvn test) pour garder un build d'image rapide
RUN mvn -q -B -DskipTests package

# --- Étape 2 : exécution (JRE seul, utilisateur non root) ---
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S erp && adduser -S erp -G erp \
    && mkdir -p /var/log/erp /var/lib/erp/documents \
    && chown -R erp:erp /var/log/erp /var/lib/erp
USER erp
WORKDIR /app
COPY --from=build /src/target/erp-core.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Duser.timezone=UTC", "-jar", "/app/app.jar"]
