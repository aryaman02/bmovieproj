package com.example.entrypoint;

import com.example.controller.BMovie;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BMovieDataAggregator {
    private Map<String, BMovie> bMovieData = new HashMap<>();
    private static final String META_DATA_FILEPATH = "//Users/aryaman/Documents/bmovieproj/bmovie_spring/src/main/resources/bollywood_meta_1990-2009.csv";
    private static final String BTEXT_DATA_FILEPATH = "/Users/aryaman/Documents/bmovieproj/bmovie_spring/src/main/resources/bollywood_text_1990-2009.csv";
    private static final String BRATINGS_DATA_FILEPATH = "/Users/aryaman/Documents/bmovieproj/bmovie_spring/src/main/resources/bollywood_ratings_1990-2009.csv";

    public Map<String, BMovie> getbMovieData() {
        return bMovieData;
    }

    public void aggregateMovieInfo() {
        parseBMetaData();
        parseBTextData();
        parseBRatingsData();
    }

    private void parseBMetaData() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(META_DATA_FILEPATH));
            try {
                String line = br.readLine();

                while ((line = br.readLine()) != null) {
                    String[] bMovieInfo = line.split(",");
                    int year = Integer.parseInt(bMovieInfo[4]);

                    if (year < 1990 || year > 2009) {
                        continue;
                    }
                    String imdbId = bMovieInfo[0];
                    //System.out.println(imdbId + " " + bMovieInfo.length);

                    if (!bMovieData.containsKey(imdbId)) {
                        String bMovieTitle = bMovieInfo[2];
                        int yearOfRelease = Integer.parseInt(bMovieInfo[4]);

                        int runtime = -1;
                        String duration = bMovieInfo[5];
                        if (!duration.equals("\\N")) {
                            runtime = Integer.parseInt(duration);
                        }

                        String genres = line.substring(line.lastIndexOf(',')+1, line.length());
                        List<String> genreCategories = new ArrayList<>();
                        int index = genres.indexOf('|');

                        if (index == -1) {
                            genreCategories.add(genres);
                        } else {
                            String firstGenre = genres.substring(0, index);
                            genreCategories.add(firstGenre);
                            int beginIndex = index;

                            while (true) {
                                index = genres.indexOf('|', beginIndex+1);
                                if (index == -1) {
                                    String lastGenre = genres.substring(beginIndex+1, genres.length());
                                    genreCategories.add(lastGenre);
                                    break;
                                }
                                genreCategories.add(genres.substring(beginIndex+1, index));
                                beginIndex = index;
                            }
                        }

                        BMovie bollyMovie = new BMovie(imdbId, bMovieTitle, yearOfRelease, runtime, genreCategories, new ArrayList<>(), -1, null);
                        bMovieData.put(imdbId, bollyMovie);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void parseBTextData() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(BTEXT_DATA_FILEPATH));
            try {
                String line = br.readLine();

                while ((line = br.readLine()) != null) {
                    //String[] bMovieInfo = line.split(",");
                    String imdbId = line.substring(0, line.indexOf(','));

                    if (bMovieData.containsKey(imdbId)) {
                        BMovie bMovie = bMovieData.get(imdbId);
                        /*if (bMovieInfo.length < 7) {
                            System.out.println(imdbId + " " + bMovieInfo.length);
                        }*/
                        int index = line.indexOf('|');

                        if (index == -1) {
                            continue;
                        } else {
                            int i = index;
                            while (line.charAt(i) != ',') {
                                i--;
                            }
                            List<String> actorNames = new ArrayList<>();

                            String firstActor = line.substring(i+1, index);
                            actorNames.add(firstActor);
                            int beginIndex = index;

                            while (true) {
                                index = line.indexOf('|', beginIndex+1);

                                if (index == -1) {
                                    break;
                                }
                                actorNames.add(line.substring(beginIndex+1, index));
                                beginIndex = index;
                            }
                            bMovie.setActors(actorNames);
                        }
                        bMovieData.put(imdbId, bMovie);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void parseBRatingsData() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(BRATINGS_DATA_FILEPATH));
            try {
                String line = br.readLine();

                while ((line = br.readLine()) != null) {
                    String imdbId = line.substring(0, line.indexOf(','));

                    if (bMovieData.containsKey(imdbId)) {
                        double imdbRating = Double.parseDouble(line.substring(line.indexOf(',')+1, line.lastIndexOf(',')));
                        BMovie bMovie = bMovieData.get(imdbId);
                        bMovie.setImdbRating(imdbRating);
                        bMovieData.put(imdbId, bMovie);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}

