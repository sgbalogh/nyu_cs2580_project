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
//Algorithm: TF-IDF - Lucene data (modeled on previous Comprehensive)
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

    public int _max_expansion = 50; //Max number of Expansion Terms

    //Cache: ~5 expanded terms in cache
    //TODO: Manage Cache memory
    //public HashMap<String, ScoredSumTuple> _cache;

    public RankerGeoComprehensive(SearchEngine.Options options,
                                  QueryHandler.CgiArguments arguments, Indexer indexer) {
        super(options, arguments, indexer);
        //_cache = new HashMap<>();
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

            if(init_query._tokens.size() == 0) //Flat out empty String
                return new Vector<ScoredDocument>();

            if(query.best) { //Return cached value
                return returnBestList(query, numResults);
            }

            ScoredSumTuple origBenchmark = runQuery(query, numResults);

            System.out.println("Original Finished: " + origBenchmark.scored.size() + " Score: " + (origBenchmark.total_score / origBenchmark.queryNorm));

            StringBuilder logs = new StringBuilder("Orig Term :");
            logs.append(query._query).append(" ").append(origBenchmark.total_score).append("\n");

            //=================End of Original Run===================

            //=============================================
            // Expansion Modes
            //=============================================

            query.resolve(); // This helps us reduce the amount of candidate locations by
            // looking to see if any of them are related

            //=====================================Ambiguous mode====================================
            if(query.get_candidate_geo_entities().size() > 1) {
                //Run uniqify
                System.out.println("Uniquify Stage");

                //Uniqify Each Candidate
                query.uniqify(query.get_candidate_geo_entities());

                HashMap<String, Double> scores = new HashMap<>();

                //Run queries for each and get the best ones
                Iterator<GeoEntity> geIter = query.get_candidate_geo_entities().iterator();

                while(geIter.hasNext()) {

                    GeoEntity ge = geIter.next();

                    //Insert into Tokens new
                    String[] extraLocationInfo = ge.getUniqueName().split(",");
                    for(int ind = 1; ind < extraLocationInfo.length; ind++) {
                        query._tokens.add( extraLocationInfo[ind] );
                    }

                    ScoredSumTuple newResults = runQuery(query , numResults);

                    //Should I again have a threshold?
                    if(newResults.total_score / newResults.queryNorm >= 0) {
                        scores.put(ge.getUniqueName() , newResults.total_score / newResults.queryNorm );
                    } else { //Remove if not qualified
                        geIter.remove();
                    }

                    //Remove last Info
                    for(int ind = 0; ind < extraLocationInfo.length - 1; ind++) {
                        query._tokens.remove( query._tokens.size() - 1 - ind );
                    }
                }

                //Sort candidates by their scores & assign new candidates
                query.get_candidate_geo_entities().sort( new Comparator<GeoEntity>() {
                    @Override
                    public int compare(GeoEntity o1, GeoEntity o2) {
                        return Double.compare(scores.get(o2.getUniqueName()), scores.get(o1.getUniqueName()));
                    }
                });


                //Set To Ambiguous MODE
                query._presentation_mode = GEO_MODE.AMBIGUOUS;

            } else if(query.get_candidate_geo_entities().size() == 1) {
                //=================================Local Expansion Mode===================================

                //Determine if original benchmark is good enough to not expand
                if(		query.getSupportingTokens().size() > 0 //Only expand if supporting terms exist
                        &&
                        (origBenchmark.scored.size() <= numResults
                                || //Original results not good enough
                                origBenchmark.total_score / origBenchmark.queryNorm < _orig_threshold * numResults)) {

                    System.out.println("Expansion");

                    //Expand Word

                    query.expand(_max_expansion);

                    query._tokens = new Vector<>(query.getSupportingTokens());

                    HashMap<String, Double> scores = new HashMap<>();

                    Iterator<GeoEntity> expQueryIterator = ((QueryBoolGeo) init_query).get_expanded_geo_entities().iterator();

                    //Iterator through all nearby cities of each
                    while(expQueryIterator.hasNext()) {

                        //Create new query:
                        String cityName = expQueryIterator.next().getName().toLowerCase().trim();

                        QueryBoolGeo expandedQuery = new QueryBoolGeo(query._query); //Dummy query
                        Vector<String> _new_terms = new Vector<>(query.getSupportingTokens()); //Add non location terms

                        //Remove older CityName
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
                                normalizedScore > 0.75 * origBenchmark.total_score / origBenchmark.queryNorm) {

                            ((QueryBoolGeo) init_query)._presentation_mode = GEO_MODE.EXPANSION;

                            scores.put(cityName, normalizedScore);

                            //cache
                            //_cache.put(expandedQuery._query, newResults);

                            logs.append(": Qualified!\n");
                            System.out.println(expandedQuery._query + ": qualified with " + normalizedScore);
                        } else {
                            //Remove expanded term if not qualified
                            expQueryIterator.remove();

                            logs.append(": Unqualified!\n");
                            System.out.println(expandedQuery._query + ": unqualified with " + normalizedScore);
                        }
                    }

                    //Sort candidates by their scores
                    /*query.get_expanded_geo_entities().sort( new Comparator<GeoEntity>() {
                        @Override
                        public int compare(GeoEntity o1, GeoEntity o2) {
                            return Double.compare(scores.get(o2.getName().toLowerCase().trim()),
                                    scores.get(o1.getName().toLowerCase().trim()));
                        }
                    });*/

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

    //Return Vector of Scored docs out of best of each candidate
    public Vector<ScoredDocument> returnBestList(QueryBoolGeo query, int numResults) {
        try {
            query._tokens = new Vector<>(query.getSupportingTokens());

            HashMap<Integer, ScoredDocument> uniqDocs = new HashMap<>();

            Iterator<GeoEntity> expQueryIterator = query.get_expanded_geo_entities().iterator();

            //Iterator through all nearby cities of each
            while(expQueryIterator.hasNext()) {

                //Create new query:
                String cityName = expQueryIterator.next().getName().toLowerCase().trim();

                QueryBoolGeo expandedQuery = new QueryBoolGeo(query._query); //Dummy query
                Vector<String> _new_terms = new Vector<>(query.getSupportingTokens()); //Add non location terms

                //Remove older CityName
                _new_terms.add(cityName);

                expandedQuery._tokens = _new_terms;

                System.out.println("Expanded Query: " + expandedQuery._tokens.toString());

                ScoredSumTuple newResults = runQuery(expandedQuery , numResults);

                //Store to List is not a duplicate or higher
                for( ScoredDocument doc : newResults.scored ) {
                    if(uniqDocs.containsKey(doc.getDocID())) {
                        if(uniqDocs.get(doc.getDocID()).getScore() < doc.getScore()) {
                            uniqDocs.put(doc.getDocID(), doc);
                        }
                    } else {
                        uniqDocs.put(doc.getDocID(), doc);
                    }
                }
            }

            //Get Top Highest Scores
            PriorityQueue<ScoredDocument> bestDocs = new PriorityQueue<>();
            for(ScoredDocument doc : uniqDocs.values()) {
                bestDocs.add(doc);
                if(bestDocs.size() > numResults) {
                    bestDocs.poll();
                }
            }

            //Sort Results
            Vector<ScoredDocument> scoredDocs = new Vector<>(bestDocs);
            scoredDocs.sort(new Comparator<ScoredDocument>() {
                @Override
                public int compare(ScoredDocument o1, ScoredDocument o2) {
                    return Double.compare(o2.getScore(),o1.getScore());
                }
            });

            return scoredDocs;

        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
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
                double score = scoreDocumentTFIDF(query, doc);
                rankQueue.add(new ScoredDocument(doc, score));
                //System.out.println(doc.getTitle() + " " + score);

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

            return new ScoredSumTuple(results, sum, Math.sqrt(normSum + 1));
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

    public Double scoreDocumentTFIDF(QueryBoolGeo query, DocumentIndexed doc) {
        Double score = 0.0;

        try {
            int foundTerms = 0;
            //Normal Tokens
            for(String term : query._tokens) {
                int docTermFreq = _indexer.documentTermFrequency(term, doc._docid);
                score = Math.sqrt(docTermFreq) * ((Math.log(_indexer._numDocs / (_indexer.corpusDocFrequencyByTerm(term) + 1.0)) / Math.log(2)) + 1);

                //System.out.println(doc.getTitle() + " " + term + " " + docTermFreq);
                score += Math.pow(_indexer.corpusDocFrequencyByTerm(term), 2) / (doc._numWords + query._tokens.size());

                double overlap = (docTermFreq + 0.0) / doc._numWords;
                //System.out.println(overlap);

                 /*score += Math.sqrt(docTermFreq) * //Term Frequency
                        Math.pow(
                                (Math.log(_indexer._numDocs / (_indexer.corpusDocFrequencyByTerm(term) + 1.0)) / Math.log(2)) + 1.0
                                ,2.0) * //IDF
                        (1.0 / Math.sqrt(query._tokens.size(s))) * overlap; //Overlap; //lengthNorm*/

                //Better Than QL: guatemala
            	 /* score += Math.log(
                        //Probability in Document
                        ((0.8) * _indexer.documentTermFrequency(term, doc._docid) / doc._numWords )
                                +
                                //Smoothing
                                (0.2 * _indexer.corpusTermFrequency(term) / _indexer._totalTermFrequency )
                	);*/
            }

            score = score * (1 + doc.getPageRank()); //multiply by pagerank + 1
                    //(foundTerms / query._tokens.size()); //coord

        } catch(Exception e) {
            e.printStackTrace();
        }

        return score;
    }
}