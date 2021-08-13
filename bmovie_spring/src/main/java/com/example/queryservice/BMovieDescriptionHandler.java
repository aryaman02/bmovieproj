package com.example.queryservice;

import com.example.controller.BMovie;
import com.example.controller.BMovieQueriedInfo;
import com.example.demo.remote.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.BasicDBObject;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import org.bson.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BMovieDescriptionHandler extends BMovieDescriptionHandlerGrpc.BMovieDescriptionHandlerImplBase {
    private BMovieQueryServiceDriver bmService;

    public BMovieDescriptionHandler(BMovieQueryServiceDriver bmqsd) {
        bmService = bmqsd;
    }

    @Override
    public void getBMovieDetails(com.example.demo.remote.BMovieDescriptionRequest request,
        io.grpc.stub.StreamObserver<com.example.demo.remote.BMovieDescriptionResponse> responseObserver) {

        BMovieDescriptionResponse.Builder builder = BMovieDescriptionResponse.newBuilder();
        String imdbId = request.getImdbID();

        try {
            processClientRequest(imdbId, builder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private void processClientRequest(String imdbID, BMovieDescriptionResponse.Builder builder) throws IOException {
        String omdbURL = new HttpUrl.Builder() .scheme("https")
            .host("omdbapi.com").addQueryParameter("i", imdbID).addQueryParameter("apikey", "da14bb5c").build().toString();

        List<Document> query = bmService.getDatabase().getCollection("bMovieDataCol").find(new Document("imdbID", imdbID)).into(new ArrayList<>());

        if (query.size() > 0) {
            Document doc = query.get(0);
            String bMovieDescription = doc.getString("description");
            String bMovieTitle = doc.getString("title");
            int bMovieYear = doc.getInteger("yearOfRelease");
            int runtime = doc.getInteger("duration");
            double imdbRating = doc.getDouble("imdbRating");
            List<String> genres = (List<String>) doc.get("genres");
            List<String> actors = (List<String>) doc.get("actors");

            if (bMovieDescription != null) {
                //BMovie bMov = new BMovie(imdbID, bMovieTitle, bMovieYear, runtime, genres, actors, imdbRating, bMovieDescription);
                BMovieGRPC bmGRPC = BMovieGRPC.newBuilder().setIsValidBMovie(true).setImdbID(imdbID).setTitle(bMovieTitle).setYearOfRelease(bMovieYear).setDuration(runtime)
                    .addAllGenres(genres).addAllActors(actors).setImdbRating(imdbRating).setDescription(bMovieDescription).build();
                //builder.putResponse(imdbID, bmService.getJsonMapper().writeValueAsString(bMov));
                builder.putResponse(imdbID, bmGRPC);

            } else {
                Request request = new Request.Builder().url(omdbURL).build();
                Response response = bmService.getClient().newCall(request).execute();
                String bMovieInfo = response.body().string();
                response.close();

                StringBuilder sb = new StringBuilder();
                int descriptionIndex = bMovieInfo.indexOf(':', bMovieInfo.indexOf("Plot")) + 2;

                while (bMovieInfo.charAt(descriptionIndex) != '\"' || bMovieInfo.charAt(descriptionIndex+1) != ',') {
                    sb.append(bMovieInfo.charAt(descriptionIndex));
                    descriptionIndex++;
                }
                bMovieDescription = sb.toString();

                BasicDBObject newDocument = new BasicDBObject();
                newDocument.append("$set", new BasicDBObject().append("description", bMovieDescription));

                BasicDBObject searchQuery = new BasicDBObject().append("imdbID", imdbID);

                bmService.getDatabase().getCollection("bMovieDataCol").updateOne(searchQuery, newDocument);

                //BMovie bMov = new BMovie(imdbID, bMovieTitle, bMovieYear, runtime, genres, actors, imdbRating, bMovieDescription);
                //builder.putResponse(imdbID, bmService.getJsonMapper().writeValueAsString(bMov));
                BMovieGRPC bmGRPC = BMovieGRPC.newBuilder().setIsValidBMovie(true).setImdbID(imdbID).setTitle(bMovieTitle).setYearOfRelease(bMovieYear).setDuration(runtime)
                    .addAllGenres(genres).addAllActors(actors).setImdbRating(imdbRating).setDescription(bMovieDescription).build();

                builder.putResponse(imdbID, bmGRPC);
            }

        } else {
            String bMovieTitle = "", bMovieDescription = "";
            int bMovieYear = -1, duration = -1;
            double imdbRating = -1;
            List<String> genres = new ArrayList<>(), actors = new ArrayList<>();
            BMovie newBMov = null;

            Request request = new Request.Builder().url(omdbURL).build();
            Response response = bmService.getClient().newCall(request).execute();
            String bMovieInfo = response.body().string();
            response.close();
            boolean isValidBMovieRequest = false;

            if (bMovieInfo.equals("{\"Response\":\"False\",\"Error\":\"Error getting data.\"}")) {
                //builder.putResponse(imdbID, "null");
                builder.putResponse(imdbID, BMovieGRPC.newBuilder().setIsValidBMovie(false).setImdbID("").build());
                return;

            }

            if (bMovieInfo.indexOf("\"Type\":\"movie\"") != -1) {
                int yearIndex = bMovieInfo.indexOf(':', bMovieInfo.indexOf("Year")) + 2;

                StringBuilder sb = new StringBuilder();
                int i = yearIndex;
                char ch = bMovieInfo.charAt(i);

                while (ch != '\"') {
                    sb.append(ch);
                    i++;
                    ch = bMovieInfo.charAt(i);
                }
                int year = Integer.parseInt(sb.toString());

                if (bMovieInfo.indexOf("\"Country\":\"India\"") != -1 && bMovieInfo.indexOf("\"Language\":\"Hindi\"") != -1 && year >= 1990 && year <= 2009) {
                    isValidBMovieRequest = true;
                    bMovieYear = year;
                } else {
                    //builder.putResponse(imdbID, imdbID);
                    builder.putResponse(imdbID, BMovieGRPC.newBuilder().setIsValidBMovie(false).setImdbID(imdbID).build());
                    return;
                }
            } else {
                //builder.putResponse(imdbID, imdbID);
                builder.putResponse(imdbID, BMovieGRPC.newBuilder().setIsValidBMovie(false).setImdbID(imdbID).build());
                return;
            }

            if (isValidBMovieRequest) {
                StringBuilder sb = new StringBuilder();
                int titleIndex = bMovieInfo.indexOf(':');
                titleIndex += 2;

                while (bMovieInfo.charAt(titleIndex) != '\"') {
                    sb.append(bMovieInfo.charAt(titleIndex));
                    titleIndex++;
                }
                bMovieTitle = sb.toString();

                sb.delete(0, sb.length());
                int runtimeIndex = bMovieInfo.indexOf(':', bMovieInfo.indexOf("Runtime")) + 2;

                while (bMovieInfo.charAt(runtimeIndex) != '\"') {
                    sb.append(bMovieInfo.charAt(runtimeIndex));
                    runtimeIndex++;
                }
                if (!sb.toString().equals("N/A")) {
                    String[] arr = sb.toString().split(" ");
                    duration = Integer.parseInt(arr[0]);
                }

                sb.delete(0, sb.length());
                int ratingIndex = bMovieInfo.indexOf(':', bMovieInfo.indexOf("imdbRating")) + 2;

                while (bMovieInfo.charAt(ratingIndex) != '\"') {
                    sb.append(bMovieInfo.charAt(ratingIndex));
                    ratingIndex++;
                }

                if (!sb.toString().equals("N/A")) {
                    imdbRating = Double.parseDouble(sb.toString());
                }

                sb.delete(0, sb.length());
                int descriptionIndex = bMovieInfo.indexOf(':', bMovieInfo.indexOf("Plot")) + 2;

                while (bMovieInfo.charAt(descriptionIndex) != '\"' || bMovieInfo.charAt(descriptionIndex+1) != ',') {
                    sb.append(bMovieInfo.charAt(descriptionIndex));
                    descriptionIndex++;
                }
                bMovieDescription = sb.toString();

                sb.delete(0, sb.length());
                int genreIndex = bMovieInfo.indexOf(':', bMovieInfo.indexOf("Genre")) + 2;

                while (bMovieInfo.charAt(genreIndex) != '\"') {
                    sb.append(bMovieInfo.charAt(genreIndex));
                    genreIndex++;
                }
                String[] genreArr = sb.toString().split(",");
                for (String str : genreArr) {
                    genres.add(str.trim());
                }

                sb.delete(0, sb.length());
                int actorIndex = bMovieInfo.indexOf(':', bMovieInfo.indexOf("Actors")) + 2;

                while (bMovieInfo.charAt(actorIndex) != '\"') {
                    sb.append(bMovieInfo.charAt(actorIndex));
                    actorIndex++;
                }
                String[] actorArr = sb.toString().split(",");
                for (String str : actorArr) {
                    actors.add(str.trim());
                }
                newBMov = new BMovie(imdbID, bMovieTitle, bMovieYear, duration, genres, actors, imdbRating, bMovieDescription);
                String jsonStr = bmService.getJsonMapper().writeValueAsString(newBMov);
                Document doc = Document.parse(jsonStr);
                bmService.getDatabase().getCollection("bMovieDataCol").insertOne(doc);
            }
            //builder.putResponse(imdbID, bmService.getJsonMapper().writeValueAsString(newBMov));
            BMovieGRPC bmGRPC = BMovieGRPC.newBuilder().setIsValidBMovie(true).setImdbID(imdbID).setTitle(bMovieTitle).setYearOfRelease(bMovieYear).setDuration(duration)
                .addAllGenres(genres).addAllActors(actors).setImdbRating(imdbRating).setDescription(bMovieDescription).build();

            builder.putResponse(imdbID, bmGRPC);

            // update list of genres
            for (String g : genres) {
                bmService.updateBMovieGenres(g);
            }
        }
    }

    @Override
    public void getMoviesByRatingRange(com.example.demo.remote.BMovieRequestByRating request,
        io.grpc.stub.StreamObserver<com.example.demo.remote.BMovieResponse> responseObserver) {

        /*System.out.println("hello grpc");
        BMovieResponse.Builder builder = BMovieResponse.newBuilder();
        builder.putResponse("foo", "bar");
        BMovieResponse response = builder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();*/
    }
}

