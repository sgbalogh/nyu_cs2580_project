package edu.nyu.cs.cs2580;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    // SUPPORT BOOLEAN
    // Needs to be aware of whether or not expansion has already been performed by checking
        // to see if there exists a true value in QBG (read QBG._expanded)

public class RankerGeoComprehensive extends Ranker {
	public static double _alpha;
	public static double _threshold;
	public static int _max_terms;

	public RankerGeoComprehensive(SearchEngine.Options options,
	                           QueryHandler.CgiArguments arguments, Indexer indexer) {
	    super(options, arguments, indexer);
	    _alpha = 0.3; //Default Alpha is 0.3 for Smoothing
	    
	    _threshold = 0.01; 
	    _max_terms = 10;
	    
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
	      ArrayList<Double> candScores = new ArrayList<Double>(actualQuery.get_candidate_geo_entities().size() + 1);
	
	      //Get Document Indexed
	      while ((doc = (DocumentIndexed) _indexer.nextDoc((QueryBoolGeo) query, docid)) != null) {
	        rankQueue.add(new ScoredDocument(doc, scoreDocument( actualQuery , doc, candScores)));
	
	        //Make sure top X documents are in memory
	        if (rankQueue.size() > numResults) {
	          rankQueue.poll();
	        }
	        docid = doc._docid;
	      }
	
	      //Get Results and Store them
	      Vector<ScoredDocument> results = new Vector<ScoredDocument>();
	      ScoredDocument scoredDoc = null;
	      while ((scoredDoc = rankQueue.poll()) != null) {
	        results.add(scoredDoc);
	      }

	      //Find best candidate
	      int bestInd = -1;
	      double bestScore = -1;
	      for(int ind = 0; ind < candScores.size(); ind++) {
	    	  if(candScores.get(ind) > bestScore) {
	    		  bestInd = ind;
	    		  bestScore = candScores.get(ind);
	    	  }
	      }
	      
	      //Found a reasonable location to expand upon
	      if(bestInd < candScores.size() - 1) {
	    	  expand(actualQuery, bestInd, _max_terms);
		      actualQuery._should_present = true; 
		      actualQuery._expanded = true; 
	      }
	      
	      return results;
	    } catch(Exception e) {
	      e.printStackTrace();
	    }
	
	    return null;
	}
	
	//Get Query Likelihood Score
	public Double scoreDocument(QueryBoolGeo query, DocumentIndexed doc, List<Double> candScores) {
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
	  
	  //Go through all the candidates and determine if there is any improvement
	  //Resolving ambiguous locations
	  int index = 0;
	  for(GeoEntity candidate : query.get_candidate_geo_entities()) {
		  double candScore = _indexer.documentTermFrequency(candidate.getStateName(), doc._docid) / doc._numWords;
		  
		  //If no measurable improvement from any candidate
		  candScores.set(index, candScores.get(index) + candScore * (doc.getPageRank() + 1));
		  
		  index++;
	  }
	  
	  //Update no name
	  candScores.set(index, candScores.get(index) + _threshold);
	  
	  //TODO: Need to support extracting locations from documents
	  //Suggestion
	
	  return score;
	}
	
	private void expand(QueryBoolGeo query, int index_to_expand, int max_expand_locations) {
	    GeoEntity toExpand = query.get_candidate_geo_entity(index_to_expand);
	    query.set_expanded_geo_entities(toExpand.getNearbyCities(max_expand_locations));
	}

}
