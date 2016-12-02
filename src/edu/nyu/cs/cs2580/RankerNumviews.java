package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @author congyu
 * @author fdiaz
 * @CS2580: Use this template to implement the numviews ranker for HW1.
 */
public class RankerNumviews extends Ranker {

    public RankerNumviews(Options options,
                          CgiArguments arguments, Indexer indexer) {
        super(options, arguments, indexer);
        System.out.println("Using Ranker: " + this.getClass().getSimpleName());
    }

    @Override
    public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    	try {
  	      Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();

  	      DocumentIndexed doc = null;
  	      int docid = -1;

  	      //Get Document Indexed
  	      while ((doc = (DocumentIndexed) _indexer.nextDoc((QueryPhrase) query, docid)) != null) {
  	        rankQueue.add(new ScoredDocument(doc, doc.getNumViews()));

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
}
