package com.example.queryservice;

import com.example.controller.BMovieQueriedInfo;
import com.example.demo.remote.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class BMovieQueryHandler extends BMovieQueryHandlerGrpc.BMovieQueryHandlerImplBase {
    private BMovieQueryServiceDriver bmService;

    public BMovieQueryHandler(BMovieQueryServiceDriver bmqsd) {
        bmService = bmqsd;
    }

    private void processGenresANDActors(StringBuilder sb, String genres, String actors, MutableBoolean genreFlag, MutableBoolean actorFlag) {
        if (genres.equals("n.a.") || genres.equals("N.A.")) {
            genreFlag.setFalse();
        }
        if (actors.equals("n.a.") || actors.equals("N.A.")) {
            actorFlag.setFalse();
        }

        if (genreFlag.isTrue()) {
            sb.append(genres);
        }
        if (actorFlag.isTrue()) {
            sb.append(actors);
        }
    }

    @Override
    public void getMoviesByGeneralQuery(com.example.demo.remote.BMovieSimpleQueryRequest request,
        io.grpc.stub.StreamObserver<com.example.demo.remote.BMovieSimpleQueryResponse> responseObserver) {

        BMovieSimpleQueryResponse.Builder builder = BMovieSimpleQueryResponse.newBuilder();
        String bMovieGenres = request.getGenres();
        String bMovieActors = request.getActors();

        try {
            processGeneralQuery(bMovieGenres, bMovieActors, builder);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private void processGeneralQuery(String genres, String actors, BMovieSimpleQueryResponse.Builder builder)
        throws JsonProcessingException {
        StringBuilder sb = new StringBuilder();

        MutableBoolean genreFlag = new MutableBoolean(true);
        MutableBoolean actorFlag = new MutableBoolean(true);
        processGenresANDActors(sb, genres, actors, genreFlag, actorFlag);

        String[] genreArr = genres.split(",");
        String[] actorArr = actors.split(",");

        if (genreFlag.isTrue()) {
            for (String str : genreArr) {
                if (!bmService.isValidGenre(str.trim())) {
                    builder.putResponse(sb.toString(), QueriedResultsGRPC.newBuilder().setFlag(false).build());
                    return;
                }
            }
        }

        List<Document> queryInputs = new ArrayList<>();

        if (actorFlag.isTrue()) {
            for (String str : actorArr) {
                queryInputs.add(new Document("actors", str.trim()));
            }
        }
        if (genreFlag.isTrue()) {
            for (String str : genreArr) {
                queryInputs.add(new Document("genres", str.trim()));
            }
        }

        Document bMovieQuery = new Document("$and", queryInputs);
        List<Document> queryDocs = bmService.getDatabase().getCollection("bMovieDataCol").find(bMovieQuery).into(new ArrayList<>());

        if (queryDocs.size() == 0) {
            builder.putResponse(sb.toString(), QueriedResultsGRPC.newBuilder().setFlag(true).addAllImdbIDs(new ArrayList<>()).build());
            return;
        }
        List<String> imdbIdResults = new ArrayList<>();

        for (Document doc : queryDocs) {
            String imdbID = doc.getString("imdbID");
            imdbIdResults.add(imdbID);
        }
        builder.putResponse(sb.toString(), QueriedResultsGRPC.newBuilder().setFlag(true).addAllImdbIDs(imdbIdResults).build());
    }

    @Override
    public void getMoviesBySingleQuery(com.example.demo.remote.BMovieSingleRequest request,
        io.grpc.stub.StreamObserver<com.example.demo.remote.BMovieSingleResponse> responseObserver) {

        BMovieSingleResponse.Builder builder = BMovieSingleResponse.newBuilder();

        String queryType = request.getQueryType();
        double lowerBound = request.getMin();
        double upperBound = request.getMax();
        String bMovieGenres = request.getGenres();
        String bMovieActors = request.getActors();

        try {
            processSingleCriteriaQuery(queryType, lowerBound, upperBound, bMovieGenres, bMovieActors, builder);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private void processSingleCriteriaQuery(String qType, double min, double max, String genres, String actors, BMovieSingleResponse.Builder builder)
        throws JsonProcessingException {

        StringBuilder sb = new StringBuilder();
        sb.append(min);
        sb.append(max);

        MutableBoolean genreFlag = new MutableBoolean(true);
        MutableBoolean actorFlag = new MutableBoolean(true);
        processGenresANDActors(sb, genres, actors, genreFlag, actorFlag);

        String[] genreArr = genres.split(",");
        String[] actorArr = actors.split(",");

        if (genreFlag.isTrue()) {
            for (String str : genreArr) {
                if (!bmService.isValidGenre(str.trim())) {
                    builder.putResponse(sb.toString(), QueriedResultsGRPC.newBuilder().setFlag(false).build());
                    return;
                }
            }
        }
        List<Document> queryInputs = new ArrayList<>();

        if (actorFlag.isTrue()) {
            for (String str : actorArr) {
                queryInputs.add(new Document("actors", str.trim()));
            }
        }
        if (genreFlag.isTrue()) {
            for (String str : genreArr) {
                queryInputs.add(new Document("genres", str.trim()));
            }
        }

        if (qType.equals("Rating")) {
            queryInputs.add(new Document("imdbRating", new Document("$gte", min)));
            queryInputs.add(new Document("imdbRating", new Document("$lte", max)));
        } else if (qType.equals("Runtime")) {
            queryInputs.add(new Document("duration", new Document("$gte", min)));
            queryInputs.add(new Document("duration", new Document("$lte", max)));
        } else {
            queryInputs.add(new Document("yearOfRelease", new Document("$gte", min)));
            queryInputs.add(new Document("yearOfRelease", new Document("$lte", max)));
        }

        Document bMovieQuery = new Document("$and", queryInputs);
        List<Document> queryDocs = bmService.getDatabase().getCollection("bMovieDataCol").find(bMovieQuery).into(new ArrayList<>());

        if (queryDocs.size() == 0) {
            builder.putResponse(sb.toString(), QueriedResultsGRPC.newBuilder().setFlag(true).addAllImdbIDs(new ArrayList<>()).build());
            return;
        }
        List<String> imdbIdResults = new ArrayList<>();

        for (Document doc : queryDocs) {
            String imdbID = doc.getString("imdbID");
            imdbIdResults.add(imdbID);
        }
        builder.putResponse(sb.toString(), QueriedResultsGRPC.newBuilder().setFlag(true).addAllImdbIDs(imdbIdResults).build());
    }

    @Override
    public void getMoviesByDoubleQuery(com.example.demo.remote.BMovieDoubleRequest request,
        io.grpc.stub.StreamObserver<com.example.demo.remote.BMovieDoubleResponse> responseObserver) {

        BMovieDoubleResponse.Builder builder = BMovieDoubleResponse.newBuilder();

        String queryType1 = request.getQueryType1();
        String queryType2 = request.getQueryType2();

        double lowerBound1 = request.getMin1();
        double upperBound1 = request.getMax1();
        double lowerBound2 = request.getMin2();
        double upperBound2 = request.getMax2();

        String bMovieGenres = request.getGenres();
        String bMovieActors = request.getActors();

        try {
            processDoubleCriteriaQuery(queryType1, queryType2, lowerBound1, upperBound1, lowerBound2, upperBound2, bMovieGenres, bMovieActors, builder);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private void processDoubleCriteriaQuery(String qType1, String qType2, double min1, double max1, double min2, double max2,
        String genres, String actors, BMovieDoubleResponse.Builder builder) throws JsonProcessingException {

        StringBuilder sb = new StringBuilder();
        sb.append(min1);
        sb.append(max1);
        sb.append(min2);
        sb.append(max2);

        MutableBoolean genreFlag = new MutableBoolean(true);
        MutableBoolean actorFlag = new MutableBoolean(true);
        processGenresANDActors(sb, genres, actors, genreFlag, actorFlag);

        String[] genreArr = genres.split(",");
        String[] actorArr = actors.split(",");

        if (genreFlag.isTrue()) {
            for (String str : genreArr) {
                if (!bmService.isValidGenre(str.trim())) {
                    builder.putResponse(sb.toString(), QueriedResultsGRPC.newBuilder().setFlag(false).build());
                    return;
                }
            }
        }
        List<Document> queryInputs = new ArrayList<>();

        if (actorFlag.isTrue()) {
            for (String str : actorArr) {
                queryInputs.add(new Document("actors", str.trim()));
            }
        }
        if (genreFlag.isTrue()) {
            for (String str : genreArr) {
                queryInputs.add(new Document("genres", str.trim()));
            }
        }

        if (qType1.equals("Rating")) {
            queryInputs.add(new Document("imdbRating", new Document("$gte", min1)));
            queryInputs.add(new Document("imdbRating", new Document("$lte", max1)));
        } else if (qType1.equals("Runtime")) {
            queryInputs.add(new Document("duration", new Document("$gte", min1)));
            queryInputs.add(new Document("duration", new Document("$lte", max1)));
        } else {
            queryInputs.add(new Document("yearOfRelease", new Document("$gte", min1)));
            queryInputs.add(new Document("yearOfRelease", new Document("$lte", max1)));
        }

        if (qType2.equals("Rating")) {
            queryInputs.add(new Document("imdbRating", new Document("$gte", min2)));
            queryInputs.add(new Document("imdbRating", new Document("$lte", max2)));
        } else if (qType2.equals("Runtime")) {
            queryInputs.add(new Document("duration", new Document("$gte", min2)));
            queryInputs.add(new Document("duration", new Document("$lte", max2)));
        } else {
            queryInputs.add(new Document("yearOfRelease", new Document("$gte", min2)));
            queryInputs.add(new Document("yearOfRelease", new Document("$lte", max2)));
        }

        Document bMovieQuery = new Document("$and", queryInputs);
        List<Document> queryDocs = bmService.getDatabase().getCollection("bMovieDataCol").find(bMovieQuery).into(new ArrayList<>());

        if (queryDocs.size() == 0) {
            builder.putResponse(sb.toString(), QueriedResultsGRPC.newBuilder().setFlag(true).addAllImdbIDs(new ArrayList<>()).build());
            return;
        }
        List<String> imdbIdResults = new ArrayList<>();

        for (Document doc : queryDocs) {
            String imdbID = doc.getString("imdbID");
            imdbIdResults.add(imdbID);
        }
        builder.putResponse(sb.toString(), QueriedResultsGRPC.newBuilder().setFlag(true).addAllImdbIDs(imdbIdResults).build());
    }

    @Override
    public void getMoviesByTripleQuery(com.example.demo.remote.BMovieTripleRequest request,
        io.grpc.stub.StreamObserver<com.example.demo.remote.BMovieTripleResponse> responseObserver) {

        BMovieTripleResponse.Builder builder = BMovieTripleResponse.newBuilder();

        double lowerBound1 = request.getMin1();
        double upperBound1 = request.getMax1();
        double lowerBound2 = request.getMin2();
        double upperBound2 = request.getMax2();
        double lowerBound3 = request.getMin3();
        double upperBound3 = request.getMax3();

        String bMovieGenres = request.getGenres();
        String bMovieActors = request.getActors();

        try {
            processTripleCriteriaQuery(lowerBound1, upperBound1, lowerBound2, upperBound2, lowerBound3,
                upperBound3, bMovieGenres, bMovieActors, builder);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private void processTripleCriteriaQuery(double min1, double max1, double min2, double max2,
        double min3, double max3, String genres, String actors, BMovieTripleResponse.Builder builder) throws JsonProcessingException {

        StringBuilder sb = new StringBuilder();
        sb.append(min1);
        sb.append(max1);
        sb.append(min2);
        sb.append(max2);
        sb.append(min3);
        sb.append(max3);

        MutableBoolean genreFlag = new MutableBoolean(true);
        MutableBoolean actorFlag = new MutableBoolean(true);
        processGenresANDActors(sb, genres, actors, genreFlag, actorFlag);

        String[] genreArr = genres.split(",");
        String[] actorArr = actors.split(",");

        if (genreFlag.isTrue()) {
            for (String str : genreArr) {
                if (!bmService.isValidGenre(str.trim())) {
                    builder.putResponse(sb.toString(), QueriedResultsGRPC.newBuilder().setFlag(false).build());
                    return;
                }
            }
        }
        List<Document> queryInputs = new ArrayList<>();

        if (actorFlag.isTrue()) {
            for (String str : actorArr) {
                queryInputs.add(new Document("actors", str.trim()));
            }
        }
        if (genreFlag.isTrue()) {
            for (String str : genreArr) {
                queryInputs.add(new Document("genres", str.trim()));
            }
        }

        queryInputs.add(new Document("duration", new Document("$gte", min1)));
        queryInputs.add(new Document("duration", new Document("$lte", max1)));
        queryInputs.add(new Document("imdbRating", new Document("$gte", min2)));
        queryInputs.add(new Document("imdbRating", new Document("$lte", max2)));
        queryInputs.add(new Document("yearOfRelease", new Document("$gte", min3)));
        queryInputs.add(new Document("yearOfRelease", new Document("$lte", max3)));

        Document bMovieQuery = new Document("$and", queryInputs);
        List<Document> queryDocs = bmService.getDatabase().getCollection("bMovieDataCol").find(bMovieQuery).into(new ArrayList<>());

        if (queryDocs.size() == 0) {
            builder.putResponse(sb.toString(), QueriedResultsGRPC.newBuilder().setFlag(true).addAllImdbIDs(new ArrayList<>()).build());
            return;
        }
        List<String> imdbIdResults = new ArrayList<>();

        for (Document doc : queryDocs) {
            String imdbID = doc.getString("imdbID");
            imdbIdResults.add(imdbID);
        }
        builder.putResponse(sb.toString(), QueriedResultsGRPC.newBuilder().setFlag(true).addAllImdbIDs(imdbIdResults).build());
    }
}

