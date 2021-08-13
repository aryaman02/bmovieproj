package com.example.demo;

import com.example.queryservice.BMovieQueryServiceDriver;

public class BMovieGrpcDriver {
    public static void run() {
        System.out.println("Starting GrpcService");
        BMovieQueryServiceDriver queryServiceDriver = new BMovieQueryServiceDriver();
        queryServiceDriver.start();
        queryServiceDriver.populateGenres();
        System.out.println("Press Ctl-C to stop");
        queryServiceDriver.waitUntilShutdown();
        queryServiceDriver.stop();
    }
}
