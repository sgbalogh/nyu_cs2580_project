package edu.nyu.cs.cs2580;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
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
	//Scored Vector Tuple
	public class ScoredSumTuple {
		public double total_score = 0.0;
		public Vector<ScoredDocument> scored;
		
		public ScoredSumTuple(Vector<ScoredDocument> scored, double sum) {
			this.scored = scored;
			this.total_score = sum;
		}
	}
	
	//Peeking Iterator for Merging
	public class PeekingIterator {
		public ScoredDocument doc;
		public Iterator<ScoredDocument> iter;
		
		public PeekingIterator(Vector<ScoredDocument> scored) {
			iter = scored.iterator();
			doc = iter.next();
		}
		
		public boolean pop() {
			if(iter.hasNext())
				return false;
			
			doc = iter.next();
			return true;
		}
	}
	
	public double _alpha = 0.5; //Default Alpha is 0.5 for Smoothing
	public double _orig_threshold = 100.0; //Determines if original score is strong enough not to do expansion
	public double _threshold = 100.0; //Significant expansion term score threshold
	
	public int _max_expansion = 5; //Max number of Expansion Terms

	//Cache: ~5 expanded terms in cache
	//TODO: Manage Cache memory
	public HashMap<String, Vector<ScoredDocument>> _cache;
	
    public RankerGeoComprehensive(SearchEngine.Options options,
                               QueryHandler.CgiArguments arguments, Indexer indexer) {
        super(options, arguments, indexer);
        _cache = new HashMap<String, Vector<ScoredDocument>>();
        System.out.println("Using Ranker: " + this.getClass().getSimpleName());
    }
    
    //TODO: Parallelize/Multi-thread
    @Override
    public Vector<ScoredDocument> runQuery(Query init_query, int numResults) {
    	if(!(init_query instanceof QueryBoolGeo))
    		throw new IllegalStateException("Please Use Query Phrase");
	
    	if(init_query._tokens.size() == 0)
    		return new Vector<ScoredDocument>();
    	
    	QueryBoolGeo query = (QueryBoolGeo) init_query;
    	
    	if(query.cache) { //Return cached value
    		return returnCachedList(query._query, numResults, query.best);
    	} 
    	
    	ScoredSumTuple origBenchmark = runQuery(query, numResults);
    	
    	StringBuilder logs = new StringBuilder("Orig Term :");
    	logs.append(query._query).append(" ").append(origBenchmark.total_score).append("\n");
    	
		//Determine if orig benchmark is good enough to not expand
    	if(origBenchmark.scored.size() < numResults || origBenchmark.total_score < _orig_threshold * numResults) {
	    	//Expand Word
	    	query.expand(_max_expansion);
	    	
	    	Iterator<String> expQueryIterator = ((QueryBoolGeo) init_query)._expanded_queries.iterator();
	    	
	    	while(expQueryIterator.hasNext()) {
	    		
	    		//Create new query
	    		QueryBoolGeo expandedQuery = new QueryBoolGeo(expQueryIterator.next());
	    		expandedQuery.populateInputStrings(Arrays.asList(expandedQuery._query.split("\\s")));
	    		
	    		ScoredSumTuple newResults = runQuery(expandedQuery , numResults);
	    		
	    		//Log Results:
	    		logs.append(expandedQuery._query).append(": ").append(newResults.total_score);
	    		
	    		//If expansion helps...
	    		if(newResults.total_score > (_threshold * numResults)
	    				&&
	    				newResults.total_score > origBenchmark.total_score) {
	    			
	    			((QueryBoolGeo) init_query)._should_present = true;
	    			
	    			//cache
	    			_cache.put(expandedQuery._query, newResults.scored);
	    			
	    			logs.append(": Qualified!\n");
	    		} else {
	    			//Remove expanded term if not qualified
	    			expQueryIterator.remove();
	    			
	    			logs.append(": Unqualified!\n");
	    		}
	    	}
	    	
	    	//Log Expansion queries
	    	if(query._should_present) { 
	    		logs.append("Expansion Terms: ").append(query._expanded_queries.toString()).append("\n");
	    	}
    	} else {
    		//No Expansion
    		logs.append("Good Enough for Expansion: Size: ").append(origBenchmark.scored.size())
    						.append(" Score: ").append(origBenchmark.total_score).append("\n");
    	}
    	
		//Flush Log
		try {
			BufferedWriter br = new BufferedWriter(new FileWriter("rankerDecision.txt"));
			br.write(logs.toString());
			br.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
    	
    	return origBenchmark.scored;
    }
    
    //Return Vector of Scored docs from cache
    public Vector<ScoredDocument> returnCachedList(String input, int numResults, boolean best) {
    	//Merge all cache results
    	if(best) { 
    		Vector<ScoredDocument> newResults = new Vector<ScoredDocument>();
    		
    		//Priority Queue of Iterators
    		PriorityQueue<PeekingIterator> mergeBuffer = new PriorityQueue<>( _cache.size(), 
    				new Comparator<PeekingIterator>() {
						@Override
						public int compare(PeekingIterator o1, PeekingIterator o2) {
							return Double.compare(o2.doc.getScore(),o1.doc.getScore());
						}
    		});
    		
    		//Initial Insert
    		for(Vector<ScoredDocument> docs :_cache.values()) {
    			if(docs.size() > 0)
    				mergeBuffer.add(new PeekingIterator(docs));
    		}
    		
    		while(newResults.size() >= numResults ) {
    			
    			PeekingIterator nextElem = mergeBuffer.poll();
    			newResults.add(nextElem.doc);
    			
    			if(!nextElem.pop())
    				break;
    			
    			mergeBuffer.add(nextElem);
    		}
    		
    		return newResults;
    	}
    	
    	return _cache.get(input);
    }
    
    //Score All Docs
    public ScoredSumTuple runQuery(QueryBoolGeo query, int numResults) {
    	try {
    		Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();
    		double sum = 0.0;
	
    		DocumentIndexed doc = null;
    		int docid = -1;
	
    		//Get Document Indexed
    		while ((doc = (DocumentIndexed) _indexer.nextDoc(query, docid)) != null) {
    			double score = scoreDocumentQL(query, doc);
    			rankQueue.add(new ScoredDocument(doc, score));
    			
    			sum += score;
	
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
    		
    		//Collections.sort(results, Collections.reverseOrder());
    		
    		return new ScoredSumTuple(results, sum);
		} catch(Exception e) {
			e.printStackTrace();
		}
	    return null;
    }
	
    //TODO: Modify to Lucene's formula: https://lucene.apache.org/core/3_6_0/api/core/org/apache/lucene/search/Similarity.html
    //Get Query Likelihood Score
    public Double scoreDocumentQL(QueryBoolGeo query, DocumentIndexed doc) {
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
	
    	return score * (1 + doc.getPageRank()); //multiply by pagerank + 1
    }
}
