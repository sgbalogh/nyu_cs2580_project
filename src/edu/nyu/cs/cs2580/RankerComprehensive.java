package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3 based on your {@code RankerFavorite}
 * from HW2. The new Ranker should now combine both term features and the
 * document-level features including the PageRank and the NumViews.
 */
public class RankerComprehensive extends Ranker {
  private float _betaPageRank = 100;
  private float _betaNumviews = (float) 0.01;

  public RankerComprehensive(Options options,
                      CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    System.out.println("  with beta values" +
            ": pagerank=" + Float.toString(_betaPageRank) +
            ", numviews=" + Float.toString(_betaNumviews));

    Vector<ScoredDocument> all;
    HashMap<Integer, Double> favorite = normalizeByMaxMin(allScoredFavorite(query, numResults));
    HashMap<Integer, Double> numViews = normalizeByMaxMin(allScoredNumViews(query, numResults));
    HashMap<Integer, Double> pageRank = normalizeByMaxMin(allScoredPageRank(query, numResults));

    HashMap<Integer, Double> linearModelScores = getLinearModelScores(favorite, numViews, pageRank);

    all = vectorizeLinearScores(linearModelScores, query._query);
    Collections.sort(all, Collections.reverseOrder());
    Vector<ScoredDocument> results = new Vector<>();
    for (int i = 0; i < all.size() && i < numResults; ++i) {
      results.add(all.get(i));
    }
    return results;

  }

  public Vector<ScoredDocument> allScoredFavorite(Query query, int numResults) {
    Vector<ScoredDocument> all = new Vector<ScoredDocument>();
    RankerFavorite ranker = new RankerFavorite(_options, _arguments, _indexer);
    all = ranker.runQuery(query, numResults);
    Collections.sort(all, Collections.reverseOrder());
    return all;
  }

  public Vector<ScoredDocument> allScoredNumViews(Query query, int numResults) {
    Vector<ScoredDocument> all = new Vector<ScoredDocument>();
    RankerNumviews ranker = new RankerNumviews(_options, _arguments, _indexer);
    all = ranker.runQuery(query, numResults);
    Collections.sort(all, Collections.reverseOrder());
    return all;
  }


  public Vector<ScoredDocument> allScoredPageRank(Query query, int numResults) {
    Vector<ScoredDocument> all = new Vector<ScoredDocument>();
    RankerPagerank ranker = new RankerPagerank(_options, _arguments, _indexer);
    all = ranker.runQuery(query, numResults);
    Collections.sort(all, Collections.reverseOrder());
    return all;
  }

  /*
  public HashMap<Integer, Double> normalizeByRank(Vector<ScoredDocument> vector) {
    HashMap<Integer, Double> toReturn = new HashMap<>();
    int counter = 1;
    for (ScoredDocument doc : vector) {
      double new_score = 1.0 / (double) counter;
      toReturn.put(doc.getDoc()._docid, new_score);
      counter++;
    }
    return toReturn;
  }
  */

  public HashMap<Integer, Double> normalizeByMaxMin(Vector<ScoredDocument> vector) {
    HashMap<Integer, Double> toReturn = new HashMap<>();

    if (vector.size() != 0) {

      double max_score = vector.get(0).getScore();
      double min_score = vector.get(vector.size() - 1).getScore();

      for (ScoredDocument doc : vector) {
        double new_score;
        if (max_score == min_score) {
          new_score = 0.0;
        } else {
          new_score = (doc.getScore() - min_score) / (max_score - min_score);
        }
        toReturn.put(doc.getDoc()._docid, new_score);
      }
    }
    return toReturn;
  }



  public HashMap<Integer, Double> getLinearModelScores(HashMap<Integer, Double> favorite,
                                                       HashMap<Integer, Double> numviews,
                                                       HashMap<Integer, Double> pagerank
                                                       ) {
    HashMap<Integer, Double> toReturn = new HashMap<>();

    for (Map.Entry<Integer, Double> entry : favorite.entrySet()) {
      int doc_id = entry.getKey();
      double fav_score = entry.getValue();

      double numviews_score;
      if (numviews.containsKey(doc_id)) {
        numviews_score = numviews.get(doc_id); // Ql
      } else {
        numviews_score = 0.0;
      }

      double pagerank_score;
      if (pagerank.containsKey(doc_id)) {
        pagerank_score = pagerank.get(doc_id); // Ql
      } else {
        pagerank_score = 0.0;
      }

      double score = fav_score * (1 +_betaNumviews * numviews_score + _betaPageRank * pagerank_score);
      toReturn.put(doc_id, score);
    }
    return toReturn;
  }

  public Vector<ScoredDocument> vectorizeLinearScores(HashMap<Integer, Double> map, String query) {
    Vector<ScoredDocument> toReturn = new Vector<>();

    for (Map.Entry<Integer, Double> entry : map.entrySet()) {
      ScoredDocument doc = new ScoredDocument(_indexer.getDoc(entry.getKey()), entry.getValue());
      toReturn.add(doc);
    }
    return toReturn;
  }
}