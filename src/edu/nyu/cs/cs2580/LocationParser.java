package edu.nyu.cs.cs2580;
import java.util.*;
import java.io.*;
/**
 * Created by stephen on 12/3/16.
 */
public class LocationParser {

    private SpatialEntityKnowledgeBase _gkb;
    private Indexer _indexer;
    private String givenQuery;
    private String[] tokens;
    private int[] lengthOfCandidateTerm = new int[] {1,2,3};
    private List<String> listOfCandidateLocation = new ArrayList<>();

    public LocationParser(Indexer indexer, SpatialEntityKnowledgeBase gkb) {
        _indexer = indexer;
        _gkb = gkb;

    }

    public QueryBoolGeo parseQuery(String inputString) {
        QueryBoolGeo toReturn = new QueryBoolGeo(inputString);
        return toReturn;
    }

    public void segmentThisQuery(String s){
        givenQuery = s;
        tokens = givenQuery.split("\\s");
        int length = tokens.length;
        String currentStringTested="";
        String currentStringTested2="";
        int freq1=0;
        int freq2=0;
        int[] spaces = new int[length-2];
        for(int i=0;i<spaces.length;i++)
            spaces[i] = 0;

        int currentLengthOfCnadidate = 2;
        for(int i=0; i< (length - currentLengthOfCnadidate + 1 - 1); i++){
                currentStringTested = tokens[i] + tokens[i+1];
                currentStringTested2 = tokens[i+1] + tokens[i+2];
                freq1 = _indexer.corpusTermFrequency(currentStringTested);
                freq2 = _indexer.corpusTermFrequency(currentStringTested2);
                if(freq1 < freq2){

                    spaces[i] += 1;
                }
                else {

                    spaces[i + 1] += 1;
                }
        }

        currentLengthOfCnadidate = 3;

        for(int i=0; i< (length - currentLengthOfCnadidate - 1); i++){
            currentStringTested = tokens[i] + tokens[i+1] + tokens[i+2];
            currentStringTested2 = tokens[i+1] + tokens[i+2] + tokens[i+3];
            freq1 = _indexer.corpusTermFrequency(currentStringTested);
            freq2 = _indexer.corpusTermFrequency(currentStringTested2);
            if(freq1 < freq2){

                spaces[i] += 1;
            }
            else {

                spaces[i + 2] += 1;
            }
        }

        String pendingToken = tokens[0];
        for(int i=0; i< (length-2); i++){
            if(spaces[i] == 0 ){
                pendingToken += tokens[i+1];
            }
            else{
                listOfCandidateLocation.add(pendingToken);
                pendingToken = tokens[i+1];
            }
        }
    }

    public void forEachSegments(){
        List<String> location_terms = new ArrayList<>();
        List<String> non_location_terms = new ArrayList<>();
        for(String s: listOfCandidateLocation){
            List<GeoEntity> localList = _gkb.getCandidates(s);
            if(localList.isEmpty()){
                non_location_terms.add(s);
            }
            else{
                location_terms.add(s);
            }
        }

    }


    //===============================================
    // INCLUDE METHOD FOR STATISTICAL SEGMENTATION
    //===============================================



}
