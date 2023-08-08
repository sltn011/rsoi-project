FROM openjdk:17-jdk-alpine
MAINTAINER sltn011
EXPOSE 8100
COPY target/Stats-0.0.1-SNAPSHOT.jar stats.jar
ENTRYPOINT ["java","-jar","./stats.jar"]