FROM openjdk:8-alpine
FROM maven
WORKDIR /home/app
ADD . /home/app
RUN mvn package

CMD ["java", "-jar", "target/MusicBotW.jar"]
