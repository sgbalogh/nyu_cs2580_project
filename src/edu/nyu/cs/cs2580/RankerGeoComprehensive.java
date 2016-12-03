package edu.nyu.cs.cs2580;

import java.util.Vector;

/**
 * Created by stephen on 12/3/16.
 */
//TODO: implement this
    //Algorithm: QL and PageRank data (modeled on previous Comprehensive)
    // THIS RANKER IS RESPONSIBLE FOR CHOOSING THE CANDIDATE TO EXPAND ON
    // ... AND WHETHER OR NOT TO EXPAND GIVEN CONDITIONS
        // 1) not enough results
        // 2) reasonably think its a place
            // During scoring candidates, vote by majority frequency, winner takes all
    // SUPPORT BOOLEAN
public class RankerGeoComprehensive extends Ranker {


    public RankerGeoComprehensive(SearchEngine.Options options,
                               QueryHandler.CgiArguments arguments, Indexer indexer) {
        super(options, arguments, indexer);
        System.out.println("Using Ranker: " + this.getClass().getSimpleName());
    }

    @Override
    public Vector<ScoredDocument> runQuery(Query query, int numResults) {
        if (! (query instanceof QueryBoolGeo))
            throw new IllegalStateException("Pass in QBG");
        return null;
    }

    private void expand(QueryBoolGeo query, int index_to_expand, int max_expand_locations) {
        GeoEntity toExpand = query.get_candidate_geo_entity(index_to_expand);
        query.set_expanded_geo_entities(toExpand.getNearbyCities(max_expand_locations));
    }



}
