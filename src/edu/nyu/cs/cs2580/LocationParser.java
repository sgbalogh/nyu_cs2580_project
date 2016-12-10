package edu.nyu.cs.cs2580;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by stephen on 12/3/16.
 */
public class LocationParser {

    private SpatialEntityKnowledgeBase _gkb;
    private Indexer _indexer;
    
    private double threshold; //threshold for votes to be qualified a break
    private int maxN = 3;

    public LocationParser(Indexer indexer, SpatialEntityKnowledgeBase gkb) {
        _indexer = indexer;
        _gkb = gkb;
        threshold = 3; 
    }

    public QueryBoolGeo parseQuery(String inputString) {
        QueryBoolGeo toReturn = new QueryBoolGeo(inputString);
        
        //TODO: this needs to be able to deal with quotation/phrase searches
        for(String term : processQuery(toReturn)) { //statisticalSegmentation(inputString)) {
        	System.out.println("Term: " + term);
        	List<GeoEntity> candidates = _gkb.getCandidates(term);
        	if(candidates.isEmpty())
        		toReturn._tokens.add(term);
        	else
        		toReturn.populateGeoEntities(candidates);
        }
        
        System.out.println(toReturn._tokens.toString()); 
        
        for(GeoEntity ge : toReturn.get_candidate_geo_entities()) {
        	System.out.println("Location: " + ge.getName());
        }
        
        return toReturn;
    }
    
    //Query Phrase normal
    public Vector<String> processQuery(QueryBoolGeo query) {
    	Vector<String> results = new Vector<String>();
    	if (query._query == null) {
    		return results;
    	}

    	//Split by spaces or quotes
    	Matcher m = Pattern.compile("([^\"\\s]+|[^\"]\\S+|\".*?\")\\s*").matcher(query._query);
    	while (m.find()) {
    		String token = m.group(1).toLowerCase().replaceAll("[^A-Za-z0-9\"\\s]", "");

    		//If starts with quotes, it's a phrase
    		if(token.startsWith("\"")) {
    			token = token.replace("\"", "").trim();
    		} else {
    			token = token.trim();
    		}

    		if(token.length() > 0)
    			results.add(token);
    	}
    	
    	return results;
    }

    //===============================================
    // INCLUDE METHOD FOR STATISTICAL SEGMENTATION
    //===============================================
}
