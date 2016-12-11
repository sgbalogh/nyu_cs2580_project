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
		String[] tokens = givenQuery.split("\\s+");
		int length = tokens.length;
		int[] id = new int[length];
		for(int i=0; i<length; i++){
			id[i]=i;
		}




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
			listOfCandidateLocation.put(tokens[0],0);
		}
		else if(length == 2){
			int fre1 = _indexer.corpusTermFrequency(tokens[0]);
			int fre2 = _indexer.corpusTermFrequency(tokens[1]);
			int fre3 = _indexer.corpusTermFrequency(tokens[0] + tokens[1]);
			if(fre3>freq2 && fre3>freq2){
				listOfCandidateLocation.put(tokens[0] +" " + tokens[1],1);
			}
			else{
				listOfCandidateLocation.put(tokens[0],0);
				listOfCandidateLocation.put(tokens[1],1);
			}
		}

		else if(length == 3){
			int freqWhole = _indexer.corpusTermFrequency(tokens[0] + " " + tokens[1] + " "+ tokens[2]);
			System.out.println(tokens[0] + " " + tokens[1] + " "+ tokens[2] + " " + freqWhole);
			int fre3= _indexer.corpusTermFrequency(tokens[0] + " "+ tokens[1]);
			int fre4= _indexer.corpusTermFrequency(tokens[1] + " "+ tokens[2]);
			if(freqWhole> fre3 && freqWhole>fre4){
				listOfCandidateLocation.put(tokens[0] + " " + tokens[1] + " "+ tokens[2],2);
			}
			else if(fre3 > fre4){
				listOfCandidateLocation.put(tokens[0] + " "+ tokens[1],1);
				listOfCandidateLocation.put(tokens[2],2);
			}
			else if (fre3 < fre4){
				listOfCandidateLocation.put(tokens[0],0);
				listOfCandidateLocation.put(tokens[1] + " " +tokens[2],2);
			}
			else if(fre3 == fre4) {
				listOfCandidateLocation.put(tokens[0],0);
				listOfCandidateLocation.put(tokens[1],1);
				listOfCandidateLocation.put(tokens[2],2);
			}

		}

		else {
			int flag=0;
			String pendingToken = tokens[0];
			int pendingLastId=0;
			for (int i = 0; i < (length - 1); i++) {
				if (spaces[i] == 0) {
					pendingToken += " " + tokens[i + 1];
					pendingLastId = id[i+1];
					if(i == length-2 ){
						flag=1;
					}
				} else {
					listOfCandidateLocation.put(pendingToken,pendingLastId);
					System.out.println("sizeD: "+ listOfCandidateLocation.size());

					pendingToken = tokens[i + 1];
					pendingLastId = id[i+1];

					//flag=1;
				}
			}
			//if(flag==1){
			//	pendingToken = pendingToken + " " + tokens[length-1];
			//}
			//else{
			//	pendingToken=tokens[length - 1];
			//}

			listOfCandidateLocation.put(pendingToken,pendingLastId);
			System.out.println("sizeC: "+ listOfCandidateLocation.size());
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
		Vector<String> non_location_terms = new Vector<>();
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

		toReturn._tokens = non_location_terms;
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


	//===============================================
	// INCLUDE METHOD FOR STATISTICAL SEGMENTATION
	//===============================================



}
