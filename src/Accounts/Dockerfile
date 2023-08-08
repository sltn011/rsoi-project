FROM openjdk:17-jdk-alpine
MAINTAINER sltn011
EXPOSE 8010
COPY target/Accounts-0.0.1-SNAPSHOT.jar accounts.jar
ENTRYPOINT ["java","-jar","./accounts.jar"]