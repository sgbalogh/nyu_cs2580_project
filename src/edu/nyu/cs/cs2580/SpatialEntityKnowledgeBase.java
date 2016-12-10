package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;

/**
 * Created by stephen on 12/2/16.
 */
public class SpatialEntityKnowledgeBase implements Serializable {
    private HashMap<Integer, GeoEntity> _entity_map; // GeoName ID to corresponding GeoEntity
    private HashMap<String, GeoEntity> _us_county_map; // County code to corresponding GeoEntity, e.g. "US.GA.005" -> GeoEntity
    private HashMap<String, GeoEntity> _us_state_map; // State abbr. to corresponding GeoEntity, e.g. "NY" -> GeoEntity
    private HashMap<String, List<GeoEntity>> _term_search_map; // Given a term (e.g. "springfield"), find candidate GeoEntities

    public SpatialEntityKnowledgeBase() {
        this._entity_map = new HashMap<>();
        this._us_county_map = new HashMap<>();
        this._us_state_map = new HashMap<>();
        this._term_search_map = new HashMap<>();
    }

    public static String makeGeoJSON(List<GeoEntity> geoentities) {
        // NOTE: We assume first GeoEntity in the list is the "primary" one (which was searched by user)
        if (geoentities.size() > 0) {
            StringBuilder json = new StringBuilder();
            json.append("{ \"type\": \"FeatureCollection\", \"features\": [");
            int stop_int = geoentities.size();
            json.append("{ \"type\": \"Feature\", \"id\": \"")
                    .append(geoentities.get(0).getId())
                    .append("\", \"geometry\": { \"type\": \"Point\", \"coordinates\": [ ")
                    .append(geoentities.get(0).longitude).append(", ").append(geoentities.get(0).latitude).append("]}")
                    .append(", \"properties\": { \"name\": \"").append(geoentities.get(0).getName())
                    .append("\", \"type\": \"primary\", \"population\": ").append(geoentities.get(0).population).append("}}");
            for (int i = 1; i < stop_int; i++) {
                GeoEntity nearby = geoentities.get(i);
                json.append(",{ \"type\": \"Feature\", \"id\": \"")
                        .append(nearby.getId())
                        .append("\", \"geometry\": { \"type\": \"Point\", \"coordinates\": [ ")
                        .append(nearby.longitude).append(", ").append(nearby.latitude).append("]}")
                        .append(", \"properties\": { \"name\": \"").append(nearby.getName())
                        .append("\", \"type\": \"expanded\", \"population\": ").append(nearby.population).append("}}");
            }
            json.append("]}");
            return json.toString();
        }
        return null;
    }

    public boolean addEntityToMap(GeoEntity toAdd) {
        if (_entity_map.containsKey(toAdd.getId())) {
            return false;
        } else {
            _entity_map.put(toAdd.getId(), toAdd);
            updateLookupMaps(toAdd);
            return true;
        }
    }

    private void updateLookupMaps(GeoEntity toAdd) {
        if (toAdd.type.equals("COUNTY")) {
            _us_county_map.put(toAdd.admin2Code, toAdd);
        } else if (toAdd.type.equals("STATE")) {
            _us_state_map.put(toAdd.admin1Code, toAdd);
        }
    }

    private void constructTermMap() {
        for (Map.Entry<Integer, GeoEntity> entry : _entity_map.entrySet()) {
            GeoEntity entity = entry.getValue();
            String key = entity.getName().toLowerCase();
            if (_term_search_map.containsKey(key)) {
                _term_search_map.get(key).add(entity);
            } else {
                ArrayList<GeoEntity> toAdd = new ArrayList<>();
                toAdd.add(entity);
                _term_search_map.put(key, toAdd);
            }
            String[] split_key = key.split(" ");
            if (split_key.length > 1) {
                for (int i = 0; i < split_key.length; i++) {
                    String key_segment = split_key[i];
                    if (_term_search_map.containsKey(key_segment)) {
                        _term_search_map.get(key_segment).add(entity);
                    } else {
                        ArrayList<GeoEntity> toAdd = new ArrayList<>();
                        toAdd.add(entity);
                        _term_search_map.put(key_segment, toAdd);
                    }
                }
            }
        }
    }

    public List<GeoEntity> getCandidates(String term) {
        List<GeoEntity> candidates;
        if (this._term_search_map.containsKey(term)) {
            candidates = this._term_search_map.get(term);
        } else {
            candidates = new ArrayList<>();
        }
        return candidates;
    }

    public boolean constructTree() {

        for (Map.Entry<Integer, GeoEntity> entry : _entity_map.entrySet()) {
            Integer key = entry.getKey();
            GeoEntity entity = entry.getValue();
            GeoEntity parent = null;

            if (entity.type.equals("CITY")) {
                String admin2Code;
                if (entity.admin2Code.length() == 1) {
                    admin2Code = "00" + entity.admin2Code;
                } else if (entity.admin2Code.length() == 2) {
                    admin2Code = "0" + entity.admin2Code;
                } else {
                    admin2Code = entity.admin2Code;
                }
                String lookup_key = "US." + entity.admin1Code + "." + admin2Code;
                if (_us_county_map.containsKey(lookup_key)) {
                    parent = _us_county_map.get(lookup_key);
                    entity.parent = parent;
                    parent.children.add(entity);
                } else {
                    // Deal with edge-cases (NYC and DC)
                    switch (lookup_key) {
                        case "US.DC.001":
                            parent = _entity_map.get(6252001);
                            entity.parent = parent;
                            parent.children.add(entity);
                            break;
                        case "US.NY.":
                            parent = _us_state_map.get("US.NY");
                            entity.parent = parent;
                            parent.children.add(entity);
                            break;
                    }
                }
            } else if (entity.type.equals("COUNTY")) {
                if (_us_state_map.containsKey(entity.admin2Code.substring(0,5))) {
                    parent = this._us_state_map.get(entity.admin2Code.substring(0,5));
                    entity.parent = parent;
                    parent.children.add(entity);
                }
            } else if (entity.type.equals("STATE")) {
                if (_entity_map.containsKey(6252001)) {
                    parent = _entity_map.get(6252001); // Parent is "United States"
                    entity.parent = parent;
                    parent.children.add(entity);
                }
            }

        }
        this.constructTermMap();
        return true;
    }

    public void addNeighbors(HashMap<Integer, LinkedList<Integer>> nearestNeighbors) {
        for (Map.Entry<Integer, LinkedList<Integer>> entry : nearestNeighbors.entrySet()) {
            Integer entityId = entry.getKey();
            LinkedList<Integer> neighborIds = entry.getValue();
            GeoEntity entity = _entity_map.get(entityId);
            for (Integer neighborId : neighborIds) {
                GeoEntity neighbor = _entity_map.get(neighborId);
                entity.nearby.add(neighbor);
            }
        }
    }

    public int getTotalEntityCount() {
        return _entity_map.size();
    }

}
