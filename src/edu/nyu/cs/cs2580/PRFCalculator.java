package edu.nyu.cs.cs2580;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class PRFCalculator {
    private Indexer _indexer;
    private List<Integer> docIds;
    private List<String> terms;

    public PRFCalculator(Indexer _indexer, Vector<ScoredDocument> docs, Query query) {
        this._indexer = _indexer;
        docIds = new ArrayList<Integer>();
        for(ScoredDocument doc : docs) {
            docIds.add(doc.getDocID());
        }
        terms = query._tokens;
    }

    //TODO: Assume equation is: count of word in each document / count of all words in query
    public Map<String, Double> generateOutput() {
        Map<String, Double> score = new HashMap<>();
        double sum = 0.0;

        for(String term: terms) {
            double value = 0;
            for(Integer docId : docIds) {
                int count = _indexer.documentTermFrequency(term, docId);
                value += count;
                sum += count;
            }

            score.put(term, value);
        }

        //Normalize
        for(String key: score.keySet()) {
            score.put(key, score.get(key) / sum);
        }

        return score;
    }

    //Term
    public List<String> sortedTerms(Map<String, Double> termProbabilities, int numTerms) {
        //Sort All Terms
        List<String> sortedTerms = new ArrayList<>(terms);

        Collections.sort( sortedTerms , new Comparator<String>() {
            @Override
            public int compare(String a1, String a2) {
                return Double.compare(termProbabilities.get(a2),termProbabilities.get(a1));
            }
        });

        //Return N number of top Terms
        return sortedTerms.subList(0, Math.min(numTerms, sortedTerms.size()));
    }
}
