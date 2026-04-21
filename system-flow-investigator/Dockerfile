FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml /app/pom.xml
COPY flow-lab-producer /app/flow-lab-producer
COPY flow-lab-consumer-producer /app/flow-lab-consumer-producer
COPY flow-lab-consumer /app/flow-lab-consumer
COPY system-flow-investigator /app/system-flow-investigator
RUN mvn -q -pl system-flow-investigator -am clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/system-flow-investigator/target/*-jar-with-dependencies.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
