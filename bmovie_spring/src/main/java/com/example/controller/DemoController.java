package com.example.controller;

import com.example.utils.BMovieConfigProps;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.*;
import io.minio.errors.*;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
public class DemoController {
    private final ObjectMapper m = new ObjectMapper();
    private final BMovieQueryClient bMovieQueryClient = new BMovieQueryClient();
    private final OkHttpClient client = new OkHttpClient();

    private static final String MINIO_CONNECTIONINFO = "http://%s:9000";
    private MinioClient minioClient =
        MinioClient.builder()
            .endpoint(String.format(MINIO_CONNECTIONINFO, BMovieConfigProps.getMinIOAddress()))
            .credentials("key", "aryaman02")
            .build();
    private static final String BUCKET_NAME = "bmovieimgs";

    @PostConstruct
    public void initialize() throws ServerException, InsufficientDataException, ErrorResponseException, IOException,
        NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        // Make 'bmovieimgs' bucket if not exist.
        boolean found =
            minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build());
        if (!found) {
            // Make a new bucket called 'bmovieimgs'.
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build());
        } else {
            System.out.println("Bucket 'bmovieimgs' already exists.");
        }
    }

    @PreDestroy
    public void cleanup() {

    }

    @RequestMapping(value="/api/v1/bmovie/description", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> getDescriptionOfBMovie(@RequestParam(name="id") String imdbID)
        throws JsonProcessingException {
        boolean isValidId = true;

        if (imdbID.length() == 9 && imdbID.indexOf("tt") == 0) {
            for (int i = 2; i < imdbID.length(); i++) {
                char ch = imdbID.charAt(i);

                if (!Character.isDigit(ch)) {
                    isValidId = false;
                }
            }
        } else {
            isValidId = false;
        }

        if (!isValidId) {
            return new ResponseEntity<>("Bad Request. Please provide a valid IMDB id.", HttpStatus.BAD_REQUEST);
        }

        BMovie outputMovDetails = bMovieQueryClient.findBMovieDetails(imdbID);

        //System.out.println("Imdb id: " + outputMovDetails.getImdbID());
        //System.out.println("Title: " + outputMovDetails.getTitle());
        //System.out.println("year of release: " + outputMovDetails.getYearOfRelease());

        if (outputMovDetails.getImdbID().equals("")) {
            return new ResponseEntity<>("Sorry! Could not provide BMovie details for this imdb id.", HttpStatus.NOT_FOUND);
        } else if (outputMovDetails.getImdbID() != null && outputMovDetails.getTitle() == null) {
            return new ResponseEntity<>("Sorry! Details pertaining to this imdb id is off-limits for the user.", HttpStatus.FORBIDDEN);
        } else {
            String response = m.writeValueAsString(outputMovDetails);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    @RequestMapping(value="/api/v1/bmovie/img", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<byte[]> getImageOfBMovie(@RequestParam(name="id") String imdbID)
        throws IOException, ServerException, InsufficientDataException, ErrorResponseException,
        NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        boolean isValidId = true;

        if (imdbID.length() == 9 && imdbID.indexOf("tt") == 0) {
            for (int i = 2; i < imdbID.length(); i++) {
                char ch = imdbID.charAt(i);

                if (!Character.isDigit(ch)) {
                    isValidId = false;
                }
            }
        } else {
            isValidId = false;
        }

        if (!isValidId) {
            HttpHeaders specializedHeaders = new HttpHeaders();
            specializedHeaders.setContentType(MediaType.TEXT_PLAIN);
            return new ResponseEntity<>("Bad Request. Please provide a valid IMDB id.".getBytes(StandardCharsets.UTF_8), specializedHeaders, HttpStatus.BAD_REQUEST);
        }

        BMovieQueriedInfo bMovImgStatus = bMovieQueryClient.getStatusOfBMovieImg(imdbID);

        if (!bMovImgStatus.getQueryStatus()) {
            HttpHeaders specializedHeaders = new HttpHeaders();
            specializedHeaders.setContentType(MediaType.TEXT_PLAIN);
            return new ResponseEntity<>("Sorry! Could not provide BMovie poster image for this imdb id.".getBytes(
                StandardCharsets.UTF_8), specializedHeaders, HttpStatus.NOT_FOUND);

        } else if (bMovImgStatus.getQueryStatus() && bMovImgStatus.getQueriedResults().size() == 0) {
            HttpHeaders specializedHeaders = new HttpHeaders();
            specializedHeaders.setContentType(MediaType.TEXT_PLAIN);
            return new ResponseEntity<>("Sorry! You are only allowed to request poster images pertaining to valid BMovies.".getBytes(
                StandardCharsets.UTF_8), specializedHeaders, HttpStatus.FORBIDDEN);

        } else {
            String omdbURL = new HttpUrl.Builder() .scheme("https")
                .host("omdbapi.com").addQueryParameter("i", imdbID).addQueryParameter("apikey", "da14bb5c").build().toString();

            String imgName = imdbID + ".jpg";

            boolean found = false;
            try {
                StatObjectResponse response = minioClient.statObject(StatObjectArgs.builder().bucket(BUCKET_NAME).object(imgName).build());
                found = true;
            } catch (MinioException e) {
                System.out.println("Error occurred: " + e);
            }

            if (found) {
                // Do things when object is found
                // No need to upload image!
            } else {
                // Do things when object is not found
                Request bMovInfoRequest = new Request.Builder().url(omdbURL).build();
                Response bMovInfoResponse = client.newCall(bMovInfoRequest).execute();
                String bMovieInfo = bMovInfoResponse.body().string();
                bMovInfoResponse.close();

                StringBuilder sb = new StringBuilder();
                int imgIndex = bMovieInfo.indexOf(':', bMovieInfo.indexOf("Poster")) + 2;

                while (bMovieInfo.charAt(imgIndex) != '\"') {
                    sb.append(bMovieInfo.charAt(imgIndex));
                    imgIndex++;
                }
                String imgURL = sb.toString();

                if (imgURL.equals("N/A")) {
                    HttpHeaders specializedHeaders = new HttpHeaders();
                    specializedHeaders.setContentType(MediaType.TEXT_PLAIN);
                    return new ResponseEntity<>("Sorry! Could not provide BMovie poster image for this imdb id.".getBytes(
                        StandardCharsets.UTF_8), specializedHeaders, HttpStatus.NOT_FOUND);
                }
                Request imgRequest = new Request.Builder().url(imgURL).build();
                Response imgResponse = client.newCall(imgRequest).execute();

                byte[] resp = imgResponse.body().bytes();

                // Headers headers = imgResponse.headers();
                // headers.forEach(header -> System.out.println(header.getFirst() + "   " + header.getSecond()));
                // System.out.println(body);

                // Create a InputStream for object upload.
                ByteArrayInputStream bais = new ByteArrayInputStream(resp);

                // Create object 'my-objectname' in 'my-bucketname' with content from the input stream.
                minioClient.putObject(
                    PutObjectArgs.builder().bucket(BUCKET_NAME).object(imgName).stream(
                        bais, bais.available(), -1)
                        .build());

                imgResponse.close();
                //System.out.println("BMovie image uploaded successfully!");
            }

            StringBuilder sb = new StringBuilder();

            InputStream stream =
                minioClient.getObject(
                    GetObjectArgs.builder().bucket(BUCKET_NAME).object(imgName).build());

            byte[] byteArr = IOUtils.toByteArray(stream);

            return new ResponseEntity<>(byteArr, HttpStatus.OK); // this will return the image!
        }
    }

    @RequestMapping(value="/api/v1/bmovie/query0", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> getBMoviesBySimpleQuery(@RequestParam(name="g") String genres, @RequestParam(name="a") String actors)
        throws JsonProcessingException {
        boolean genreChoice = true, actorChoice = true;

        if (genres.equals("n.a.") || genres.equals("N.A.")) {
            genreChoice = false;
        }
        if (actors.equals("n.a.") || actors.equals("N.A.")) {
            actorChoice = false;
        }
        StringBuilder sb = new StringBuilder();
        boolean flag = false;

        if (!genreChoice && !actorChoice) {
            sb.append("Bad Request - Please search for movies by actor or by genre (or both).");
            flag = true;
        }

        if (flag) {
            return new ResponseEntity<>(sb.toString(), HttpStatus.BAD_REQUEST);
        }
        BMovieQueriedInfo imdbIDs = bMovieQueryClient.retrieveGeneralQueriedResults(genres, actors);

        if (!imdbIDs.getQueryStatus()) {
            return new ResponseEntity<>("Bad Request - Please enter valid genres only, or hit the genre endpoint if unsure.", HttpStatus.BAD_REQUEST);
        } else if (imdbIDs.getQueryStatus() && imdbIDs.getQueriedResults().size() == 0) {
            return new ResponseEntity<>("Sorry! No results found!", HttpStatus.NOT_FOUND);
        }
        String response = m.writeValueAsString(imdbIDs);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value="/api/v1/bmovie/query1", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> getBMoviesByRatingQuery(@RequestParam(name="minR") String minR, @RequestParam(name="maxR") String maxR, @RequestParam(name="g") String genres, @RequestParam(name="a") String actors)
        throws JsonProcessingException {

        double minRating = -1, maxRating = -1;
        boolean isValidMinRating = false;
        try {
            minRating = Double.parseDouble(minR);
            isValidMinRating = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse min rating: " + ex);
        }

        boolean isValidMaxRating = false;
        try {
            maxRating = Double.parseDouble(maxR);
            isValidMaxRating = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse max rating: " + ex);
        }

        if ((isValidMinRating && minRating < 0) || (isValidMinRating && minRating > 10)) {
            isValidMinRating = false;
        }
        if ((isValidMaxRating && maxRating < 0) || (isValidMaxRating && maxRating > 10)) {
            isValidMaxRating = false;
        }
        StringBuilder sb = new StringBuilder();
        boolean flag = false;

        if (!isValidMinRating || !isValidMaxRating) {
            sb.append("Bad Request - Please enter a number between 0 and 10 inclusive for min and max rating.");
            flag = true;
        }
        if (minRating > maxRating) {
            sb.append("Bad Request - Min rating must always be less than or equal to max rating.");
            flag = true;
        }

        if (flag) {
            return new ResponseEntity<>(sb.toString(), HttpStatus.BAD_REQUEST);
        }

        BMovieQueriedInfo queriedIMDBids = bMovieQueryClient.retrieveSingleQueriedResults("Rating", minRating, maxRating, genres, actors);

        if (!queriedIMDBids.getQueryStatus()) {
            return new ResponseEntity<>("Bad Request - Please enter valid genres only, or hit the genre endpoint if unsure.", HttpStatus.BAD_REQUEST);
        } else if (queriedIMDBids.getQueryStatus() && queriedIMDBids.getQueriedResults().size() == 0) {
            return new ResponseEntity<>("Sorry! No results found!", HttpStatus.NOT_FOUND);
        }
        String response = m.writeValueAsString(queriedIMDBids);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value="/api/v1/bmovie/query2", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> getBMoviesByRuntimeQuery(@RequestParam(name="minD") String minD, @RequestParam(name="maxD") String maxD, @RequestParam(name="g") String genres, @RequestParam(name="a") String actors)
        throws JsonProcessingException {

        int minRuntime = -1, maxRuntime = -1;
        boolean isValidMinRuntime = false;
        try {
            minRuntime = Integer.parseInt(minD);
            isValidMinRuntime = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse min runtime: " + ex);
        }

        boolean isValidMaxRuntime = false;
        try {
            maxRuntime = Integer.parseInt(maxD);
            isValidMaxRuntime = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse max runtime: " + ex);
        }

        if (isValidMinRuntime && minRuntime < 0) {
            isValidMinRuntime = false;
        }
        if (isValidMaxRuntime && maxRuntime < 0) {
            isValidMaxRuntime = false;
        }

        StringBuilder sb = new StringBuilder();
        boolean flag = false;

        if (!isValidMinRuntime || !isValidMaxRuntime) {
            sb.append("Bad Request - Please enter an integer >= 0 for min and max runtime.");
            flag = true;
        }
        if (minRuntime > maxRuntime) {
            sb.append("Bad Request - Min runtime must always be less than or equal to max runtime.");
            flag = true;
        }

        if (flag) {
            return new ResponseEntity<>(sb.toString(), HttpStatus.BAD_REQUEST);
        }

        BMovieQueriedInfo queriedIMDBids = bMovieQueryClient.retrieveSingleQueriedResults("Runtime", minRuntime, maxRuntime, genres, actors);

        if (!queriedIMDBids.getQueryStatus()) {
            return new ResponseEntity<>("Bad Request - Please enter valid genres only, or hit the genre endpoint if unsure.", HttpStatus.BAD_REQUEST);
        } else if (queriedIMDBids.getQueryStatus() && queriedIMDBids.getQueriedResults().size() == 0) {
            return new ResponseEntity<>("Sorry! No results found!", HttpStatus.NOT_FOUND);
        }
        String response = m.writeValueAsString(queriedIMDBids);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value="/api/v1/bmovie/query3", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> getBMoviesByYearQuery(@RequestParam(name="minY") String minY, @RequestParam(name="maxY") String maxY, @RequestParam(name="g") String genres, @RequestParam(name="a") String actors)
        throws JsonProcessingException {

        int minYear = -1, maxYear = -1;
        boolean isValidMinYear = false;
        try {
            minYear = Integer.parseInt(minY);
            isValidMinYear = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse min year: " + ex);
        }

        boolean isValidMaxYear = false;
        try {
            maxYear = Integer.parseInt(maxY);
            isValidMaxYear = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse max year: " + ex);
        }

        if ((isValidMinYear && minYear < 1990) || (isValidMinYear && minYear > 2009)) {
            isValidMinYear = false;
        }
        if ((isValidMaxYear && maxYear < 1990) || (isValidMaxYear && maxYear > 2009)) {
            isValidMaxYear = false;
        }

        StringBuilder sb = new StringBuilder();
        boolean flag = false;

        if (!isValidMinYear || !isValidMaxYear) {
            sb.append("Bad Request - Please enter an integer between 1990 and 2009 inclusive for min and max year.");
            flag = true;
        }
        if (minYear > maxYear) {
            sb.append("Bad Request - Min year must always be less than or equal to max year.");
            flag = true;
        }

        if (flag) {
            return new ResponseEntity<>(sb.toString(), HttpStatus.BAD_REQUEST);
        }

        BMovieQueriedInfo queriedIMDBids = bMovieQueryClient.retrieveSingleQueriedResults("Year", minYear, maxYear, genres, actors);

        if (!queriedIMDBids.getQueryStatus()) {
            return new ResponseEntity<>("Bad Request - Please enter valid genres only, or hit the genre endpoint if unsure.", HttpStatus.BAD_REQUEST);
        } else if (queriedIMDBids.getQueryStatus() && queriedIMDBids.getQueriedResults().size() == 0) {
            return new ResponseEntity<>("Sorry! No results found!", HttpStatus.NOT_FOUND);
        }
        String response = m.writeValueAsString(queriedIMDBids);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value="/api/v1/bmovie/query4", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> getBMoviesByRatingANDYearQuery(@RequestParam(name="minR") String minR, @RequestParam(name="maxR") String maxR,
        @RequestParam(name="minY") String minY, @RequestParam(name="maxY") String maxY, @RequestParam(name="g") String genres, @RequestParam(name="a") String actors)
        throws JsonProcessingException {

        double minRating = -1, maxRating = -1;
        boolean isValidMinRating = false;
        try {
            minRating = Double.parseDouble(minR);
            isValidMinRating = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse min rating: " + ex);
        }

        boolean isValidMaxRating = false;
        try {
            maxRating = Double.parseDouble(maxR);
            isValidMaxRating = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse max rating: " + ex);
        }

        if ((isValidMinRating && minRating < 0) || (isValidMinRating && minRating > 10)) {
            isValidMinRating = false;
        }
        if ((isValidMaxRating && maxRating < 0) || (isValidMaxRating && maxRating > 10)) {
            isValidMaxRating = false;
        }

        StringBuilder sb = new StringBuilder();
        boolean flag = false;

        if (!isValidMinRating || !isValidMaxRating) {
            sb.append("Bad Request - Please enter a number between 0 and 10 inclusive for min and max rating.");
            flag = true;
        }
        if (minRating > maxRating) {
            sb.append("Bad Request - Min rating must always be less than or equal to max rating.");
            flag = true;
        }

        int minYear = -1, maxYear = -1;
        boolean isValidMinYear = false;
        try {
            minYear = Integer.parseInt(minY);
            isValidMinYear = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse min year: " + ex);
        }

        boolean isValidMaxYear = false;
        try {
            maxYear = Integer.parseInt(maxY);
            isValidMaxYear = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse max year: " + ex);
        }

        if ((isValidMinYear && minYear < 1990) || (isValidMinYear && minYear > 2009)) {
            isValidMinYear = false;
        }
        if ((isValidMaxYear && maxYear < 1990) || (isValidMaxYear && maxYear > 2009)) {
            isValidMaxYear = false;
        }

        if (!isValidMinYear || !isValidMaxYear) {
            sb.append("Bad Request - Please enter an integer between 1990 and 2009 inclusive for min and max year.");
            flag = true;
        }
        if (minYear > maxYear) {
            sb.append("Bad Request - Min year must always be less than or equal to max year.");
            flag = true;
        }

        if (flag) {
            return new ResponseEntity<>(sb.toString(), HttpStatus.BAD_REQUEST);
        }

        BMovieQueriedInfo queriedIMDBids = bMovieQueryClient.retrieveDoubleQueriedResults("Rating", "Year", minRating, maxRating, minYear,
            maxYear, genres, actors);

        if (!queriedIMDBids.getQueryStatus()) {
            return new ResponseEntity<>("Bad Request - Please enter valid genres only, or hit the genre endpoint if unsure.", HttpStatus.BAD_REQUEST);
        } else if (queriedIMDBids.getQueryStatus() && queriedIMDBids.getQueriedResults().size() == 0) {
            return new ResponseEntity<>("Sorry! No results found!", HttpStatus.NOT_FOUND);
        }
        String response = m.writeValueAsString(queriedIMDBids);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value="/api/v1/bmovie/query5", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> getBMoviesByRuntimeANDYearQuery(@RequestParam(name="minD") String minD, @RequestParam(name="maxD") String maxD,
        @RequestParam(name="minY") String minY, @RequestParam(name="maxY") String maxY, @RequestParam(name="g") String genres, @RequestParam(name="a") String actors)
        throws JsonProcessingException {

        int minRuntime = -1, maxRuntime = -1;
        boolean isValidMinRuntime = false;
        try {
            minRuntime = Integer.parseInt(minD);
            isValidMinRuntime = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse min runtime: " + ex);
        }

        boolean isValidMaxRuntime = false;
        try {
            maxRuntime = Integer.parseInt(maxD);
            isValidMaxRuntime = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse max runtime: " + ex);
        }

        if (isValidMinRuntime && minRuntime < 0) {
            isValidMinRuntime = false;
        }
        if (isValidMaxRuntime && maxRuntime < 0) {
            isValidMaxRuntime = false;
        }

        StringBuilder sb = new StringBuilder();
        boolean flag = false;

        if (!isValidMinRuntime || !isValidMaxRuntime) {
            sb.append("Bad Request - Please enter an integer >= 0 for min and max runtime.");
            flag = true;
        }
        if (minRuntime > maxRuntime) {
            sb.append("Bad Request - Min runtime must always be less than or equal to max runtime.");
            flag = true;
        }

        int minYear = -1, maxYear = -1;
        boolean isValidMinYear = false;
        try {
            minYear = Integer.parseInt(minY);
            isValidMinYear = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse min year: " + ex);
        }

        boolean isValidMaxYear = false;
        try {
            maxYear = Integer.parseInt(maxY);
            isValidMaxYear = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse max year: " + ex);
        }

        if ((isValidMinYear && minYear < 1990) || (isValidMinYear && minYear > 2009)) {
            isValidMinYear = false;
        }
        if ((isValidMaxYear && maxYear < 1990) || (isValidMaxYear && maxYear > 2009)) {
            isValidMaxYear = false;
        }

        if (!isValidMinYear || !isValidMaxYear) {
            sb.append("Bad Request - Please enter an integer between 1990 and 2009 inclusive for min and max year.");
            flag = true;
        }
        if (minYear > maxYear) {
            sb.append("Bad Request - Min year must always be less than or equal to max year.");
            flag = true;
        }

        if (flag) {
            return new ResponseEntity<>(sb.toString(), HttpStatus.BAD_REQUEST);
        }

        BMovieQueriedInfo queriedIMDBids = bMovieQueryClient.retrieveDoubleQueriedResults("Runtime", "Year", minRuntime, maxRuntime, minYear,
            maxYear, genres, actors);

        if (!queriedIMDBids.getQueryStatus()) {
            return new ResponseEntity<>("Bad Request - Please enter valid genres only, or hit the genre endpoint if unsure.", HttpStatus.BAD_REQUEST);
        } else if (queriedIMDBids.getQueryStatus() && queriedIMDBids.getQueriedResults().size() == 0) {
            return new ResponseEntity<>("Sorry! No results found!", HttpStatus.NOT_FOUND);
        }
        String response = m.writeValueAsString(queriedIMDBids);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value="/api/v1/bmovie/query6", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> getBMoviesByRuntimeANDRatingQuery(@RequestParam(name="minD") String minD, @RequestParam(name="maxD") String maxD,
        @RequestParam(name="minR") String minR, @RequestParam(name="maxR") String maxR, @RequestParam(name="g") String genres, @RequestParam(name="a") String actors)
        throws JsonProcessingException {

        int minRuntime = -1, maxRuntime = -1;
        boolean isValidMinRuntime = false;
        try {
            minRuntime = Integer.parseInt(minD);
            isValidMinRuntime = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse min runtime: " + ex);
        }

        boolean isValidMaxRuntime = false;
        try {
            maxRuntime = Integer.parseInt(maxD);
            isValidMaxRuntime = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse max runtime: " + ex);
        }

        if (isValidMinRuntime && minRuntime < 0) {
            isValidMinRuntime = false;
        }
        if (isValidMaxRuntime && maxRuntime < 0) {
            isValidMaxRuntime = false;
        }

        StringBuilder sb = new StringBuilder();
        boolean flag = false;

        if (!isValidMinRuntime || !isValidMaxRuntime) {
            sb.append("Bad Request - Please enter an integer >= 0 for min and max runtime.");
            flag = true;
        }
        if (minRuntime > maxRuntime) {
            sb.append("Bad Request - Min runtime must always be less than or equal to max runtime.");
            flag = true;
        }

        double minRating = -1, maxRating = -1;
        boolean isValidMinRating = false;
        try {
            minRating = Double.parseDouble(minR);
            isValidMinRating = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse min rating: " + ex);
        }

        boolean isValidMaxRating = false;
        try {
            maxRating = Double.parseDouble(maxR);
            isValidMaxRating = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse max rating: " + ex);
        }

        if ((isValidMinRating && minRating < 0) || (isValidMinRating && minRating > 10)) {
            isValidMinRating = false;
        }
        if ((isValidMaxRating && maxRating < 0) || (isValidMaxRating && maxRating > 10)) {
            isValidMaxRating = false;
        }

        if (!isValidMinRating || !isValidMaxRating) {
            sb.append("Bad Request - Please enter a number between 0 and 10 inclusive for min and max rating.");
            flag = true;
        }
        if (minRating > maxRating) {
            sb.append("Bad Request - Min rating must always be less than or equal to max rating.");
            flag = true;
        }

        if (flag) {
            return new ResponseEntity<>(sb.toString(), HttpStatus.BAD_REQUEST);
        }

        BMovieQueriedInfo queriedIMDBids = bMovieQueryClient.retrieveDoubleQueriedResults("Runtime", "Rating", minRuntime, maxRuntime, minRating,
            maxRating, genres, actors);

        if (!queriedIMDBids.getQueryStatus()) {
            return new ResponseEntity<>("Bad Request - Please enter valid genres only, or hit the genre endpoint if unsure.", HttpStatus.BAD_REQUEST);
        } else if (queriedIMDBids.getQueryStatus() && queriedIMDBids.getQueriedResults().size() == 0) {
            return new ResponseEntity<>("Sorry! No results found!", HttpStatus.NOT_FOUND);
        }
        String response = m.writeValueAsString(queriedIMDBids);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value="/api/v1/bmovie/query7", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> getBMoviesByAllQueries(@RequestParam(name="minD") String minD, @RequestParam(name="maxD") String maxD,
        @RequestParam(name="minR") String minR, @RequestParam(name="maxR") String maxR, @RequestParam(name="minY") String minY, @RequestParam(name="maxY") String maxY,
        @RequestParam(name="g") String genres, @RequestParam(name="a") String actors)
        throws JsonProcessingException {

        int minRuntime = -1, maxRuntime = -1;
        boolean isValidMinRuntime = false;
        try {
            minRuntime = Integer.parseInt(minD);
            isValidMinRuntime = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse min runtime: " + ex);
        }

        boolean isValidMaxRuntime = false;
        try {
            maxRuntime = Integer.parseInt(maxD);
            isValidMaxRuntime = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse max runtime: " + ex);
        }

        if (isValidMinRuntime && minRuntime < 0) {
            isValidMinRuntime = false;
        }
        if (isValidMaxRuntime && maxRuntime < 0) {
            isValidMaxRuntime = false;
        }

        StringBuilder sb = new StringBuilder();
        boolean flag = false;

        if (!isValidMinRuntime || !isValidMaxRuntime) {
            sb.append("Bad Request - Please enter an integer >= 0 for min and max runtime.");
            flag = true;
        }
        if (minRuntime > maxRuntime) {
            sb.append("Bad Request - Min runtime must always be less than or equal to max runtime.");
            flag = true;
        }

        double minRating = -1, maxRating = -1;
        boolean isValidMinRating = false;
        try {
            minRating = Double.parseDouble(minR);
            isValidMinRating = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse min rating: " + ex);
        }

        boolean isValidMaxRating = false;
        try {
            maxRating = Double.parseDouble(maxR);
            isValidMaxRating = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse max rating: " + ex);
        }

        if ((isValidMinRating && minRating < 0) || (isValidMinRating && minRating > 10)) {
            isValidMinRating = false;
        }
        if ((isValidMaxRating && maxRating < 0) || (isValidMaxRating && maxRating > 10)) {
            isValidMaxRating = false;
        }

        if (!isValidMinRating || !isValidMaxRating) {
            sb.append("Bad Request - Please enter a number between 0 and 10 inclusive for min and max rating.");
            flag = true;
        }
        if (minRating > maxRating) {
            sb.append("Bad Request - Min rating must always be less than or equal to max rating.");
            flag = true;
        }

        int minYear = -1, maxYear = -1;
        boolean isValidMinYear = false;
        try {
            minYear = Integer.parseInt(minY);
            isValidMinYear = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse min year: " + ex);
        }

        boolean isValidMaxYear = false;
        try {
            maxYear = Integer.parseInt(maxY);
            isValidMaxYear = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse max year: " + ex);
        }

        if ((isValidMinYear && minYear < 1990) || (isValidMinYear && minYear > 2009)) {
            isValidMinYear = false;
        }
        if ((isValidMaxYear && maxYear < 1990) || (isValidMaxYear && maxYear > 2009)) {
            isValidMaxYear = false;
        }

        if (!isValidMinYear || !isValidMaxYear) {
            sb.append("Bad Request - Please enter an integer between 1990 and 2009 inclusive for min and max year.");
            flag = true;
        }
        if (minYear > maxYear) {
            sb.append("Bad Request - Min year must always be less than or equal to max year.");
            flag = true;
        }

        if (flag) {
            return new ResponseEntity<>(sb.toString(), HttpStatus.BAD_REQUEST);
        }

        BMovieQueriedInfo queriedIMDBids = bMovieQueryClient.retrieveTripleQueriedResults(minRuntime, maxRuntime, minRating,
            maxRating, minYear, maxYear, genres, actors);

        if (!queriedIMDBids.getQueryStatus()) {
            return new ResponseEntity<>("Bad Request - Please enter valid genres only, or hit the genre endpoint if unsure.", HttpStatus.BAD_REQUEST);
        } else if (queriedIMDBids.getQueryStatus() && queriedIMDBids.getQueriedResults().size() == 0) {
            return new ResponseEntity<>("Sorry! No results found!", HttpStatus.NOT_FOUND);
        }
        String response = m.writeValueAsString(queriedIMDBids);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value="/api/v1/bmovie/genres", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> getGenres() {
        BMovieGenres bMovGenres = bMovieQueryClient.retrieveAllBMovieGenres();

        try {
            String response = m.writeValueAsString(bMovGenres);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @RequestMapping(value="/api/v1/bmovie/help", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> getDescriptionOfBMovieEndpoints() {
        StringBuilder sb = new StringBuilder();

        sb.append("Welcome to the BMovie API! Here, you will get the opportunity to browse for and learn about Bollywood movies.\n");
        sb.append("To access the information you need, simply make a HTTP GET Request from whatever HTTP Client you are using "
            + "(like a web browser or a Postman REST client.\n");
        sb.append("There are a total of 12 different GET Requests you can make, and they are organized into 5 main categories: "
            + "Description, Image, Query, Genres, and Help. Here they are: \n\n\n");

        sb.append("Help (can be used as a guide for how to use the BMovie API): \n");
        sb.append("    1. http://0.0.0.0:8080/api/v1/bmovie/help\n\n");

        sb.append("Description (useful for finding the full description of a Bollywood movie): \n");
        sb.append("    1. http://0.0.0.0:8080/api/v1/bmovie/description?id=[IMDB_ID]\n\n");

        sb.append("Image (useful for finding the the poster image of a Bollywood movie): \n");
        sb.append("    1. http://0.0.0.0:8080/api/v1/bmovie/img?id=[IMDB_ID]\n\n");

        sb.append("Query (allows you to browse for Bollywood movies by various different criteria): \n");
        sb.append("    1. http://0.0.0.0:8080/api/v1/bmovie/query0?g=[GENRES]&a=[ACTORS]\n");
        sb.append("    2. http://0.0.0.0:8080/api/v1/bmovie/query1?minR=[MINR]&maxR=[MAXR]&g=[GENRES]&a=[ACTORS]\n");
        sb.append("    3. http://0.0.0.0:8080/api/v1/bmovie/query2?minD=[MIND]&maxD=[MAXD]&g=[GENRES]&a=[ACTORS]\n");
        sb.append("    4. http://0.0.0.0:8080/api/v1/bmovie/query3?minY=[MINY]&maxY=[MAXY]&g=[GENRES]&a=[ACTORS]\n");
        sb.append("    5. http://0.0.0.0:8080/api/v1/bmovie/query4?minR=[MINR]&maxR=[MAXR]&minY=[MINY]&maxY=[MAXY]&g=[GENRES]&a=[ACTORS]\n");
        sb.append("    6. http://0.0.0.0:8080/api/v1/bmovie/query5?minD=[MIND]&maxD=[MAXD]&minY=[MINY]&maxY=[MAXY]&g=[GENRES]&a=[ACTORS]\n");
        sb.append("    7. http://0.0.0.0:8080/api/v1/bmovie/query6?minD=[MIND]&maxD=[MAXD]&minR=[MINR]&maxR=[MAXR]&g=[GENRES]&a=[ACTORS]\n");
        sb.append("    8. http://0.0.0.0:8080/api/v1/bmovie/query7?minD=[MIND]&maxD=[MAXD]&minR=[MINR]&maxR=[MAXR]&minY=[MINY]&maxY=[MAXY]&g=[GENRES]&a=[ACTORS]\n\n");

        sb.append("Genres (useful for knowing what genres are available for browsing): \n");
        sb.append("    1. http://0.0.0.0:8080/api/v1/bmovie/genres\n\n\n");

        sb.append("Here is a more detailed description of the Description, Image, and Query endpoints: \n\n\n");

        sb.append("    Description: The imdbID, title, year of release, duration (runtime), genres, actors, imdbRating, and brief movie description "
            + "are all included in a BMovie description. To get the information you need, simply enter the imdbID for the desired movie. In the GET Request, "
            + "(see above) there is a query parameter called \"id\" and next to that you provide the imdbID. Note that this endpoint only accepts valid "
            + "imdbIDs (aka tt followed by 7 numeric digits - ex. tt0424489), and will send you an error message if it represents invalid input. "
            + "Make sure to not provide imdbIDs that don't even exist - you will receive an error message. The other important thing to consider is "
            + "you will only be able to receive BMovie descriptions for movies that are in the Hindi language, have India as the country of origin, and "
            + "have been released between the years 1990 and 2009 inclusive. You will receive an error message if at least one of these requirements is not met. "
            + "If all is good, then you will receive the full description of the desired BMovie formatted as a JSON string.\n\n");

        sb.append("    Image: To get the poster image for the desired BMovie, simply enter its imdbID. In the GET Request, (see above) there is a query parameter called \"id\" and "
            + "next to that you provide the imdbID. Note that this endpoint only accepts valid imdbIDs (aka tt followed by 7 numeric digits - ex. tt0424489), and will send you an error message if it represents invalid input. "
            + "Make sure to not provide imdbIDs that don't even exist - you will receive an error message. The other important thing to consider is "
            + "you will only be able to receive BMovie poster images for movies that are in the Hindi language, have India as the country of origin, and "
            + "have been released between the years 1990 and 2009 inclusive. You will receive an error message if at least one of these requirements is not met. "
            + "Occasionally, you might be unlucky to not be provided with a poster image for an imdbID that actually exists, though such cases are pretty rare. "
            + "If all is good, then you will receive the poster image formatted as a JPEG image.\n\n");

        sb.append("    Query: The 8 different kinds of GET Requests mentioned above represent the 8 different ways in which you can query for "
            + "Bollywood movies. You can browse for these movies by the following criteria: List of Actor names, List of genres, upper and lower bound values "
            + "for the imdbRating, upper and lower bound values for the duration (runtime), and upper and lower bound values for the year of release. "
            + "Think of these criteria as search filters - the more criteria you add on to your query search, the more specific your search results will become. "
            + "For example, if I browse for BMovies by the actress Madhuri Dixit, then I will get all Madhuri Dixit movies, but if I add on to it 1990 "
            + "and 1993 as my lower and upper bound values for the year of release, then I will only get Madhuri Dixit movies released between the years "
            + "1990 and 1993 inclusive. The words MINR, MAXR, MIND, MAXD, MINY, MAXY, GENRES, and ACTORS are query parameters which stand for: lower bound of imdbRating, "
            + "upper bound for imdbRating, lower bound of duration (runtime), upper bound of duration (runtime), lower bound for year of release, "
            + "upper bound for year of release, list of genres, and list of actors respectively. Say I want to browse for BMovies by duration, genres, and actors. "
            + "Then I would hit the Query Endpoint with query parameter as query2. There are some things to keep in mind when using the Query Endpoints. "
            + "One is when you are entering a list of genres, do so separated by commas (same thing applies for list of actors). Also, you can only enter "
            + "valid genres (hit the Genres endpoint if you are unsure what valid gwnres are available for browsing). There are other rules to keep in mind "
            + "when using these Query Endpoints, but it would take too much time to describe it here - explore them yourself! Every time you enter invalid input "
            + "for a query parameter for a particular GET Request, you will get a very specific error message describing what the issue(s) are, so read them "
            + "carefully to figure out how you can make the perfect Query request! One more thing - the output of a query search will be a list of imdbIDS "
            + "corresponding to the movies that match your search criteria formatted as a JSON string. If no movies match your query search, you will receive "
            + "an error message.\n\n\n");

        sb.append("Now that you have all the information you need to use the BMovie API, I will sign off by saying one more thing - happy browsing!!\n");

        return new ResponseEntity<>(sb.toString(), HttpStatus.OK);
    }
}
