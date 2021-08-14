package com.example.processing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import processing.core.*;

public class BMovieImgLoader extends PApplet { // CANNOT WRITE THIS CODE IN A SEPARATE CLASS!!
    private String uri;
    private File bMovImgDirectory;
    private List<String> bMovQueryIMDBids;
    private OkHttpClient client = new OkHttpClient();

    private static final int NUM_THREADS = 5;
    private final LinkedBlockingQueue<String> lbq = new LinkedBlockingQueue<>();
    private final CountDownLatch beginWorkLatch = new CountDownLatch(NUM_THREADS);
    private final CountDownLatch finishedWorkLatch = new CountDownLatch(NUM_THREADS);

    public BMovieImgLoader(String uri, File dir, List<String> l) {
        this.uri = uri;
        bMovImgDirectory = dir;
        bMovQueryIMDBids = l;
    }

    public void beginIMGLoading() {
        try {
            beginWorkLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (String id : bMovQueryIMDBids) {
            lbq.add(id);
        }
        try {
            finishedWorkLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void loadPosterIMGFromIMDBId(String imdbID) {
        try {
            File jpgIMG = new File(bMovImgDirectory.getAbsolutePath() + File.separator + imdbID + ".jpg");

            if (!jpgIMG.exists()) {
                Response response = client.newCall(new Request.Builder().url(uri + imdbID).build()).execute(); // get raw image data

                byte[] byteArr = response.body().bytes();

                if (byteArr.length >= 8000) {
                    jpgIMG.createNewFile(); // storing the poster image in the img directory

                    try (FileOutputStream outputStream = new FileOutputStream(jpgIMG)) {
                        outputStream.write(byteArr); // write bytes to the empty file
                    }
                    //queryImgs.add(loadImage(bMovImgDirectory.getAbsolutePath() + imdbID + ".jpg"));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private class Worker implements Runnable {
        private BMovieImgLoader bil;

        public Worker(BMovieImgLoader loader) {
            bil = loader;
        }

        @Override
        public void run() {
            beginWorkLatch.countDown();
            boolean startedWork = false;

            while (!startedWork) {
                while (!lbq.isEmpty()) {
                    startedWork = true;
                    try {
                        String imdbID = lbq.poll(4, TimeUnit.SECONDS);

                        if (imdbID != null) {
                            startedWork = true;
                            bil.loadPosterIMGFromIMDBId(imdbID);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            finishedWorkLatch.countDown();
        }
    }

    public void startWorkerThreads() {
        for (int i = 0; i < NUM_THREADS; i++) {
            Thread thr = new Thread(new Worker(this));
            thr.start();
        }
    }
}

