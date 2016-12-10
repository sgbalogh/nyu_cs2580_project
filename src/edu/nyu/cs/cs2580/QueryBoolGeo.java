package edu.nyu.cs.cs2580;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by stephen on 12/3/16.
 */
public class QueryBoolGeo extends Query{

    // For example query "jersey city zoo"...
    ///
    // QueryHandler creates a QGB that contains the original query string,
    // this object is then passed as an argument to LocationParser,
    // LocationParser performs statistical segmentation on the terms in the query string,
    // LocationParser adds back to the QBG instance the list of expanded GeoEntities,
    // QBG then creates a List of expanded strings that represent the merging of non spatial
    // original query terms and expanded query terms (i.e. {"bronx zoo", "manhattan zoo", ...})
    //
    // The QBG that was passed to LocationParser is then passed back to the QueryHandler, which
    // passes the QBG to the Ranker

    // QBG needs to separate original query string from expanded query strings (so that the Ranker
    // can disambiguate between them)

    private List<GeoEntity> _candidate_geo_entities; // This should hold all candidate GeoEntities for "jersey city"...
    // { Jersey City, NJ ; Jersey City, Utah ; etc..}
    private List<GeoEntity> _expanded_geo_entities;

    public boolean _should_present; // THIS SHOULD always be true if _expanded_queries.size() > 0
    //TODO Need to introduce cache flag which returns cached value and best which returns merged form
    public boolean cache = false;
    public boolean best = false;

    public QueryBoolGeo(String inputString) {
        super(inputString);
        //DO NOT PROCESS INPUT STRING
        _should_present = false;
        _candidate_geo_entities = new ArrayList<>();
        _expanded_geo_entities = new ArrayList<>();
    }

    // THESE ARE THE METHODS THAT LOCATIONPARSER WILL USE :
    public void populateGeoEntities(List<GeoEntity> geoEntities) {
        _candidate_geo_entities.addAll(geoEntities);
    }

    public List<GeoEntity> get_candidate_geo_entities() {
        return this._candidate_geo_entities;
    }


    // THESE ARE THE METHODS THAT RANKERGEOCOMPREHENSIVE WILL USE:
    public List<GeoEntity> get_expanded_geo_entities () {
        return _expanded_geo_entities;
    }

    // TAKES THE CANDIDATE GEO ENTITIES to max degree (~2 - 3), EXPANDS ALL OF THEM, AND CREATES NEW
    // QUERY STRINGS THAT POPULATE _expanded_queries
    //TODO: make sure that max refers to the total number of expanded entities, not total per entity
    public void expand(int max) {
        for (int i = 0; i < Math.min( _candidate_geo_entities.size(), max); i++) {
        	_expanded_geo_entities.addAll((ArrayList) _candidate_geo_entities.get(i).fullExpand(3));
        }
        System.out.println("Expanded Size: " + _expanded_geo_entities.size());
    }

}
