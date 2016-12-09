package edu.nyu.cs.cs2580.utilities.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by stephen on 12/9/16.
 */

public class Crawler implements Runnable {

    private int id;
    private Controller myController = null;

    public Crawler(int id, Controller controlla) {
        this.id = id;
        this.myController = controlla;
    }

    @Override
    public void run() {
        Document doc;
        while (true) {
            String urlFocus = this.myController.grab();
            System.out.println("Worker " + id + ": " + urlFocus);
            System.out.println("#Links: " + this.myController.getNumLinks());
            if (okUrl(urlFocus)) {
                doc = connect(urlFocus);
                if (doc != null) {
                    considerLinks(doc.select("a[href]"));

                    try {
                        saveDocument(doc);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    private Document connect(String url) {
        try {
            return Jsoup.connect(url).get();
        } catch (IOException e) {
            return null;
        }
    }

    private boolean okUrl(String url) {
        boolean toReturn = false;
        try {
            new URL(url);
            toReturn = true;
        } catch (MalformedURLException e) {

        }
        return toReturn;

    }

    private void considerLinks(Elements links) {
        for (int i = 0; i < links.size(); i++) {
            Element link = links.get(i);
            String reportUrl = link.attr("abs:href");
            myController.handle(reportUrl);
        }
    }

    private boolean saveDocument(Document doc) throws URISyntaxException, IOException {
        URI address = new URI(doc.baseUri());
        if (address.getPath().contains("wiki/Category") || address.getPath().contains("wiki/Talk") || address.getPath().contains("wiki/User") || address.getPath().contains("wiki/File:") ) {
            return false;
        } else {
            String body = doc.text();
            if (body.contains("United States") || body.contains("USA") || body.contains("America")) {
                String directory = "data";
                String hostname = address.getHost();
                String pathname = address.getPath().replaceAll("/", "_");
                String full_dir_path = directory + "/" + hostname + "/" + pathname;
                File file = new File(full_dir_path);
                File parent = file.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) {
                    throw new IllegalStateException("Couldn't create dir: " + parent);
                }
                if (!file.exists()) {
                    file.createNewFile();
                }
                FileWriter writer = new FileWriter(file, true);
                BufferedWriter bufferWriter = new BufferedWriter(writer);
                bufferWriter.write(doc.outerHtml());
                bufferWriter.close();
                return true;
            }
            return false;
        }
    }
}
