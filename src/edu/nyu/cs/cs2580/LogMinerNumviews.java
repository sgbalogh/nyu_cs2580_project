package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.HashMap;
import java.util.Vector;

import org.jsoup.Jsoup;

import java.net.URLDecoder;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class LogMinerNumviews extends LogMiner {


    private HashMap<Integer, Integer> _map = new HashMap<>();
    private HashMap<String, Integer> _dictionary = new HashMap<>();
    private Vector<String> _corpus_documents = new Vector<>();
    private Vector<Integer> _views = new Vector<>();

    public LogMinerNumviews(Options options) {
        super(options);
    }

    /**
     * This function processes the logs within the log directory as specified by
     * the {@link _options}. The logs are obtained from Wikipedia dumps and have
     * the following format per line: [language]<space>[article]<space>[#views].
     * Those view information are to be extracted for documents in our corpus and
     * stored somewhere to be used during indexing.
     * <p>
     * Note that the log contains view information for all articles in Wikipedia
     * and it is necessary to locate the information about articles within our
     * corpus.
     *
     * @throws IOException
     */
    @Override
    public void compute() throws IOException {
        System.out.println("Computing using " + this.getClass().getName());

        /* Iterate through directory of log files, collect names */
        File folder = new File(this._options._logPrefix);
        File[] all_logs = folder.listFiles();
        Vector<File> logfiles = new Vector<>();
        for (int i = 0; i < all_logs.length; i++) {
            if (all_logs[i].isFile() && !all_logs[i].isDirectory()) {
                logfiles.add(all_logs[i]);
            }
        }

        /* Iterate through folder of documents in corpus, collect names */

        for (File fileEntry : new File(_options._corpusPrefix).listFiles()) {
	        if (fileEntry.isFile()) {
	        	try {
		        	String[] docWords = parseDocument(fileEntry);
	
		        	if(docWords.length <= 1) {
		        		//System.out.println(fileEntry.getName());
		        		continue;
		        	}
	
	                _corpus_documents.add(fileEntry.getName());
	        	} catch(Exception e) {
	        		e.printStackTrace();
	        	}
	        }
        }

        System.out.println("Log parser considering " +  _corpus_documents.size() + " files");
        System.out.println("Corpus document size: " + _corpus_documents.size()); 
        
        for (int i = 0; i < _corpus_documents.size(); i++) {
            _dictionary.put(_corpus_documents.get(i).replaceAll(".html", ""), i);
            _map.put(i, 0);
        }

        // Crawl through the logs
        for (File log : logfiles ) {
            try (BufferedReader br = new BufferedReader(new FileReader(log))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] entry = line.split(" ");

                    // Attempt to parse numviews int from string
                    int numviews = 0;
                    try {
                        numviews = Integer.parseInt(entry[2]);
                    } catch (Exception e) {
                        System.out.println("Invalid Line: " + line);
                    }

                    // Try to parse a URL value
                    String cleanUrl;
                    try {
                        cleanUrl = URLDecoder.decode(entry[1], "UTF-8");
                    } catch(Exception e) {
                        cleanUrl = entry[1];
                    }

                    // If map has the URL, update the views count with the parsed numviews
                    if (_dictionary.containsKey(cleanUrl)) {
                        Integer docid = _dictionary.get(cleanUrl);
                        Integer views = _map.get(docid);
                        views += numviews;
                        _map.put(docid, views);
                    }

                }
            }

        }


        for (int i = 0; i < _corpus_documents.size(); i++) {
            int numViews = _map.get(i);
            _views.add(numViews);
        }

        // Save HashMap with counts
        FileOutputStream toSave = new FileOutputStream(this._options._corpusPrefix.replace("data/", "") + "_map.ser");
        ObjectOutputStream objToSave = new ObjectOutputStream(toSave);
        objToSave.writeObject(_views);
        objToSave.close();


        createTextOutput();
    }

    /**
     * During indexing mode, this function loads the NumViews values computed
     * during mining mode to be used by the indexer.
     *
     * @throws IOException
     */
    @Override

    public Object load() throws IOException {
        System.out.println("Loading using " + this.getClass().getName());

        FileInputStream fis = new FileInputStream(this._options._corpusPrefix.replace("data/", "") + "_map.ser");
        ObjectInputStream ois = new ObjectInputStream(fis);
        Vector<Integer> map = null;
        try {
            map = (Vector<Integer>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ois.close();

        return map;
    }
    
  	private String[] parseDocument(File doc) throws Exception {
  		return cleanDocument(Jsoup.parse(doc, "UTF-8").text()).split("\\s+");
  	}
  	
  	private String cleanDocument(String document_body) {
  		return document_body.replaceAll("[^A-Za-z0-9\\s]", "").toLowerCase();
  	}

    public void createTextOutput() {
        String textFilePath = this._options._corpusPrefix.replace("data/", "") + "_numviews.txt";
        FileWriter writer = null;
        try {
            writer = new FileWriter(textFilePath, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedWriter bufferWriter = new BufferedWriter(writer);
        StringBuilder message = new StringBuilder();

        for (int i = 0; i < _corpus_documents.size(); i++) {
            int numViews = _map.get(i);
            message.append(numViews + "\n");
        }

        try {
            bufferWriter.write(message.toString());
            bufferWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
