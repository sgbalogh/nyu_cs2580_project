package edu.nyu.cs.cs2580;

import java.io.Serializable;
import java.util.*;

/**
 * Created by stephen on 12/2/16.
 */
public class GeoEntity implements Serializable {

    // ID assigned by GeoNames
    private Integer id;

    private String primaryName;

    //========================
    //Unique Name
    //=======================
    private String uniqueName;

    public String countryCode;

    // Admin1 is STATE CODE (FIPS), e.g. "US.MO"
    public String admin1Code;

    // Admin2 is COUNTY CODE, e.g. "US.AL.017"
    public String admin2Code;

    public Integer population;
    public Integer elevation;
    public String timezone;

    // Type options are: CITY, COUNTY, STATE, NATION
    public String type;

    public Double northing;
    public Double easting;
    public String mapDatum;

    public Double latitude;
    public Double longitude;

    // Tree / Graph aspects:
    public GeoEntity parent;
    public LinkedList<GeoEntity> children;
    public LinkedList<GeoEntity> nearby;


    public GeoEntity(Integer id, String primaryName, String type) {
        this.id = id;
        this.primaryName = primaryName;
        this.type = type;
        this.children = new LinkedList<>();
        this.nearby = new LinkedList<>();
    }


    public Integer getId() {
        return this.id;
    }

    public String getName() { return this.primaryName; }

    public String getParentName() {
        if (this.parent != null) {
            return this.parent.getName();
        }
        return "";
    }

    public List<GeoEntity> getNearbyCities(int max) {
        if (this.nearby.size() > 0) {
            int stop_int = (max > this.nearby.size()) ? this.nearby.size() : max;
            ArrayList<GeoEntity> toReturn = new ArrayList<>();
            for (int i = 0; i < stop_int; i++) {
                toReturn.add(this.nearby.get(i));
            }
            return toReturn;
        }
        return null;
    }

    // Full expand provides same functionality as getNearbyCities, but also
    // returns the parent county and state (if they exist)
    public List<GeoEntity> fullExpand(int maxcities) {
        ArrayList<GeoEntity> toReturn = new ArrayList<>();
        if (this.nearby.size() > 0) {
            int stop_int = (maxcities > this.nearby.size()) ? this.nearby.size() : maxcities;
            for (int i = 0; i < stop_int; i++) {
                toReturn.add(this.nearby.get(i));
            }
            //return toReturn;
        }
        if (this.type == "CITY" && this.getCounty() != null) {
            toReturn.add(this.getCounty());
        }
        if ((this.type == "CITY" || this.type == "COUNTY") && this.getState() != null) {
            // toReturn.add(this.getState());
        }
        if ((this.type == "STATE")) {
            ArrayList<GeoEntity> all_cities = new ArrayList<>();
            ArrayList<GeoEntity> all_actual_cities = new ArrayList<>();
            List<GeoEntity> state_child = this.children;
            HashMap<String, Integer> _county_populations = new HashMap<>();
            for (GeoEntity child : state_child) {
                int county_pop = 0;
                for (GeoEntity city : child.children) {
                    county_pop += city.population;
                    all_actual_cities.add(city);
                }
                _county_populations.put(child.getName(), county_pop);
                all_cities.add(child);
            }

            all_cities.sort(new Comparator<GeoEntity>() {
                @Override
                public int compare(GeoEntity o1, GeoEntity o2) {
                    return Integer.compare(_county_populations.get(o2.getName()), _county_populations.get(o1.getName()));
                }
            });
            int stop_int = (maxcities > all_cities.size() ? all_cities.size() : maxcities);
            for (int i = 0; i < stop_int; i++) {
                toReturn.add(all_cities.get(i));
            }

            all_actual_cities.sort(new Comparator<GeoEntity>() {
                @Override
                public int compare(GeoEntity o1, GeoEntity o2) {
                    return Integer.compare(o2.population, o1.population);
                }
            });
            stop_int = (maxcities > all_actual_cities.size() ? all_actual_cities.size() : maxcities);
            for (int i = 0; i < stop_int; i++) {
                toReturn.add(all_actual_cities.get(i));
            }

            return toReturn;
        }
        if ((this.type == "COUNTY")) {
            ArrayList<GeoEntity> all_cities = new ArrayList<>();
            all_cities.addAll(this.children);
            all_cities.sort(new Comparator<GeoEntity>() {
                @Override
                public int compare(GeoEntity o1, GeoEntity o2) {
                    return Integer.compare(o2.population, o1.population);
                }
            });
            int stop_int = (maxcities > all_cities.size() ? all_cities.size() : maxcities);
            for (int i = 0; i < stop_int; i++) {
                toReturn.add(all_cities.get(i));
            }
        }
        return toReturn;
    }

    public boolean isSameOrDescendantOf(GeoEntity that) {
        boolean toReturn = false;
        if (this.equals(that)) {
            toReturn = true;
        } else if (this.parent != null) {
            toReturn = this.parent.isSameOrDescendantOf(that);
        }
        return toReturn;
    }

    public GeoEntity getState() {
        GeoEntity toReturn = null;
        if (this.type.equals("STATE")) {
            toReturn = this;
        } else if (this.type.equals("CITY") || this.type.equals("COUNTY")) {
            toReturn = this.parent.getState();
        }
        return toReturn;
    }

    public GeoEntity getCounty() {
        GeoEntity toReturn = null;
        if (this.type.equals("COUNTY")) {
            toReturn = this;
        } else if (this.type.equals("CITY")) {
            toReturn = this.parent.getCounty();
        }
        return toReturn;
    }

    public String getStateName() {
        String toReturn = "";
        if (this.type.equals("STATE")) {
            toReturn = this.getName();
        } else if (this.type.equals("CITY") || this.type.equals("COUNTY")) {
            toReturn = this.parent.getStateName();
        }
        return toReturn;
    }

    public String getCountyName() {
        String toReturn = "";
        if (this.type.equals("COUNTY")) {
            toReturn = this.getName();
        } else if (this.type.equals("CITY")) {
            toReturn = this.parent.getCountyName();
        }
        return toReturn;
    }

    public String[] getNearbyCityNames(int max) {
        if (this.nearby.size() > 0) {
            int stop_int = (max > this.nearby.size()) ? this.nearby.size() : max;
            String[] toReturn = new String[stop_int];
            for (int i = 0; i < stop_int; i++) {
                toReturn[i] = this.nearby.get(i).getName();
            }
            return toReturn;
        }
        return null;
    }

    public String getNearbyCitiesGEOJSON(int max) {
        if (this.nearby.size() > 0) {
            StringBuilder json = new StringBuilder();
            json.append("{ \"type\": \"FeatureCollection\", \"features\": [");
            int stop_int = (max > this.nearby.size()) ? this.nearby.size() : max;
            json.append("{ \"type\": \"Feature\", \"id\": \"")
                    .append(this.getId())
                    .append("\", \"geometry\": { \"type\": \"Point\", \"coordinates\": [ ")
                    .append(this.longitude).append(", ").append(this.latitude).append("]}")
                    .append(", \"properties\": { \"name\": \"").append(this.getName())
                    .append("\", \"type\": \"primary\", \"population\": ").append(this.population).append("}}");
            for (int i = 0; i < stop_int; i++) {
                GeoEntity nearby = this.nearby.get(i);
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


    public String[] getChildrenNames(int max) {
        if (this.children.size() > 0) {
            int stop_int = (max > this.nearby.size()) ? this.nearby.size() : max;
            String[] toReturn = new String[stop_int];
        }
        return null;
    }


    public String getUniqueName() {
        return uniqueName;
    }


    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }


}
