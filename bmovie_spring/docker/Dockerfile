FROM openjdk:11

ADD BMovieProj-0.0.1-SNAPSHOT.jar /tmp

ENV KAFKA_HOST 0.0.0.0
ENV REDIS_HOST 0.0.0.0
ENV MONGO_HOST 0.0.0.0

WORKDIR /tmp
ENTRYPOINT java -Dmode=evaggregator -jar BMovieProj-0.0.1-SNAPSHOT.jar com.example.demo.DemoApplication
