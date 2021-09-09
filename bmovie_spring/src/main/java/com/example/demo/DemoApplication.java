package com.example.demo;

import com.example.entrypoint.EntryPointDriver;
import com.example.generator.EventsGeneratorDriver;
import com.example.kafka.EventAggregatorDriver;
import com.example.kafka.EventProcessorDriver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

@SpringBootApplication
@ComponentScan(basePackages = "com.example")
public class DemoApplication {

	@PostConstruct
	public void start() {
		System.out.println("DemoApplication.start");
	}

	@PreDestroy
	public void stop() {
		System.out.println("DemoApplication.stop");
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		String prop = System.getProperty("mode", "restservice");

		if (prop.equals("restservice")) {
			SpringApplication.run(DemoApplication.class, args);
		} else if (prop.equals("grpcservice")) {
			BMovieGrpcDriver.run();
		} else if (prop.equals("entrypoint")) {
			EntryPointDriver.run();
		} else if (prop.equals("evprocessor")) {
			EventProcessorDriver.startConsumerThreads();
		} else if (prop.equals("evaggregator")) {
			EventAggregatorDriver.startConsumerThreads();
 		} else if (prop.equals("generator")) {
			EventsGeneratorDriver.generateEvents();
		} else {
			System.out.println("unknown mode");
		}
	}
}
