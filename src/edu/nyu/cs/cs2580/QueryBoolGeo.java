package edu.nyu.cs.cs2580;

import java.util.List;

/**
 * Created by stephen on 12/3/16.
 */
public class QueryBoolGeo extends Query{

    public List<String> _input_strings;
    private List<GeoEntity> _candidate_geo_entities;
    private List<GeoEntity> _expanded_geo_entities;

    public QueryBoolGeo(String inputString, List<String> inputStrings, List<GeoEntity> inputGeoEntities) {
        super(inputString);
        //DO NOT PROCESS INPUT STRING

        _input_strings = inputStrings;
        _candidate_geo_entities = inputGeoEntities;

    }

    public void populateGeoEntities(List<GeoEntity> geoEntities) {
        _candidate_geo_entities = geoEntities;
    }


    public List<GeoEntity> get_expanded_geo_entities() {
        return _expanded_geo_entities;
    }

    public void set_expanded_geo_entities(List<GeoEntity> _expanded_geo_entities) {
        this._expanded_geo_entities = _expanded_geo_entities;
    }

    public GeoEntity get_candidate_geo_entity(int index_to_grab) {
        return _candidate_geo_entities.get(index_to_grab);
    }

    public void set_candidate_geo_entities(List<GeoEntity> _candidate_geo_entities) {
        this._candidate_geo_entities = _candidate_geo_entities;
    }
}
