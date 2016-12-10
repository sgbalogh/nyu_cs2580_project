package edu.nyu.cs.cs2580;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Vector;

/**
 * Created by stephen on 12/3/16.
 */
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
        public double queryNorm = 0.0;
        public Vector<ScoredDocument> scored;

        public ScoredSumTuple(Vector<ScoredDocument> scored, double sum, double queryNorm) {
            this.scored = scored;
            this.total_score = sum;
            this.queryNorm = queryNorm;
        }
    }

    //Peeking Iterator for Merging
    public class PeekingIterator {
        public ScoredDocument doc;
        public double queryNorm;
        public Iterator<ScoredDocument> iter;

        public PeekingIterator(Vector<ScoredDocument> scored, double queryNorm) {
            iter = scored.iterator();
            doc = iter.next();
            this.queryNorm = queryNorm;
        }

        public boolean pop() {
            if(iter.hasNext())
                return false;

            doc = iter.next();
            return true;
        }
    }

    //Testing
    public double _orig_threshold = 2.0; //Determines if original score is strong enough not to do expansion
    public double _expanded_threshold = 1.0; //Significant expansion term score threshold

    public int _max_expansion = 3; //Max number of Expansion Terms

    //Cache: ~5 expanded terms in cache
    //TODO: Manage Cache memory
    public HashMap<String, ScoredSumTuple> _cache;

    public RankerGeoComprehensive(SearchEngine.Options options,
                                  QueryHandler.CgiArguments arguments, Indexer indexer) {
        super(options, arguments, indexer);
        _cache = new HashMap<>();
        System.out.println("Using Ranker: " + this.getClass().getSimpleName());
    }

    //TODO: Parallelize/Multi-thread
    @Override
    public Vector<ScoredDocument> runQuery(Query init_query, int numResults) {
        if(!(init_query instanceof QueryBoolGeo))
            throw new IllegalStateException("Please Use Query Phrase");

        try {
            //=============================================
            // Initial Query Run
            //=============================================
	        QueryBoolGeo query = (QueryBoolGeo) init_query;
	        
	        HashMap<String, Integer> geoTermIndex = new HashMap<>(); //Central Park => 0, Hoboken => 1
	        //Don't really care about duplicates
	        
	        boolean supportingTerms = (query._tokens.size() > 0? true: false);
	        
	        if(query.get_candidate_geo_entities().size() > 0) {
	        	//Find indexes of changed values
	        	int index = query._tokens.size();
	        	for(GeoEntity ge : query.get_candidate_geo_entities()) {
	        		
	        		String cleanedName = ge.getName().toLowerCase().trim();
		        	query._tokens.add(cleanedName);
		        	geoTermIndex.put(cleanedName, index);
		        	index++;
	        	}
	        }
	        
	        if(init_query._tokens.size() == 0) //Flat out empty String
	            return new Vector<ScoredDocument>();
	
	        if(query.cache) { //Return cached value
	            return returnCachedList(query._query, numResults, query.best);
	        }
	        
	        System.out.println(query._tokens.toString());
	        
	        ScoredSumTuple origBenchmark = runQuery(query, numResults);
	        
	        System.out.println("Original Finished: " + origBenchmark.scored.size() + " Score: " + (origBenchmark.total_score / origBenchmark.queryNorm));
	        
	        StringBuilder logs = new StringBuilder("Orig Term :");
	        logs.append(query._query).append(" ").append(origBenchmark.total_score).append("\n");
   
		    //=================End of Original Run===================
	        
	        //=====================================Ambiguous mode====================================
	        if(query.get_candidate_geo_entities().size() > 0) {
	        	//Run uniquify
     	
	        } else if(query.get_candidate_geo_entities().size() == 1) {
	        //=================================Local Expansion Mode===================================
	        
		        //Determine if original benchmark is good enough to not expand
		        if(		supportingTerms //Only expand if supporting terms exist
		        		&&
		        		(origBenchmark.scored.size() < numResults 
		        				|| //Original results not good enough
		        				origBenchmark.total_score / origBenchmark.queryNorm < _orig_threshold * numResults)) {
		        	System.out.println("Try to Expand");
		        	
		        	//Expand Word
		            query.expand(_max_expansion);
		
		            Iterator<GeoEntity> expQueryIterator = ((QueryBoolGeo) init_query).get_expanded_geo_entities().iterator();
		        	
		            //Iterator through all nearby cities of each 
		            while(expQueryIterator.hasNext()) {
		
		                //Create new query:
		                String cityName = expQueryIterator.next().getName();
		
		                QueryBoolGeo expandedQuery = new QueryBoolGeo(query._tokens.toString().replaceAll( "[^A-Za-z0-9//s]", "") + " " + cityName);
		                Vector<String> _new_terms = new Vector<>(query._tokens); //Add non location terms
		                _new_terms.add(cityName);
		
		                expandedQuery._tokens = _new_terms;
		                
		                System.out.println("Expanded Query: " + expandedQuery._tokens.toString());

		                ScoredSumTuple newResults = runQuery(expandedQuery , numResults);
		
		                //If expansion helps...
		                double normalizedScore = newResults.total_score / newResults.queryNorm;
		                if(newResults.total_score == 0)
		                	normalizedScore = 0.0;
		                
		                //Log Results:
		                logs.append(expandedQuery._query).append(": ").append(normalizedScore);
		
		                if( normalizedScore > _expanded_threshold
		                        &&
		                        normalizedScore > origBenchmark.total_score / origBenchmark.queryNorm) {
		
		                    ((QueryBoolGeo) init_query)._presentation_mode = GEO_MODE.EXPANSION;
		
		                    //cache
		                    _cache.put(expandedQuery._query, newResults);
		
		                    logs.append(": Qualified!\n");
		                    System.out.println(expandedQuery._query + ": qualified with " + normalizedScore);
		                } else {
		                    //Remove expanded term if not qualified
		                    expQueryIterator.remove();
		
		                    logs.append(": Unqualified!\n");
		                    System.out.println(expandedQuery._query + ": unqualified with " + normalizedScore);
		                }
		            }
		
		            //Log Expansion queries
		            if(query._presentation_mode.equals(GEO_MODE.EXPANSION)) {
		                logs.append("Expansion Terms: ");
		                for(GeoEntity gEnt : query.get_expanded_geo_entities()) {
		                    logs.append(gEnt.getName()).append(" ");
		                }
		                logs.append("\n");
		            }
		            
		        } else {
		            //No Expansion
		            logs.append("Good Enough for Expansion: Size: ").append(origBenchmark.scored.size())
		                    .append(" Score: ").append(origBenchmark.total_score).append("\n");
		        }
	        } else {
	            //No Expansion Terms
	            logs.append("Good Enough for Expansion: Size: ").append(origBenchmark.scored.size())
	                    .append(" Score: ").append(origBenchmark.total_score).append("\n");
	        }
	
	        //Flush Log
	        try {
	            BufferedWriter br = new BufferedWriter(new FileWriter("rankerDecision.txt", true));
	            br.write(logs.toString());
	            br.close();
	        } catch(Exception e) {
	            e.printStackTrace();
	        }
	        
	        return origBenchmark.scored;
        } catch (Exception e) {
        	e.printStackTrace();
        }

        return null;
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
                            return Double.compare( o2.doc.getScore() / o2.queryNorm ,
                                    o1.doc.getScore() / o1.queryNorm );
                        }
                    });

            //Initial Insert
            for(ScoredSumTuple docs :_cache.values()) {
                if(docs.scored.size() > 0)
                    mergeBuffer.add(new PeekingIterator(docs.scored, docs.queryNorm));
            }

            while(newResults.size() >= numResults ) {

                PeekingIterator nextElem = mergeBuffer.poll();
                newResults.add(nextElem.doc);

                if(nextElem.pop()) {
                    mergeBuffer.add(nextElem);
                }

                //Out of Expanded Documents
                if(mergeBuffer.size() <= 0)
                    break;
            }

            return newResults;
        }

        return _cache.get(input).scored;
    }

    //Score All Docs
    public ScoredSumTuple runQuery(QueryBoolGeo query, int numResults) {
        try {
            Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();
            double sum = 0.0;
            double normSum = 0.0;

            DocumentIndexed doc = null;
            int docid = -1;

            //Get Document Indexed
            while ((doc = (DocumentIndexed) _indexer.nextDoc(query, docid)) != null) {
                double score = scoreDocumentQL(query, doc);
                rankQueue.add(new ScoredDocument(doc, score));

                //Make sure top X documents are in memory
                if (rankQueue.size() > numResults) {
                    rankQueue.poll();
                }
                docid = doc._docid;
                
                sum += score;
                normSum += Math.pow(score, 2);
            }

            //Get Results and Store them
            Vector<ScoredDocument> results = new Vector<ScoredDocument>();
            ScoredDocument scoredDoc = null;
            while ((scoredDoc = rankQueue.poll()) != null) {
                results.add(scoredDoc);
            }

            Collections.sort(results, Collections.reverseOrder());

            return new ScoredSumTuple(results, sum, Math.sqrt(normSum));
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //Modified to Lucene's formula:
    //    - https://lucene.apache.org/core/3_6_0/api/core/org/apache/lucene/search/Similarity.html
    //Get Query Likelihood Score
    //Implemented according to: http://www.lucenetutorial.com/advanced-topics/scoring.html

    /* Reasoning
     * Documents containing *all* the search terms are good
     * Matches on rare words are better than for common words
     * Long documents are not as good as short ones
     * Documents which mention the search terms many times are good
     */

    public Double scoreDocumentQL(QueryBoolGeo query, DocumentIndexed doc) {
        Double score = 0.0;

        try {
            int foundTerms = 0;
            //Normal Tokens
            for(String term : query._tokens) {
                int docTermFreq = _indexer.documentTermFrequency(term, doc._docid);
            	System.out.println(doc.getTitle() + " " + term + " " + docTermFreq);
                score += docTermFreq * //Term Frequency
                        Math.pow(
                                (Math.log(_indexer._numDocs / (_indexer.corpusDocFrequencyByTerm(term) + 1.0)) / Math.log(2)) + 1
                                ,2) * //IDF
                        (1 / Math.sqrt(query._tokens.size())); //lengthNorm 
            	
            	 //Better Than QL: guatemala
            	 /* score += Math.log(
                        //Probability in Document
                        ((0.8) * _indexer.documentTermFrequency(term, doc._docid) / doc._numWords )
                                +
                                //Smoothing
                                (0.2 * _indexer.corpusTermFrequency(term) / _indexer._totalTermFrequency )
                	);*/

                //Overlap
                foundTerms += (docTermFreq > 0? 1:0);
            }

            score = score * (1 + doc.getPageRank()) * //multiply by pagerank + 1
            		(foundTerms / query._tokens.size()); //coord

        } catch(Exception e) {
            e.printStackTrace();
        }

        return score;
    }
}