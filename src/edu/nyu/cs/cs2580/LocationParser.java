package edu.nyu.cs.cs2580;

import java.util.Arrays;
import java.util.Vector;

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
        //TODO: this needs to be able to deal with quotation/phrase searches
        toReturn._tokens = new Vector<>();
        //toReturn._tokens.addAll(Arrays.asList(inputString.split("\\s+")));
        toReturn._tokens.add(inputString);

        return toReturn;
    }

    //===============================================
    // INCLUDE METHOD FOR STATISTICAL SEGMENTATION
    //===============================================


}
