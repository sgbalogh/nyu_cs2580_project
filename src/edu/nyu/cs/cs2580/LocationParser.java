package edu.nyu.cs.cs2580;

import java.util.*;

/**
 * Created by stephen on 12/3/16.
 */
public class LocationParser {

    private SpatialEntityKnowledgeBase _gkb;
    private Indexer _indexer;
    private String[] tokens;
    private int length;
    private List<String> final_location_candidates = new ArrayList<>();

    public LocationParser(Indexer indexer, SpatialEntityKnowledgeBase gkb) {
        _indexer = indexer;
        _gkb = gkb;

    }

    public QueryBoolGeo parseQuery(String inputString) {
        QueryBoolGeo toReturn = new QueryBoolGeo(inputString, null, null);
        return toReturn;
    }

    public void SegmentThisQueryString(String inputString) {
            length = inputString.length();
            String[] tokens = inputString.split("\\s");
            int startIndex, endIndex;
            int nextSegmentIndex=0;
            String candidateOfCandidate="";      //for testing the possible query segment as a valid location candidate
            for(int i=nextSegmentIndex; i<length; i++){

                for(int j=i;j<length; j++){
                    startIndex = i;
                    endIndex = j;
                    if(startIndex == endIndex){
                        candidateOfCandidate = tokens[startIndex];
                    }
                    else{
                        for(int k=startIndex; k<endIndex; k++){
                            candidateOfCandidate += tokens[k] + " ";
                        }
                        candidateOfCandidate += tokens[endIndex];
                    }

                    if(testCandidatePossibility(candidateOfCandidate)) {
                        final_location_candidates.add(candidateOfCandidate);
                        if(j != (length - 1)){
                            nextSegmentIndex = j+1;
                            break;
                        }
                    }
                }
            }



    }

    public boolean testCandidatePossibility(String candidateOfCandidate){

        //here we look up to the geo-spatial data for whether the input string exists as one of the location entity
        return false;

    }



    //===============================================
    // INCLUDE METHOD FOR STATISTICAL SEGMENTATION
    //===============================================



}
