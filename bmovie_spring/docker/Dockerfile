FROM openjdk:14.0.2

ADD BMovieProj-0.0.1-SNAPSHOT.jar /tmp

ENV KAFKA_HOST
ENV REDIS_HOST
ENV MONGO_HOST

WORKDIR /tmp
ENTRYPOINT java -Dmode=evaggregator -jar BMovieProj-0.0.1-SNAPSHOT.jar com.example.demo.DemoApplication
