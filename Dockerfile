# Etapa 1: compila el jar con el JDK completo
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw -q -DskipTests package

# Etapa 2: la imagen final solo lleva el JRE y el jar (más liviana, sin código fuente ni Maven)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx256m"
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
