package edu.nyu.cs.cs2580;

import java.util.ArrayList;
import java.util.HashMap;
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
    
    public GEO_MODE _presentation_mode = GEO_MODE.NONE; // THIS SHOULD always be true if _expanded_queries.size() > 0
    
    //TODO Need to introduce cache flag which returns cached value and best which returns merged form
    public boolean cache = false;
    public boolean best = false;
    
    public HashMap<Integer, GeoEntity> _ambiguous_candidates;

    public QueryBoolGeo(String inputString) {
        super(inputString);
        //DO NOT PROCESS INPUT STRING
        _candidate_geo_entities = new ArrayList<>();
        _expanded_geo_entities = new ArrayList<>();
    }


    public void uniqify(List<GeoEntity> input) {
        int level = 0;
        HashMap<String, List<GeoEntity>> names = new HashMap<>();

        for (GeoEntity entity : input) {
            String entity_name = entity.getName().toLowerCase().trim();
            if (names.containsKey(entity_name)) {
                names.get(entity_name).add(entity);
            } else {
                List<GeoEntity> toAdd = new ArrayList<>();
                toAdd.add(entity);
                names.put(entity_name, toAdd);
            }
        }

        while (names.size() < input.size() && level < 3) {
            HashMap<String, List<GeoEntity>> temp = new HashMap<>();
            for (String key : names.keySet()) {
                List<GeoEntity> list = names.get(key);
                if (list.size() > 1) {
                    for (GeoEntity ge : list) {
                        String expanded_name;
                        if (level == 0){
                            expanded_name = ge.getName() + " " + ge.getStateName();
                        } else {
                            expanded_name = ge.getName() + " " + ge.getCountyName() + " " + ge.getStateName();
                        }
                        if (temp.containsKey(expanded_name)) {
                            temp.get(expanded_name).add(ge);
                        } else {
                            List<GeoEntity> tempToAdd = new ArrayList<>();
                            tempToAdd.add(ge);
                            temp.put(expanded_name, tempToAdd);
                        }
                    }
                } else {
                    temp.put(key, list);
                }

            }
            names = temp;
            level++;
        }

        // At this point, names should be a hashmap with as many keys as there are input GEs,
        // so we can easily convert it into a hashmap from string to a single GeoEntity

        /*
        HashMap<String, GeoEntity> toReturn = new HashMap<>();
        for ( String key : names.keySet()) {
            toReturn.put(key, names.get(key).get(0));
        }
        return toReturn;
        */

        _ambiguous_candidates = new HashMap<>();
        for (String key : names.keySet()) {
        	GeoEntity ge = names.get(key).get(0);
        	ge.setUniqueName(key.toLowerCase().trim());
        	_ambiguous_candidates.put(names.get(key).get(0).getId(), ge);
        }
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
        	_expanded_geo_entities.addAll((ArrayList) _candidate_geo_entities.get(i).fullExpand(max));
        }
        System.out.println("Expanded Size: " + _expanded_geo_entities.size());
    }

}
