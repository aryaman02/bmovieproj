package com.example.entrypoint;

import java.io.IOException;

public class EntryPointDriver {
    public static void run() throws IOException {
        BMovieDataInserter bMovDataInserter = new BMovieDataInserter();
        bMovDataInserter.connect();
        bMovDataInserter.addIndexesInBMovieCollection();
        bMovDataInserter.storeBMovieDatainDB();
    }
}
