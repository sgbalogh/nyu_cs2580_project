package edu.nyu.cs.cs2580;
import java.util.*;
import java.io.*;
/**
 * Created by stephen on 12/3/16.
 */
public class LocationParser {

	private SpatialEntityKnowledgeBase _gkb;
	private Indexer _indexer;
	private Map<String,Integer> listOfCandidateLocation = new HashMap<>();
	private QueryBoolGeo toReturn;
	public LocationParser(Indexer indexer, SpatialEntityKnowledgeBase gkb) {
		_indexer = indexer;
		_gkb = gkb;

	}


	public QueryBoolGeo parseQuery(String givenQuery){
		listOfCandidateLocation.clear();
		toReturn = new QueryBoolGeo(givenQuery);
		System.out.println(givenQuery);
		List<String> tokens = removeStopWords(givenQuery);
		toReturn._tokens = new Vector<String>(tokens);
		for(int i=0; i<tokens.size(); i++) {
			System.out.println("tokens: " + tokens.get(i));
		}


		//String[] tokens = givenQuery.split("\\s+");
		int length = tokens.size();
		int[] id = new int[length];
		for(int i=0; i<length; i++){
			id[i]=i;
		}

		System.out.println("total term frequency: "+_indexer.totalTermFrequency());


		String currentStringTested="";
		String currentStringTested2="";
		int freq1=0;
		int freq2=0;
		int[] spaces = new int[length-1];


		int currentLengthOfCnadidate = 2;
		for(int i=0; i< (length - currentLengthOfCnadidate ); i++){
			currentStringTested = tokens.get(i) + " " + tokens.get(i+1);
			currentStringTested2 = tokens.get(i+1) +" "+ tokens.get(i+2);
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



		if(length == 1){
			listOfCandidateLocation.put(tokens.get(0),0);
		}
		else if(length == 2){
			int fre1 = _indexer.corpusTermFrequency(tokens.get(0));
			int fre2 = _indexer.corpusTermFrequency(tokens.get(1));
			int fre3 = _indexer.corpusTermFrequency(tokens.get(0) + tokens.get(1));
			if(fre3>0 && (double)fre3/(_indexer.totalTermFrequency()) >= 0.00000029){
				listOfCandidateLocation.put(tokens.get(0) +" " + tokens.get(1),1);
			}
			else{
				listOfCandidateLocation.put(tokens.get(0),0);
				listOfCandidateLocation.put(tokens.get(1),1);
			}
		}

		else if(length == 3){
			int freqWhole = _indexer.corpusTermFrequency(tokens.get(0) + " " + tokens.get(1) + " "+ tokens.get(2));
			System.out.println(tokens.get(0) + " " + tokens.get(1) + " "+ tokens.get(2) + " " + freqWhole);
			int fre3= _indexer.corpusTermFrequency(tokens.get(0) + " "+ tokens.get(1));
			int fre4= _indexer.corpusTermFrequency(tokens.get(1) + " "+ tokens.get(2));
			System.out.println("here: "+   (double)freqWhole/(_indexer.totalTermFrequency() ));
			if(freqWhole>0 && ((double)freqWhole/(_indexer.totalTermFrequency()) > 0.00000029)){
				System.out.println("A..");
				listOfCandidateLocation.put(tokens.get(0) + " " + tokens.get(1) + " "+ tokens.get(2),2);
			}
			else if(fre3 > fre4){
				System.out.println("B..");
				listOfCandidateLocation.put(tokens.get(0) + " "+ tokens.get(1),1);
				listOfCandidateLocation.put(tokens.get(2),2);
			}
			else if (fre3 < fre4){
				System.out.println("C..");
				listOfCandidateLocation.put(tokens.get(0),0);
				listOfCandidateLocation.put(tokens.get(1) + " " +tokens.get(2),2);
			}
			else if(fre3 == fre4) {
				System.out.println("D..");
				listOfCandidateLocation.put(tokens.get(0),0);
				listOfCandidateLocation.put(tokens.get(1),1);
				listOfCandidateLocation.put(tokens.get(2),2);
			}

		}

		else {

			int fr = _indexer.corpusTermFrequency(givenQuery);
			System.out.println("fr: "+fr);
			System.out.println("here again : "+   (double)fr/(_indexer.totalTermFrequency() ));
			if (fr > 0 && ((double)fr / (_indexer.totalTermFrequency()) >= 0.00000029)) {
				listOfCandidateLocation.put(givenQuery, length - 1);
			} else {


				int flag = 0;
				String pendingToken = tokens.get(0);
				int pendingLastId = 0;
				for (int i = 0; i < (length - 1); i++) {
					if (spaces[i] == 0) {
						pendingToken += " " + tokens.get(i + 1);
						pendingLastId = id[i + 1];
						if (i == length - 2) {
							flag = 1;
						}
					} else {
						listOfCandidateLocation.put(pendingToken, pendingLastId);
						System.out.println("sizeD: " + listOfCandidateLocation.size());

						pendingToken = tokens.get(i + 1);
						pendingLastId = id[i + 1];

						//flag=1;
					}
				}
				//if(flag==1){
				//	pendingToken = pendingToken + " " + tokens[length-1];
				//}
				//else{
				//	pendingToken=tokens[length - 1];
				//}

				listOfCandidateLocation.put(pendingToken, pendingLastId);
				System.out.println("sizeC: " + listOfCandidateLocation.size());
			}
		}


		System.out.println("size: "+ listOfCandidateLocation.size());
		for(Map.Entry<String,Integer> entry: listOfCandidateLocation.entrySet()){
			System.out.println("string: "+ entry.getKey()+" pendingId: "+entry.getValue());
		}

		return forEachSegments();

	}

	public QueryBoolGeo forEachSegments(){
		List<GeoEntity> location_terms = new ArrayList<>();
		List<String> location_terms_string = new ArrayList<>();
		List<String> non_location_terms = new ArrayList<>();
		//System.out.println("entered forEachSegments");
		for(Map.Entry<String,Integer> entry: listOfCandidateLocation.entrySet()){
			System.out.println("in for");
			//List<GeoEntity> localList = _gkb.getCandidates(s);
			if(_gkb.getCandidates(entry.getKey()).isEmpty()){
				System.out.println("in if");
				non_location_terms.add(entry.getKey());
				System.out.println("tokenA: "+entry.getKey());

			}
			else{
				System.out.println("in for");
				location_terms_string.add(entry.getKey());
				System.out.println("tokenB: "+entry.getKey());
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


		if(size2==0){
			for(int i=0; i<size1; i++){
				List<GeoEntity> localList = _gkb.getCandidates(location_terms_string.get(i));
				for (GeoEntity g : localList) {
					location_terms.add(g);

				}
			}
		}

		else {

			for (int i = 0; i < size1; i++) {
				for (int j = 0; j < size2; j++) {


					dummy = location_terms_string.get(i) + " " + non_location_terms.get(index);
					System.out.println("dummy: " + dummy);
					List<GeoEntity> localList = _gkb.getCandidates(dummy);
					if (!localList.isEmpty()) {
						if (listOfCandidateLocation.get(location_terms_string.get(i)) < listOfCandidateLocation.get(non_location_terms.get(index))) {
							System.out.println("hereA");
							location_terms_string.set(i, dummy);
							for (GeoEntity g : localList) {
								location_terms.add(g);

							}
							System.out.println("hereB");
							non_location_terms.remove(non_location_terms.get(index));
							size2=size2-1;
							j=j-1;
						}
					} else {
						System.out.println("hereC");
						localList = _gkb.getCandidates(location_terms_string.get(i));

						for (GeoEntity g : localList) {
							location_terms.add(g);

						}
						System.out.println("in else...");
						index += 1;
						System.out.println(index);
						System.out.println("hereD");
					}
					dummy = "";


				}
				index = 0;

			}
		}
		System.out.println("hereE");

		int index2=0;
		int size3 = non_location_terms.size();
		int[] indexesToRemove = new int[size3];
		for(int i=0; i<size3; i++){
			for(int j=0; j<size3; j++){

				dummy = non_location_terms.get(i) + " " + non_location_terms.get(j);
				//System.out.println("dummy: "+dummy);
				List<GeoEntity> localList = _gkb.getCandidates(dummy);
				if (!localList.isEmpty()) {
					if (listOfCandidateLocation.get(non_location_terms.get(i)) < listOfCandidateLocation.get(non_location_terms.get(j))) {
						location_terms_string.add(dummy);
						for (GeoEntity g : localList) {
							location_terms.add(g);
						}
						indexesToRemove[index2] = i;
						index2 += 1;
						indexesToRemove[index2] = j;
						index2 += 1;
					}
				}
			}
		}

		System.out.println("hereF");

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

		toReturn.setSupportingTokens(non_location_terms);
		toReturn.populateGeoEntities(location_terms);

		//location_terms_string.clear();
		//non_location_terms.clear();
		listOfCandidateLocation.clear();
		//Vector<String> temp = new Vector<>();
		//temp.add("new york");
		//toReturn._tokens = temp;

		System.out.println("size:" +location_terms.size());
		return toReturn;
	}

	public static List<String> removeStopWords(String s){
		String[] given=s.split("\\s+");
		List<String> toReturn = new ArrayList<>();
		int l = given.length;
		int[] indexToRemove = new int[l];

		try {
			BufferedReader br = new BufferedReader(new FileReader("english.stop.txt"));
			System.out.println("*****************************************************************");
			String temp = br.readLine();
			while(temp!=null){
				//System.out.println(temp);
				for(int i=0; i<l; i++){
					if(given[i].equals(temp) && indexToRemove[i]==0){
						//toReturn.add(given[i]);
						indexToRemove[i] = 1;

					}
				}
				temp = br.readLine();

			}

			for(int i=0; i<l; i++){
				if(indexToRemove[i] == 0){
					toReturn.add(given[i]);
					System.out.println(given[i]);
				}
			}

			System.out.println("*****************************************************************");




		}
		catch(IOException e){
			System.out.println("IO exception found!");
		}


		return toReturn;
	}


	//===========================Language Model========================================

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
					System.out.println("Location: " + term);
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
			if(remainingCount <= 50)
				remainingCount = _indexer.totalTermFrequency();

			double score = Math.log( (_indexer.corpusTermFrequency(term) / remainingCount + 1) + (1.0 / _indexer.totalTermFrequency()));
			return score;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return 0;
	}


	//===============================================
	// INCLUDE METHOD FOR STATISTICAL SEGMENTATION
	//===============================================



}