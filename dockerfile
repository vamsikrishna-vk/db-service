FROM openjdk:17-oracle

WORKDIR /app

COPY target/db-service-0.0.1-SNAPSHOT.jar /app/app.jar

RUN touch metadata.txt tabledata.txt


EXPOSE 8080

CMD ["java", "-jar", "app.jar" ,"--dbservice.metadata.file=metadata.txt","--dbservice.data.file=tabledata.txt"]