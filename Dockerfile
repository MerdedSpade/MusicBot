FROM alpine
WORKDIR /home/app
RUN apk add git openjdk8 maven
ADD . /home/app
RUN sed -i "s/0.2.9-actions/$(git describe --tags)/g" pom.xml
RUN mvn package

CMD ["java", "-jar", "target/MusicBotW.jar"]
