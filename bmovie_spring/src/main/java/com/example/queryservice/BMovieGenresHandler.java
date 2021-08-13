package com.example.queryservice;

import com.example.demo.remote.BMovieGenreResponse;
import com.example.demo.remote.BMovieGenresGRPC;
import com.example.demo.remote.BMovieGenresHandlerGrpc;
import com.example.demo.remote.QueriedResultsGRPC;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BMovieGenresHandler extends BMovieGenresHandlerGrpc.BMovieGenresHandlerImplBase {
    private BMovieQueryServiceDriver bmService;

    public BMovieGenresHandler(BMovieQueryServiceDriver bmqsd) {
        bmService = bmqsd;
    }

    @Override
    public void getBMovieGenres(com.example.demo.remote.BMovieGenreRequest request,
        io.grpc.stub.StreamObserver<com.example.demo.remote.BMovieGenreResponse> responseObserver) {

        BMovieGenreResponse.Builder builder = BMovieGenreResponse.newBuilder();

        // Assuming that the list of genres is populated
        Set<String> bMovieGenres = bmService.getBMovieGenres();
        //List<String> listOfGenres = new ArrayList<>(bMovieGenres);

        builder.putResponse("genres", BMovieGenresGRPC.newBuilder().addAllGenres(bMovieGenres).build());

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}

