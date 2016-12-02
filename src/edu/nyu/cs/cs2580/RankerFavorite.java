package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2 based on a refactoring of your favorite
 * Ranker (except RankerPhrase) from HW1. The new Ranker should no longer rely
 * on the instructors' {@link IndexerFullScan}, instead it should use one of
 * your more efficient implementations.
 */
public class RankerFavorite extends Ranker {
  public static double _alpha;

  public RankerFavorite(Options options,
                        CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);

    _alpha = 0.5; //Default Alpha is 0.5 for Smoothing
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    if(!(query instanceof QueryPhrase))
      throw new IllegalStateException("Please Use Query Phrase");

    if(query._tokens.size() == 0)
    	return new Vector<ScoredDocument>();
    
    try {
      Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();

      DocumentIndexed doc = null;
      int docid = -1;

      //Get Document Indexed
      while ((doc = (DocumentIndexed) _indexer.nextDoc((QueryPhrase) query, docid)) != null) {
        rankQueue.add(new ScoredDocument(doc, scoreDocumentQL((QueryPhrase) query, doc)));

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
      Collections.sort(results, Collections.reverseOrder());
      return results;
    } catch(Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  //Get Query Likelihood Score
  public Double scoreDocumentQL(QueryPhrase query, DocumentIndexed doc) {
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

    return score;
  }
}