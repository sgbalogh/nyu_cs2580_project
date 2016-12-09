package edu.nyu.cs.cs2580.utilities.crawler;

import java.util.LinkedList;

/**
 * Created by stephen on 12/9/16.
 */
public class CrawlEngine {

    public static void main(String[] args) {
        int workers = 1;
        // int pdf_workers = 0;
        // int html_workers = 0;
        //System.setProperty("javax.net.ssl.trustStore", "/Users/sgb334/ssl/archivefda.dlib.nyu.edu.jks");

        LinkedList<Runnable> workerList = new LinkedList<>();
        LinkedList<Thread> threadList = new LinkedList<>();
        LinkedList<Runnable> pdf_workerList = new LinkedList<>();
        LinkedList<Thread> pdf_threadList = new LinkedList<>();

        Controller myController = new Controller("en.wikipedia.org/wiki");

        for (int i = 0; i < workers; i++) {
            Runnable r_add = new Crawler(i, myController);
            Thread t_add = new Thread(r_add);
            workerList.add(r_add);
            threadList.add(t_add);
            t_add.start();
        }

        /*
        for (int i = 0; i < pdf_workers; i++) {
            Runnable r_add = new PdfDownloader(i, myControlla);
            Thread t_add = new Thread(r_add);
            pdf_workerList.add(r_add);
            pdf_threadList.add(t_add);
            t_add.start();
        }
        */
    }


}

