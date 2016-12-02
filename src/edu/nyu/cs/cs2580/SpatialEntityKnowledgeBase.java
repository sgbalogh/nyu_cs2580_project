package edu.nyu.cs.cs2580;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by stephen on 12/2/16.
 */
public class SpatialEntityKnowledgeBase implements Serializable {
    private HashMap<Integer, GeoEntity> _entity_map; // GeoName ID to corresponding GeoEntity
    private HashMap<String, GeoEntity> _us_county_map; // County code to corresponding GeoEntity, e.g. "US.GA.005" -> GeoEntity
    private HashMap<String, GeoEntity> _us_state_map; // State abbr. to corresponding GeoEntity, e.g. "NY" -> GeoEntity

    public SpatialEntityKnowledgeBase() {
        this._entity_map = new HashMap<>();
        this._us_county_map = new HashMap<>();
        this._us_state_map = new HashMap<>();
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



}
