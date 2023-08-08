FROM openjdk:17-jdk-alpine
MAINTAINER sltn011
EXPOSE 9889
COPY target/Auth-0.0.1-SNAPSHOT.jar auth.jar
ENTRYPOINT ["java","-jar","./auth.jar"]