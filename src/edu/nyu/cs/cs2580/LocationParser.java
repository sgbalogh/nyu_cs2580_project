package edu.nyu.cs.cs2580;
import java.util.*;
/**
 * Created by stephen on 12/3/16.
 */
public class LocationParser {

	private SpatialEntityKnowledgeBase _gkb;
	private Indexer _indexer;

	public LocationParser(Indexer indexer, SpatialEntityKnowledgeBase gkb) {
		_indexer = indexer;
		_gkb = gkb;
	}
	
	//Logistic Regression Test
	private QueryBoolGeo langModel(String givenQuery) {
		givenQuery = givenQuery.toLowerCase();
		int k = 3; //Max N-Gram Size 
		try {
			String[] seperatedTerms = givenQuery.split("\\s+");
			double[] scores = new double[seperatedTerms.length];
			int[] pointers = new int[seperatedTerms.length];
			
			System.out.println("Orig Terms: " + givenQuery);
			
			//Segment Strings
			for(int ind = 0; ind < scores.length; ind++) {
				double bestScore = Double.NEGATIVE_INFINITY;
				int bestPt = 0;
				String term = seperatedTerms[ind];
				String remainingTerm = "";
				for(int n = ind; n >= Math.max(0, ind - k + 1); n--) {
					if(n != ind) {
						term = seperatedTerms[n] + " " + term;
						remainingTerm = seperatedTerms[n] + " " + remainingTerm;
					}
					double scoreTest = score(term, remainingTerm);
					double score = scoreTest + (n - 1 >= 0? scores[n - 1]: 0);
					if(score > bestScore) {
						bestScore = score;
						bestPt = n;
					}
				}
				scores[ind] = bestScore;
				pointers[ind] = bestPt;
			}
			
			ArrayList<String> segmentedTerms = new ArrayList<String>();

			//Extract Terms
			for(int counter = pointers.length - 1; counter >= 0; counter--) {
				String buff = "";
				int start = pointers[counter];
				while(counter >= start) {
					if(buff.length() == 0)
						buff = seperatedTerms[counter];
					else
						buff = seperatedTerms[counter] + " " + buff;
					
					if(counter == start)
						break;
					
					counter--;
				}
				segmentedTerms.add(0, buff);
			}
			
			System.out.println("Segmented Terms: " + segmentedTerms.toString());
			
			//Check for Locations
			QueryBoolGeo toReturn = new QueryBoolGeo(givenQuery);
			
			for(String term : segmentedTerms) {
				List<GeoEntity> cands = _gkb.getCandidates(term);
				if(cands.isEmpty()) {
					toReturn.getSupportingTokens().add(term);
				} else {
					toReturn.populateGeoEntities(cands);
				}
				toReturn._tokens.add(term);
			}
			
			return toReturn;
		} catch( Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	//Simple Probability + Laplacian Smoothing 
	private double score(String term, String remainingTerm) { //Simple 
		try{ 
			double remainingCount = (remainingTerm.length() > 0? _indexer.corpusTermFrequency(remainingTerm):_indexer.totalTermFrequency());
			double score = Math.log( (_indexer.corpusTermFrequency(term) / remainingCount + 1) + (1.0 / _indexer.totalTermFrequency()));
			return score;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	//================================
	//| Main API Query Handler Calls |
	//================================
	public QueryBoolGeo parseQuery(String givenQuery ){
		
		if(true)
			return langModel(givenQuery);
		
		List<String> listOfCandidateLocation = new ArrayList<>();
		
		QueryBoolGeo toReturn = new QueryBoolGeo(givenQuery);
		System.out.println(givenQuery);
		String[] tokens = givenQuery.split("\\s+");
		int length = tokens.length;

		String currentStringTested="";
		String currentStringTested2="";
		int freq1=0;
		int freq2=0;
		int[] spaces = new int[length-1];

		int currentLengthOfCnadidate = 2;
		for(int i=0; i< (length - currentLengthOfCnadidate ); i++){
			currentStringTested = tokens[i] + " " + tokens[i+1];
			currentStringTested2 = tokens[i+1] +" "+ tokens[i+2];
			freq1 = _indexer.corpusTermFrequency(currentStringTested);
			freq2 = _indexer.corpusTermFrequency(currentStringTested2);
			System.out.println( currentStringTested+ " "+currentStringTested2 +  " " + freq1 + " " + freq2);
			if(freq1 < freq2){

				spaces[i] += 1;
			}
			else if(freq1 > freq2) {

				spaces[i + 1] += 1;
			}
			else{
				spaces[i] += 1;
				spaces[i+1] += 1;
			}
		}

		currentLengthOfCnadidate = 3;
		System.out.println("size: "+ listOfCandidateLocation.size());

		for(int i=0; i< (length - currentLengthOfCnadidate); i++){
			currentStringTested = tokens[i] +" "+ tokens[i+1] +" "+ tokens[i+2];
			currentStringTested2 = tokens[i+1] + " " + tokens[i+2] + " " +tokens[i+3];
			freq1 = _indexer.corpusTermFrequency(currentStringTested);
			freq2 = _indexer.corpusTermFrequency(currentStringTested2);
			System.out.println( currentStringTested+ " "+currentStringTested2 +  " " + freq1 + " " + freq2);
			if(freq1 < freq2){

				spaces[i] += 1;
			}
			else {

				spaces[i + 2] += 1;
			}
		}

		if(length == 1){
			listOfCandidateLocation.add(tokens[0]);
		}
		else if(length == 2){
			int fre1 = _indexer.corpusTermFrequency(tokens[0]);
			int fre2 = _indexer.corpusTermFrequency(tokens[1]);
			int fre3 = _indexer.corpusTermFrequency(tokens[0] + tokens[1]);
			if(fre3>freq2 && fre3>freq2){
				listOfCandidateLocation.add(tokens[0] +" " + tokens[1]);
			}
			else{
				listOfCandidateLocation.add(tokens[0]);
				listOfCandidateLocation.add(tokens[1]);
			}
		}

		else if(length == 3){
			int freqWhole = _indexer.corpusTermFrequency(tokens[0] + " " + tokens[1] + " "+ tokens[2]);
			System.out.println(tokens[0] + " " + tokens[1] + " "+ tokens[2] + " " + freqWhole);
			int fre3= _indexer.corpusTermFrequency(tokens[0] + " "+ tokens[1]);
			int fre4= _indexer.corpusTermFrequency(tokens[1] + " "+ tokens[2]);
			if(freqWhole> fre3 && freqWhole>fre4){
				listOfCandidateLocation.add(tokens[0] + " " + tokens[1] + " "+ tokens[2]);
			}
			else if(fre3 > fre4){
				listOfCandidateLocation.add(tokens[0] + " "+ tokens[1]);
				listOfCandidateLocation.add(tokens[2]);
			}
			else if (fre3 < fre4){
				listOfCandidateLocation.add(tokens[0]);
				listOfCandidateLocation.add(tokens[1] + " " +tokens[2]);
			}
			else if(fre3 == fre4) {
				listOfCandidateLocation.add(tokens[0]);
				listOfCandidateLocation.add(tokens[1]);
				listOfCandidateLocation.add(tokens[2]);
			}

		}

		else {
			int flag=0;
			String pendingToken = tokens[0];
			for (int i = 0; i < (length - 1); i++) {
				if (spaces[i] == 0) {
					pendingToken += " " + tokens[i + 1];
					if(i == length-2 ){
						flag=1;
					}
				} else {
					listOfCandidateLocation.add(pendingToken);
					System.out.println("sizeD: "+ listOfCandidateLocation.size());

					pendingToken = tokens[i + 1];

					//flag=1;
				}
			}
			//if(flag==1){
			//	pendingToken = pendingToken + " " + tokens[length-1];
			//}
			//else{
			//	pendingToken=tokens[length - 1];
			//}

			listOfCandidateLocation.add(pendingToken);
			System.out.println("sizeC: "+ listOfCandidateLocation.size());
		}

		System.out.println("size: "+ listOfCandidateLocation.size());
		for(int i=0; i<listOfCandidateLocation.size(); i++){
			System.out.println("here...: "+listOfCandidateLocation.get(i));
		}

		QueryBoolGeo test = forEachSegments(toReturn,listOfCandidateLocation);
		System.out.println("Tokens " + toReturn._tokens.toString());
		return test;

	}

	private QueryBoolGeo forEachSegments(QueryBoolGeo toReturn, List<String> listOfCandidateLocation){
		List<GeoEntity> location_terms = new ArrayList<>();
		List<String> location_terms_string = new ArrayList<>();
		Vector<String> non_location_terms = new Vector<>();
		//System.out.println("entered forEachSegments");
		for(String s: listOfCandidateLocation){
			System.out.println("in for");
			//List<GeoEntity> localList = _gkb.getCandidates(s);
			if(_gkb.getCandidates(s).isEmpty()){
				System.out.println("in if");
				non_location_terms.add(s);
				System.out.println("tokenA: "+s);

			}
			else{
				System.out.println("in for");
				location_terms_string.add(s);
				System.out.println("tokenB: "+s);
				//for(GeoEntity g: localList) {
				//    location_terms.add(g);
				//}
			}
		}
		System.out.println("size of list: "+ listOfCandidateLocation.size());
		System.out.println("size of location list: "+location_terms_string);
		System.out.println("size of non-location list: "+non_location_terms);
		String dummy="";

		int size1=location_terms_string.size();
		int size2=non_location_terms.size();
		int index = 0;
		for(int i=0; i< size1; i++){
			for(int j=0; j<size2; j++){
				dummy = location_terms_string.get(i) +" "+ non_location_terms.get(index);
				System.out.println("dummy: "+dummy);
				List<GeoEntity> localList = _gkb.getCandidates(dummy);
				if(!localList.isEmpty()){
					location_terms_string.set(i, dummy);
					System.out.println(dummy);
					for(GeoEntity g: localList){
						System.out.println(g.getName());
						location_terms.add(g);
					}
					non_location_terms.remove(non_location_terms.get(index));
				}
				else{
					localList = _gkb.getCandidates(location_terms_string.get(i));
					for(GeoEntity g: localList){
						location_terms.add(g);
					}
					System.out.println("in else...");
					index += 1;
					System.out.println(index);
				}
				dummy="";
			}

		}

		int index2=0;
		int size3 = non_location_terms.size();
		int[] indexesToRemove = new int[size3];
		for(int i=0; i<size3; i++){
			for(int j=0; j<size3; j++){
				dummy = non_location_terms.get(i) +" "+ non_location_terms.get(j);
				//System.out.println("dummy: "+dummy);
				List<GeoEntity> localList = _gkb.getCandidates(dummy);
				if(!localList.isEmpty()){
					location_terms_string.add(dummy);
					for(GeoEntity g: localList) {
						location_terms.add(g);
					}
					indexesToRemove[index2] = i;
					index2 += 1;
					indexesToRemove[index2] = j;
					index2 += 1;
				}
			}
		}

		int index3=0;
		for(int i=0; i< index2; i++){
			int id = indexesToRemove[i] - index3;
			non_location_terms.remove(non_location_terms.get(id));
			index3 += 1;

		}

		//toReturn.populateInputStrings(non_location_terms);
		System.out.println("locations");
		System.out.println(location_terms_string);
		System.out.println("non-locations");
		System.out.println(non_location_terms.toString());

		toReturn._tokens = new Vector<>(non_location_terms);
		toReturn.populateGeoEntities(location_terms);

		location_terms_string.clear();
		non_location_terms.clear();
		listOfCandidateLocation.clear();
		return toReturn;
		
		//TODO: Fix
	}

}