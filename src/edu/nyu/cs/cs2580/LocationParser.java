package edu.nyu.cs.cs2580;

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
        return toReturn;
    }

    //===============================================
    // INCLUDE METHOD FOR STATISTICAL SEGMENTATION
    //===============================================



}
