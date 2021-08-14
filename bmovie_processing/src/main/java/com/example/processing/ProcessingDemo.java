package com.example.processing;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.io.FileUtils;
import processing.core.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ProcessingDemo extends PApplet {
    private List<PImage> bMovImgs;
    private float[] xPosArrIMG;
    private final OkHttpClient client = new OkHttpClient();
    private ObjectMapper m = new ObjectMapper();
    private List<String> errorMessages;
    private float xPosText;
    private int textIndex = 0;
    private static final float DECREMENT_FACTOR = 3;
    private static final float IMG_SPACING_FACTOR = 6;
    private static final String REST_IMG_ENDPOINT_URI = "http://0.0.0.0:8080/api/v1/bmovie/img?id=";
    private static final String bMovDirPathName = "/Users/aryaman/Documents/bMovImgs/";
    private boolean queryProcessed = false, isValidRequest = false, loadImgFlag = false, resetQuery = false;
    private PFont font;

    public static void main(String[] args) {
        PApplet.main(ProcessingDemo.class, args);
    }

    private String processUserQueryBMov() {
        Scanner in = new Scanner(System.in);
        String queryResponse = "";

        System.out.println("Welcome to the BMovie Query API! We provide to the user eight different ways to query for Bollywood movies. Here are the options: ");
        System.out.println("0: Query by Genres and Actors ONLY");
        System.out.println("1: Query by IMDB Rating ONLY");
        System.out.println("2: Query by IMDB Runtime ONLY");
        System.out.println("3: Query by Year of Release ONLY");
        System.out.println("4: Query by IMDB Rating AND Year of Release");
        System.out.println("5: Query by IMDB Runtime and Year of Release");
        System.out.println("6: Query by IMDB Runtime and IMDB Rating");
        System.out.println("7: Query by ALL Criteria");
        System.out.println("Press \'R\' or \'r\' to redo a BMovie query. Note that you WON'T be able to perform this function while entering user input.");
        System.out.println("**Note**: For each of these options, you can also query for movies by Genres and Actors, although it is optional for options 1-7.");

        System.out.print("Enter a number (0-7) indicating your choice: ");
        String line1 = in.nextLine();

        boolean validNum = false;
        int choice = -1;

        try {
            choice = Integer.parseInt(line1);
            validNum = true;
        } catch (NumberFormatException ex) {
            System.out.println("Unable to parse user choice as an integer: " + ex);
        }

        if (!validNum || (choice < 0 || choice > 7)) {
            queryResponse = "Invalid choice. Please try again!";
            return queryResponse;
        }

        String genres = "", actors = "", minR = "", maxR = "", minD = "", maxD = "", minY = "", maxY = "";

        System.out.print("Enter a list of genres separated by comma, or type n.a. OR N.A: ");
        genres = in.nextLine();
        System.out.print("Enter a list of actors separated by comma, or type n.a. OR N.A: ");
        actors = in.nextLine();

        switch (choice) {
        case 0:
            try {
                queryResponse = client.newCall(new Request.Builder().url("http://0.0.0.0:8080/api/v1/bmovie/query0?g=" + genres +
                    "&a=" + actors).build()).execute().body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            break;

        case 1:
            System.out.print("Enter lower bound for IMDB rating (0-10): ");
            minR = in.nextLine();
            System.out.print("Enter upper bound for IMDB rating (0-10): ");
            maxR = in.nextLine();
            try {
                queryResponse = client.newCall(new Request.Builder().url("http://0.0.0.0:8080/api/v1/bmovie/query1?minR=" + minR + "&maxR=" + maxR +
                    "&g=" + genres + "&a=" + actors).build()).execute().body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            break;

        case 2:
            System.out.print("Enter lower bound for IMDB runtime (>= 0): ");
            minD = in.nextLine();
            System.out.print("Enter upper bound for IMDB runtime (>= 0): ");
            maxD = in.nextLine();
            try {
                queryResponse = client.newCall(new Request.Builder().url("http://0.0.0.0:8080/api/v1/bmovie/query2?minD=" + minD + "&maxD=" + maxD +
                    "&g=" + genres + "&a=" + actors).build()).execute().body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            break;

        case 3:
            System.out.print("Enter lower bound for Year of Release (1990 to 2009 inclusive): ");
            minY = in.nextLine();
            System.out.print("Enter upper bound for Year of Release (1990 to 2009 inclusive): ");
            maxY = in.nextLine();
            try {
                queryResponse = client.newCall(new Request.Builder().url("http://0.0.0.0:8080/api/v1/bmovie/query3?minY=" + minY + "&maxY=" + maxY +
                    "&g=" + genres + "&a=" + actors).build()).execute().body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            break;

        case 4:
            System.out.print("Enter lower bound for IMDB rating (0-10): ");
            minR = in.nextLine();
            System.out.print("Enter upper bound for IMDB rating (0-10): ");
            maxR = in.nextLine();
            System.out.print("Enter lower bound for Year of Release (1990 to 2009 inclusive): ");
            minY = in.nextLine();
            System.out.print("Enter upper bound for Year of Release (1990 to 2009 inclusive): ");
            maxY = in.nextLine();
            try {
                queryResponse = client.newCall(new Request.Builder().url("http://0.0.0.0:8080/api/v1/bmovie/query4?minR=" + minR + "&maxR=" + maxR +
                    "&minY=" + minY + "&maxY=" + maxY + "&g=" + genres + "&a=" + actors).build()).execute().body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            break;

        case 5:
            System.out.print("Enter lower bound for IMDB runtime (>= 0): ");
            minD = in.nextLine();
            System.out.print("Enter upper bound for IMDB runtime (>= 0): ");
            maxD = in.nextLine();
            System.out.print("Enter lower bound for Year of Release (1990 to 2009 inclusive): ");
            minY = in.nextLine();
            System.out.print("Enter upper bound for Year of Release (1990 to 2009 inclusive): ");
            maxY = in.nextLine();
            try {
                queryResponse = client.newCall(new Request.Builder().url("http://0.0.0.0:8080/api/v1/bmovie/query5?minD=" + minD + "&maxD=" + maxD +
                    "&minY=" + minY + "&maxY=" + maxY + "&g=" + genres + "&a=" + actors).build()).execute().body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            break;

        case 6:
            System.out.print("Enter lower bound for IMDB runtime (>= 0): ");
            minD = in.nextLine();
            System.out.print("Enter upper bound for IMDB runtime (>= 0): ");
            maxD = in.nextLine();
            System.out.print("Enter lower bound for IMDB rating (0-10): ");
            minR = in.nextLine();
            System.out.print("Enter upper bound for IMDB rating (0-10): ");
            maxR = in.nextLine();
            try {
                queryResponse = client.newCall(new Request.Builder().url("http://0.0.0.0:8080/api/v1/bmovie/query6?minD=" + minD + "&maxD=" + maxD +
                    "&minR=" + minR + "&maxR=" + maxR + "&g=" + genres + "&a=" + actors).build()).execute().body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            break;

        case 7:
            System.out.print("Enter lower bound for IMDB runtime (>= 0): ");
            minD = in.nextLine();
            System.out.print("Enter upper bound for IMDB runtime (>= 0): ");
            maxD = in.nextLine();
            System.out.print("Enter lower bound for IMDB rating (0-10): ");
            minR = in.nextLine();
            System.out.print("Enter upper bound for IMDB rating (0-10): ");
            maxR = in.nextLine();
            System.out.print("Enter lower bound for Year of Release (1990 to 2009 inclusive): ");
            minY = in.nextLine();
            System.out.print("Enter upper bound for Year of Release (1990 to 2009 inclusive): ");
            maxY = in.nextLine();
            try {
                queryResponse = client.newCall(new Request.Builder().url("http://0.0.0.0:8080/api/v1/bmovie/query7?minD=" + minD + "&maxD=" + maxD +
                    "&minR=" + minR + "&maxR=" + maxR + "&minY=" + minY + "&maxY=" + maxY + "&g=" + genres + "&a=" + actors).build()).execute().body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            break;

        default:
            queryResponse = "Invalid choice. Please try again!";
        }
        return queryResponse;
    }

    public void settings() {
        this.size(1200, 1200);
    }

    private void processQueriedOutcome() {
        String queriedOutcome = processUserQueryBMov();

        if (queriedOutcome.equals("Invalid choice. Please try again!")) {
            errorMessages.add(queriedOutcome);
        } else if (queriedOutcome.equals("Sorry! No results found!")) {
            errorMessages.add(queriedOutcome);
        } else if (queriedOutcome.equals("Bad Request - Please enter valid genres only, or hit the genre endpoint if unsure.")) {
            errorMessages.add(queriedOutcome);
        } else if (queriedOutcome.indexOf('{') != -1) {
            isValidRequest = true;
            String str = queriedOutcome.substring(queriedOutcome.indexOf('[') + 1, queriedOutcome.indexOf(']'));
            String[] strArr = str.split(",");
            List<String> imdbIds = new ArrayList<>();

            for (String s : strArr) {
                imdbIds.add(s.substring(1, s.length()-1));
            }
            File imgDirectory = new File(bMovDirPathName); // create temporary directory to store images

            if (!imgDirectory.exists()) {
                imgDirectory.mkdir();
            }

            BMovieImgLoader imgLoader = new BMovieImgLoader(REST_IMG_ENDPOINT_URI, imgDirectory, imdbIds);
            imgLoader.startWorkerThreads();
            imgLoader.beginIMGLoading();

            for (String id : imdbIds) { // load images from file system
                String filePath = imgDirectory.getAbsolutePath() + File.separator + id + ".jpg";
                //System.out.println(filePath);

                File tempFile = new File(filePath);

                if (tempFile.exists()) {
                    bMovImgs.add(loadImage(filePath));
                }
            }
            // System.out.println(bMovImgs.size());

            if (bMovImgs.size() == 0) {
                loadImgFlag = false;
                errorMessages.add("Sorry! We are unable to provide poster images for these movies at this time.");
            } else {
                loadImgFlag = true;
                xPosArrIMG = new float[bMovImgs.size()];
                setInitialPositionsOfImgs();
            }

        } else {
            int beginIndex = 0;

            while (beginIndex < queriedOutcome.length()) {
                int periodIndex = queriedOutcome.indexOf('.', beginIndex);
                errorMessages.add(queriedOutcome.substring(beginIndex, periodIndex+1));
                beginIndex = periodIndex + 1;
            }
        }
    }

    public void setup() {
        errorMessages = new ArrayList<>();
        bMovImgs = new ArrayList<>();
        xPosText = this.width;

        processQueriedOutcome();
        queryProcessed = true;
    }

    private void setInitialPositionsOfImgs() {
        xPosArrIMG[0] = this.width;

        for (int i = 1; i < bMovImgs.size(); i++) {
            xPosArrIMG[i] = (xPosArrIMG[i-1] + bMovImgs.get(i-1).width + this.width/IMG_SPACING_FACTOR); // setting initial positions of images
        }
    }

    private void displayErrorMessages() {
        font = createFont("Times New Roman Black", 30);
        textFont(font);
        textAlign(LEFT);
        this.text(errorMessages.get(textIndex), xPosText, 500);

        xPosText -= DECREMENT_FACTOR;
        if (xPosText < -textWidth(errorMessages.get(textIndex))) { // text completely disappeared on left side!
            xPosText = this.width;
            textIndex = (textIndex + 1) % errorMessages.size();
        }
    }

    private void displayBMovieImgs() {
        boolean flag = false;

        for (int i = 0; i < bMovImgs.size(); i++) {
            if (resetQuery) {
                flag = true;
                break;
            }
            int imgWidth = bMovImgs.get(i).width;
            int imgHeight = bMovImgs.get(i).height;

            this.image(bMovImgs.get(i), xPosArrIMG[i], (float) (7 * this.height / 16), imgWidth, imgHeight); // show image
            xPosArrIMG[i] -= DECREMENT_FACTOR;

            if (i == bMovImgs.size() - 1 && xPosArrIMG[i] < -imgWidth) { // wait until last image has disappeared to reset img positions
                setInitialPositionsOfImgs();
            }
        }
        if (flag) {
            return;
        }
    }

    private void drawTitle() {
        // draw title
        font = createFont("Times New Roman Bold", 48);
        textFont(font);
        textAlign(LEFT);
        this.fill(139, 0, 0);
        this.text("Welcome", 300, 90);
        this.fill(255, 140, 0);
        this.text("to", (300 + textWidth("Welcome") + this.width/11), 90);
        this.fill(255, 215, 0);
        this.text("the", (300 + textWidth("Welcome") + this.width/11 + textWidth("to") + this.width/11), 90);
        this.fill(255, 255, 0);
        this.text("BMovie", 265, 160);
        this.fill(0, 100, 0);
        this.text("Poster", (265 + textWidth("BMovie") + this.width/11), 160);
        this.fill(48, 25, 52);
        this.text("API!", (265 + textWidth("BMovie") + this.width/11 + textWidth("Poster") + this.width/11), 160);
    }

    public void draw() {
        this.background(255, 255, 255);

        drawTitle();

        if (queryProcessed) { // only draw after accepting user query
            if (isValidRequest) {
                if (loadImgFlag) { // show the images!
                    displayBMovieImgs();
                } else { // print can't show images
                    displayErrorMessages();
                }
            } else {
                // print the common error messages
                displayErrorMessages();
            }
        } else {
            // just draw the title and white background
        }
    }

    public void keyPressed() {
        if ((queryProcessed && key == 'R') || (queryProcessed && key == 'r')) {
            resetQuery = true;
            queryProcessed = false;
            isValidRequest = false;
            loadImgFlag = false;
            errorMessages = new ArrayList<>();
            bMovImgs = new ArrayList<>();
            xPosText = this.width;

            processQueriedOutcome();
            resetQuery = false;
            queryProcessed = true;
        }
    }
}
