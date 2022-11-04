FROM openjdk:11
COPY target/scala-3.2.0/radioware-relay-server.jar /radioware-relay-server.jar
ENTRYPOINT ["java", "-jar", "radioware-relay-server.jar"]