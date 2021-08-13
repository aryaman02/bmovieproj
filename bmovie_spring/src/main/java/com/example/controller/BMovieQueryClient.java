package com.example.controller;

import com.example.demo.remote.*;
import com.example.queryservice.BMovieQueryServiceDriver;
import com.example.queryservice.GrpcChannelManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.List;
import java.util.Map;

public class BMovieQueryClient {
    private final ObjectMapper m = new ObjectMapper();
    private static final String GRPC_HOST = "0.0.0.0";

    public BMovie findBMovieDetails(String imdbID) {
        ManagedChannel channel = GrpcChannelManager.getInstance().getChannel(GRPC_HOST,
            BMovieQueryServiceDriver.GRPC_PORT);

        if (channel != null) {
            try {
                BMovieDescriptionHandlerGrpc.BMovieDescriptionHandlerBlockingStub stub = BMovieDescriptionHandlerGrpc.newBlockingStub(channel);
                BMovieDescriptionRequest request = BMovieDescriptionRequest.newBuilder().setImdbID(imdbID).build();

                BMovieDescriptionResponse response = stub.getBMovieDetails(request);
                Map<String, BMovieGRPC> responseMap = response.getResponseMap();

                BMovieGRPC bmGRPCResponse = responseMap.get(imdbID);

                if (imdbID.equals(bmGRPCResponse.getImdbID()) && !bmGRPCResponse.getIsValidBMovie()) {
                    return new BMovie(imdbID);
                } else if (bmGRPCResponse.getImdbID().equals("") && !bmGRPCResponse.getIsValidBMovie()) {
                    return new BMovie("");
                }
                String bMovieTitle = bmGRPCResponse.getTitle();
                int bMovieYear = bmGRPCResponse.getYearOfRelease();
                int bMovieRuntime = bmGRPCResponse.getDuration();
                List<String> genres = bmGRPCResponse.getGenresList();
                List<String> actors = bmGRPCResponse.getActorsList();
                double bMovieRating = bmGRPCResponse.getImdbRating();
                String bMovieDescription = bmGRPCResponse.getDescription();

                return new BMovie(imdbID, bMovieTitle, bMovieYear, bMovieRuntime, genres, actors, bMovieRating, bMovieDescription);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } else {
        }
        return null;
    }

    public BMovieQueriedInfo getStatusOfBMovieImg(String imdbID) {
        ManagedChannel channel = GrpcChannelManager.getInstance().getChannel("0.0.0.0",
            BMovieQueryServiceDriver.GRPC_PORT);

        if (channel != null) {
            try {
                BMovieImgHandlerGrpc.BMovieImgHandlerBlockingStub stub = BMovieImgHandlerGrpc.newBlockingStub(channel);
                BMovieImgRequest request = BMovieImgRequest.newBuilder().setImdbID(imdbID).build();

                BMovieImgResponse response = stub.getImgOfBMovie(request);
                Map<String, QueriedResultsGRPC> responseMap = response.getResponseMap();

                QueriedResultsGRPC bMovieImgStatus = responseMap.get(imdbID);

                if (!bMovieImgStatus.getFlag()) {
                    return new BMovieQueriedInfo(false);
                } else if (bMovieImgStatus.getFlag() && bMovieImgStatus.getImdbIDsList().size() == 0) {
                    return new BMovieQueriedInfo(true);
                }
                BMovieQueriedInfo bMovImdbID = new BMovieQueriedInfo(true);
                bMovImdbID.addImdbID(imdbID);
                return bMovImdbID;
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } else {
        }
        return null;
    }

    private void determineGenreANDActorChoice(StringBuilder sb, String genres, String actors, MutableBoolean genreFlag, MutableBoolean actorFlag) {
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

    public BMovieQueriedInfo retrieveGeneralQueriedResults(String genres, String actors) {
        ManagedChannel channel = GrpcChannelManager.getInstance().getChannel("0.0.0.0",
            BMovieQueryServiceDriver.GRPC_PORT);

        if (channel != null) {
            try {
                BMovieQueryHandlerGrpc.BMovieQueryHandlerBlockingStub stub = BMovieQueryHandlerGrpc.newBlockingStub(channel);
                BMovieSimpleQueryRequest request = BMovieSimpleQueryRequest.newBuilder().setGenres(genres).setActors(actors).build();

                BMovieSimpleQueryResponse response = stub.getMoviesByGeneralQuery(request);
                Map<String, QueriedResultsGRPC> responseMap = response.getResponseMap();

                MutableBoolean genreFlag = new MutableBoolean(true), actorFlag = new MutableBoolean(true);
                StringBuilder sb = new StringBuilder();

                determineGenreANDActorChoice(sb, genres, actors, genreFlag, actorFlag);

                QueriedResultsGRPC resultsOfQuery = responseMap.get(sb.toString());

                if (!resultsOfQuery.getFlag()) {
                    return new BMovieQueriedInfo(false);
                } else if (resultsOfQuery.getFlag() && resultsOfQuery.getImdbIDsList().size() == 0) {
                    return new BMovieQueriedInfo(true);
                }
                BMovieQueriedInfo imdbResults = new BMovieQueriedInfo(true);
                imdbResults.setImdbIDs(resultsOfQuery.getImdbIDsList());
                return imdbResults;
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } else {
        }
        return null;
    }

    public BMovieQueriedInfo retrieveSingleQueriedResults(String singleCriteriaType, double min, double max, String genres, String actors) {
        ManagedChannel channel = GrpcChannelManager.getInstance().getChannel("0.0.0.0",
            BMovieQueryServiceDriver.GRPC_PORT);

        if (channel != null) {
            try {
                BMovieQueryHandlerGrpc.BMovieQueryHandlerBlockingStub stub = BMovieQueryHandlerGrpc.newBlockingStub(channel);
                BMovieSingleRequest request = BMovieSingleRequest.newBuilder().setQueryType(singleCriteriaType).setMin(min).setMax(max).setGenres(genres)
                    .setActors(actors).build();

                BMovieSingleResponse response = stub.getMoviesBySingleQuery(request);
                Map<String, QueriedResultsGRPC> responseMap = response.getResponseMap();

                StringBuilder sb = new StringBuilder();
                sb.append(min);
                sb.append(max);

                MutableBoolean genreFlag = new MutableBoolean(true), actorFlag = new MutableBoolean(true);
                determineGenreANDActorChoice(sb, genres, actors, genreFlag, actorFlag);

                QueriedResultsGRPC resultsOfQuery = responseMap.get(sb.toString());

                if (!resultsOfQuery.getFlag()) {
                    return new BMovieQueriedInfo(false);
                } else if (resultsOfQuery.getFlag() && resultsOfQuery.getImdbIDsList().size() == 0) {
                    return new BMovieQueriedInfo(true);
                }
                BMovieQueriedInfo imdbResults = new BMovieQueriedInfo(true);
                imdbResults.setImdbIDs(resultsOfQuery.getImdbIDsList());
                return imdbResults;
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } else {
        }
        return null;
    }

    public BMovieQueriedInfo retrieveDoubleQueriedResults(String criteria1, String criteria2, double min1, double max1, double min2,
        double max2, String genres, String actors) {

        ManagedChannel channel = GrpcChannelManager.getInstance().getChannel("0.0.0.0",
            BMovieQueryServiceDriver.GRPC_PORT);

        if (channel != null) {
            try {
                BMovieQueryHandlerGrpc.BMovieQueryHandlerBlockingStub stub = BMovieQueryHandlerGrpc.newBlockingStub(channel);
                BMovieDoubleRequest request = BMovieDoubleRequest.newBuilder().setQueryType1(criteria1).setQueryType2(criteria2).setMin1(min1)
                    .setMax1(max1).setMin2(min2).setMax2(max2).setGenres(genres).setActors(actors).build();

                BMovieDoubleResponse response = stub.getMoviesByDoubleQuery(request);
                Map<String, QueriedResultsGRPC> responseMap = response.getResponseMap();

                StringBuilder sb = new StringBuilder();
                sb.append(min1);
                sb.append(max1);
                sb.append(min2);
                sb.append(max2);

                MutableBoolean genreFlag = new MutableBoolean(true), actorFlag = new MutableBoolean(true);
                determineGenreANDActorChoice(sb, genres, actors, genreFlag, actorFlag);

                QueriedResultsGRPC resultsOfQuery = responseMap.get(sb.toString());

                if (!resultsOfQuery.getFlag()) {
                    return new BMovieQueriedInfo(false);
                } else if (resultsOfQuery.getFlag() && resultsOfQuery.getImdbIDsList().size() == 0) {
                    return new BMovieQueriedInfo(true);
                }
                BMovieQueriedInfo imdbResults = new BMovieQueriedInfo(true);
                imdbResults.setImdbIDs(resultsOfQuery.getImdbIDsList());
                return imdbResults;
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } else {
        }
        return null;
    }

    public BMovieQueriedInfo retrieveTripleQueriedResults(double min1, double max1, double min2, double max2, double min3, double max3, String genres, String actors) {
        ManagedChannel channel = GrpcChannelManager.getInstance().getChannel("0.0.0.0",
            BMovieQueryServiceDriver.GRPC_PORT);

        if (channel != null) {
            try {
                BMovieQueryHandlerGrpc.BMovieQueryHandlerBlockingStub stub = BMovieQueryHandlerGrpc.newBlockingStub(channel);
                BMovieTripleRequest request = BMovieTripleRequest.newBuilder().setMin1(min1).setMax1(max1).setMin2(min2).setMax2(max2)
                    .setMin3(min3).setMax3(max3).setGenres(genres).setActors(actors).build();

                BMovieTripleResponse response = stub.getMoviesByTripleQuery(request);
                Map<String, QueriedResultsGRPC> responseMap = response.getResponseMap();

                StringBuilder sb = new StringBuilder();
                sb.append(min1);
                sb.append(max1);
                sb.append(min2);
                sb.append(max2);
                sb.append(min3);
                sb.append(max3);

                MutableBoolean genreFlag = new MutableBoolean(true), actorFlag = new MutableBoolean(true);
                determineGenreANDActorChoice(sb, genres, actors, genreFlag, actorFlag);

                QueriedResultsGRPC resultsOfQuery = responseMap.get(sb.toString());

                if (!resultsOfQuery.getFlag()) {
                    return new BMovieQueriedInfo(false);
                } else if (resultsOfQuery.getFlag() && resultsOfQuery.getImdbIDsList().size() == 0) {
                    return new BMovieQueriedInfo(true);
                }
                BMovieQueriedInfo imdbResults = new BMovieQueriedInfo(true);
                imdbResults.setImdbIDs(resultsOfQuery.getImdbIDsList());
                return imdbResults;
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } else {
        }
        return null;
    }

    public BMovieGenres retrieveAllBMovieGenres() {
        ManagedChannel channel = GrpcChannelManager.getInstance().getChannel("0.0.0.0",
            BMovieQueryServiceDriver.GRPC_PORT);

        if (channel != null) {
            try {
                BMovieGenresHandlerGrpc.BMovieGenresHandlerBlockingStub stub = BMovieGenresHandlerGrpc.newBlockingStub(channel);
                BMovieGenreRequest request = BMovieGenreRequest.newBuilder().build();

                BMovieGenreResponse response = stub.getBMovieGenres(request);
                Map<String, BMovieGenresGRPC> responseMap = response.getResponseMap();

                BMovieGenresGRPC bMovGenres = responseMap.get("genres");

                BMovieGenres movieGenres = new BMovieGenres();
                movieGenres.setGenres(bMovGenres.getGenresList());
                return movieGenres;
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } else {
        }
        return null;
    }
}
