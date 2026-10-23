# Build the image from the repository root:
#   docker build -f docker/Dockerfile -t rubenruiz7/tripmate:dev .

# Stage 1: build the React application.
FROM node:24-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: package the Spring Boot jar with the frontend build as static resources.
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /backend
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /frontend/dist ./src/main/resources/static
RUN mvn -B -q package -DskipTests && cp target/backend-*.jar /app.jar

# Stage 3: runtime with only the JRE and the jar. HTTPS on 443 with the keystore inside the jar.
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S tripmate && adduser -S -G tripmate tripmate
WORKDIR /app
COPY --from=backend /app.jar app.jar
USER tripmate
ENV SERVER_PORT=443
EXPOSE 443
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
