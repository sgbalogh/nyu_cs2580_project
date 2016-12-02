package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by stephen on 12/2/16.
 */
public class LocationLoader {


    public static void loadLocations() {

        SpatialEntityKnowledgeBase knowledgeBase = new SpatialEntityKnowledgeBase();
        HashMap<Integer, LinkedList<Integer>> nearestNeighbors = new HashMap<>();

        String csvFileCities = "/Users/stephen/Desktop/search_engine_proj/cities1000_usa_3857.csv";
        String csvFileCounties = "/Users/stephen/Desktop/search_engine_proj/admin2_data.csv";
        String csvFileStates = "/Users/stephen/Desktop/search_engine_proj/admin1_data.csv";
        String csvFileRelations = "/Users/stephen/Desktop/search_engine_proj/nearest_neighbors_50km.csv";

        String line = "";
        String csvSplitter = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFileCities))) {
            Integer line_num = 0;
            while ((line = br.readLine()) != null) {
                line_num++;
                String[] parsedLine = line.split(csvSplitter);
                if (line_num != 1) {
                    Double easting = Double.parseDouble(parsedLine[0]);
                    Double northing = Double.parseDouble(parsedLine[1]);
                    Integer id = Integer.parseInt(parsedLine[2]);
                    String name = parsedLine[3];
                    Double latitude = Double.parseDouble(parsedLine[5]);
                    Double longitude = Double.parseDouble(parsedLine[6]);
                    String country = parsedLine[7];
                    String admin1Code = parsedLine[8];
                    String admin2Code = parsedLine[9];
                    Integer population = Integer.parseInt(parsedLine[12]);
                    Integer elevation = Integer.parseInt(parsedLine[13]);
                    String timezone = parsedLine[15];

                    GeoEntity toAdd = new GeoEntity(id, name, "CITY");
                    toAdd.easting = easting;
                    toAdd.northing = northing;
                    toAdd.mapDatum = "EPSG:3857";
                    toAdd.latitude = latitude;
                    toAdd.longitude = longitude;
                    toAdd.countryCode = country;
                    toAdd.admin1Code = admin1Code;
                    toAdd.admin2Code = admin2Code;
                    toAdd.population = population;
                    toAdd.elevation = elevation;
                    toAdd.timezone = timezone;

                    knowledgeBase.addEntityToMap(toAdd);
                    // System.out.println("Successfully added city line " + line_num);
                }
            }

        } catch (IOException e) {
        }

        System.out.println("Finished adding cities...");
        System.out.println("Starting to add counties...");

        try (BufferedReader br = new BufferedReader(new FileReader(csvFileCounties))) {
            Integer line_num = 0;
            while ((line = br.readLine()) != null) {
                line_num++;
                String[] parsedLine = line.split(csvSplitter);
                if (line_num != 1) {

                    String code = parsedLine[0];
                    if (code.contains("US.")) {
                        String name = parsedLine[2];
                        Integer id = Integer.parseInt(parsedLine[3]);
                        GeoEntity toAdd = new GeoEntity(id, name, "COUNTY");
                        toAdd.admin2Code = code;

                        knowledgeBase.addEntityToMap(toAdd);
                        // System.out.println("Successfully added county line " + line_num);
                    }
                }
            }

        } catch (IOException e) {
        }

        System.out.println("Finished adding counties...");
        System.out.println("Starting to add states...");

        try (BufferedReader br = new BufferedReader(new FileReader(csvFileStates))) {
            Integer line_num = 0;
            while ((line = br.readLine()) != null) {
                line_num++;
                String[] parsedLine = line.split(csvSplitter);
                if (line_num != 1) {

                    String code = parsedLine[0];
                    if (code.contains("US.")) {
                        String name = parsedLine[2];
                        Integer id = Integer.parseInt(parsedLine[3]);
                        GeoEntity toAdd = new GeoEntity(id, name, "STATE");
                        toAdd.admin1Code = code;

                        knowledgeBase.addEntityToMap(toAdd);
                        //System.out.println("Successfully added state line " + line_num);
                    }
                }
            }

        } catch (IOException e) {
        }

        System.out.println("Finished adding states...");

        System.out.println("Adding nation...");
        GeoEntity nation = new GeoEntity(6252001, "United States of America", "NATION");
        knowledgeBase.addEntityToMap(nation);

        System.out.println("Connecting parents and children...");
        knowledgeBase.constructTree();

        System.out.println("Reading relations...");

        try (BufferedReader br = new BufferedReader(new FileReader(csvFileRelations))) {
            Integer line_num = 0;
            while ((line = br.readLine()) != null) {

                line_num++;
                if (!line.toString().equals("")) {
                    String[] parsedLine = line.split(csvSplitter);
                    String[] parsedNeighbors = parsedLine[1].split(":");
                    Integer entity = Integer.parseInt(parsedLine[0]);
                    LinkedList<Integer> neighbors = new LinkedList<>();
                    for (String string : parsedNeighbors) {
                        neighbors.add(Integer.parseInt(string));
                    }
                    if (entity != null) {
                        nearestNeighbors.put(entity, neighbors);
                    }
                    //System.out.println(line_num);
                }
            }

        } catch (IOException e) {
        }

        knowledgeBase.addNeighbors(nearestNeighbors);

        System.out.println("All done!");


    }

}
