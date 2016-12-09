package edu.nyu.cs.cs2580;

import java.util.Arrays;

/**
 * Created by stephen on 12/3/16.
 */
public class LocationParser {

    private SpatialEntityKnowledgeBase _gkb;
    private Indexer _indexer;

    public LocationParser(Indexer indexer, SpatialEntityKnowledgeBase gkb) {
        _indexer = indexer;
        _gkb = gkb;

    }

    public QueryBoolGeo parseQuery(String inputString) {
        QueryBoolGeo toReturn = new QueryBoolGeo(inputString);
        
        //Testing
        toReturn.populateInputStrings(Arrays.asList(inputString.split("\\s+")));
        
        return toReturn;
    }

    //===============================================
    // INCLUDE METHOD FOR STATISTICAL SEGMENTATION
    //===============================================

    //Implement Spell-Check: _indexer.
    
    //Do Stopwords


}
