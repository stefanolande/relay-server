FROM openjdk:11
COPY target/scala-2.13/radioware-relay-server.jar /radioware-relay-server.jar
ENTRYPOINT ["java", "-jar", "radioware-relay-server.jar"]