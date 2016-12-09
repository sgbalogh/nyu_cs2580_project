package edu.nyu.cs.cs2580.utilities.crawler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by stephen on 12/9/16.
 */
public class Controller {

    private ArrayList<String> _queue;
    private ArrayList<String> _pdf_queue;
    private HashMap<String, Integer> _map;
    private HashMap<String, Integer> _pdf_map;
    private String _tld = null;

    public Controller(String tld) {
        this._queue = new ArrayList<>();
        this._map = new HashMap<>();
        this._pdf_queue = new ArrayList<>();
        this._pdf_map = new HashMap<>();
        this._tld = tld;

        // We can manually enter seed URLs below:
        this.enter("https://en.wikipedia.org/wiki/Seattle_metropolitan_area");
        this.enter("https://en.wikipedia.org/wiki/Timeline_of_Seattle");
        this.enter("https://en.wikipedia.org/wiki/Miami_metropolitan_area");
        this.enter("https://en.wikipedia.org/wiki/South_Florida");
        this.enter("https://en.wikipedia.org/wiki/Delaware_Valley");
        this.enter("https://en.wikipedia.org/wiki/Connecticut");
        this.enter("https://en.wikipedia.org/wiki/Northeast_blackout_of_2003");
    }

    public boolean handle(String input) {
        String URL = cleanURL(input);
        if (URL != null) {
            enter(input);
            enterPdf(input);
        }
        return true;
    }

    public String grab() {
        return this._queue.remove(0);
    }

    public String grabRandom() {
        return this._queue.remove((int) (Math.random() * this._queue.size() - 1));
    }

    public String grabPdf() { return this._pdf_queue.remove(0);}

    public boolean hasPdf() {
        return (this._pdf_queue.size() > 0);
    }

    private String cleanURL(String input) {
        URI toReturn = null;

        try {
            toReturn = new URI(input).normalize();
        } catch (URISyntaxException e) {
            return null;
        }

        if (toReturn.getFragment() == null) {
            return toReturn.normalize().toString();
        } else {
            return toReturn.normalize().toString().replace("#" + toReturn.getFragment(), "");
        }

    }

    public int getNumLinks() {
        return _queue.size();
    }

    private boolean enter(String input) {
        boolean toReturn = false;
        if (input.contains(_tld)) {
            if (_map.containsKey(input)) {
                int count = _map.get(input);
                _map.put(input, (count + 1));

            } else {
                _map.put(input, 1);
                _queue.add(input);
                toReturn = true;
            }
        }
        return toReturn;
    }

    private boolean enterPdf(String input) {
        boolean toReturn = false;
        if (input.contains(_tld) && input.contains(".pdf") && input.contains("nyu.edu")) {
            if (_pdf_map.containsKey(input)) {
                int count = _pdf_map.get(input);
                _pdf_map.put(input, (count + 1));

            } else {
                _pdf_map.put(input, 1);
                _pdf_queue.add(input);
                toReturn = true;
            }
        }
        return toReturn;
    }




}
