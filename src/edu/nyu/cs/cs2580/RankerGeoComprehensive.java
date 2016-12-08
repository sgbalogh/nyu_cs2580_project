package edu.nyu.cs.cs2580;

import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Vector;

/**
 * Created by stephen on 12/3/16.
 */
//TODO: implement this
    //Algorithm: QL and PageRank data (modeled on previous Comprehensive)
    // THIS RANKER IS RESPONSIBLE FOR CHOOSING THE CANDIDATE TO EXPAND ON
    // ... AND WHETHER OR NOT TO EXPAND GIVEN CONDITIONS
        // 1) not enough results
        // 2) "reasonably" think its a place
            // During scoring candidates, vote by majority frequency, winner takes all
        // Call GeoEntity.getStateName() to resolve

	//Goal 1: Improve ranking algorithm: Compare to Lucene: https://lucene.apache.org/core/3_6_0/api/core/org/apache/lucene/search/Similarity.html
    // SUPPORT BOOLEAN
	// This only resolves competing city names ...
	// What I want is:
	// Use Case: Look up jersey city zoo => west orange zoo
	// Run expanded query to convert to boolean query
	// Then, check if it's top X results outperform present results by a margin

	//Goal 2: Suggestion: Suggest to me locations if location is relevant
	//Penn Station => Newark or New York? 


    // Needs to be aware of whether or not expansion has already been performed by checking
        // to see if there exists a true value in QBG (read QBG._expanded)

public class RankerGeoComprehensive extends Ranker {
	
	public class GeoCandidateTuple {
		public GeoEntity cand;
		public String expandTerm;
		public double score = 0.0;
		
		public GeoCandidateTuple(GeoEntity cand, String expandTerm) {
			this.cand = cand;
		}
		
		public void updateScore(double val) {
			score += val;
		}
	}
	
	public static double _alpha;
	public static double _threshold;
	public static int _max_terms;

	public RankerGeoComprehensive(SearchEngine.Options options,
	                           QueryHandler.CgiArguments arguments, Indexer indexer) {
	    super(options, arguments, indexer);
	    _alpha = 0.3; //Default Alpha is 0.3 for Smoothing
	    
	    //New Tunable Static Variables
	    _threshold = 0.05; //If the average percentage of documents contain more than 5% of candidate, it is likely relevant
	    _max_terms = 5; //Terms to expand on
	    
	    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
	}
	
	@Override
	public Vector<ScoredDocument> runQuery(Query query, int numResults) {
	    if (! (query instanceof QueryBoolGeo))
	        throw new IllegalStateException("Pass in QBG");
	    
	    if(query._tokens.size() == 0)
	    	return new Vector<ScoredDocument>();
	    
	    QueryBoolGeo actualQuery = (QueryBoolGeo) query;
	    
	    try {
	      Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();
	
	      DocumentIndexed doc = null;
	      int docid = -1;
	      
	      //Resolving Ambiguous queries
	      HashMap<Integer, GeoCandidateTuple> candScores = new HashMap<>();
	      for(GeoEntity candidate : actualQuery.get_candidate_geo_entities()) {
	    	  candScores.put(candidate.getId(), new GeoCandidateTuple(candidate, candidate.getStateName()));
	      }
	      
	      Integer numDocs = 0;
	
	      //Get Document Indexed
	      while ((doc = (DocumentIndexed) _indexer.nextDoc((QueryBoolGeo) query, docid)) != null) {
	        rankQueue.add(new ScoredDocument(doc, scoreDocument( actualQuery , doc, candScores)));
	
	        //Make sure top X documents are in memory
	        if (rankQueue.size() > numResults) {
	          rankQueue.poll();
	        }
	        docid = doc._docid;
	        numDocs++;
	      }
	
	      //Get Results and Store them
	      Vector<ScoredDocument> results = new Vector<ScoredDocument>();
	      ScoredDocument scoredDoc = null;
	      while ((scoredDoc = rankQueue.poll()) != null) {
	        results.add(scoredDoc);
	      }
	      
	      //Only on first expansion
	      if(!actualQuery._expanded) {

		      GeoEntity bestExpansionTerm = null;
		      double bestScore = -1;
		      
		      //Find best candidate
		      for(Integer id: candScores.keySet()) {
		    	  if(candScores.get(id).score > bestScore) {
		    		  bestExpansionTerm = candScores.get(id).cand;
		    		  bestScore = candScores.get(id).score;
		    	  }
		      }
		      
		      //Found a reasonable location to expand upon or not enough results
		      if(bestExpansionTerm != null && 
		    		  (results.size() < numResults || bestScore > _threshold * numDocs)) {
		    	  expand(actualQuery, bestExpansionTerm , _max_terms);
		    	  
			      actualQuery._should_present = true; 
			      actualQuery._expanded = true; 
		      }
	      }
	      
	      return results;
	    } catch(Exception e) {
	      e.printStackTrace();
	    }
	
	    return null;
	}
	
	//Get Query Likelihood Score
	public Double scoreDocument(QueryBoolGeo query, DocumentIndexed doc, HashMap<Integer, GeoCandidateTuple> candScores) {
	  Double score = 0.0;
	
	  try {
	    //Normal Tokens
	    for(String term : query._tokens) {
	      score += Math.log(
	              //Probability in Document
	              ((1 - _alpha) * _indexer.documentTermFrequency(term, doc._docid) / doc._numWords )
	                      +
	                      //Smoothing
	                      (_alpha * _indexer.corpusTermFrequency(term) / _indexer._totalTermFrequency )
	      ); 
	    }
	  } catch(Exception e) {
	    e.printStackTrace();
	  }
	  
	  //Score is modified by pagerank but is still considered if not referenced
	  score = score * (doc.getPageRank() + 1);
	  
	  //TODO: Need to support extracting locations from documents
	  //Suggestion
	  //for(GeoEntity suggested : doc.suggested) { //Add Suggested Values
		//  candScores.put(suggested.getId(), new GeoCandidateTuple(suggested, suggested.getName()));
	  //}
	  
	  //Go through all the candidates and determine if there is any improvement
	  //Resolving ambiguous locations
	  for(GeoCandidateTuple candidate : candScores.values()) {
		  String stateName = candidate.cand.getStateName();
		  double candScore = _indexer.documentTermFrequency(stateName, doc._docid) / doc._numWords;
		  
		  //Update Score
		  candidate.updateScore(candScore * (doc.getPageRank() + 1));
	  }
	
	  return score;
	}
	
	private void expand(QueryBoolGeo query, GeoEntity toExpand, int max_expand_locations) {
	    query.set_expanded_geo_entities(toExpand.getNearbyCities(max_expand_locations));
	}
	
	private void suggest(QueryBoolGeo query, GeoEntity toSuggest, int max_expand_locations) {
	    query.get_expanded_geo_entities().add(toSuggest);
	}
}
