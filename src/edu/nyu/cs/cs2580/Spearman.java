package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Spearman {
    public static Integer[] pageranks; //For each document, get rating score
    public static Integer[] numviews;

    public static class Tuple {
        public double score;
        public int doc;

        public Tuple(int doc, double score) {
            this.doc = doc;
            this.score = score;
        }
    }

    //Populate Data Structures
    public static Integer[] generateScores( BufferedReader br ) throws NumberFormatException, IOException {
        //PageRank
        List<Tuple> documents = new ArrayList<Tuple>();
        int document = 0;

        String line = null;
        while((line = br.readLine()) != null) {
            documents.add(new Tuple(document, Double.parseDouble(line)));
            document++;
        }

        //Reverse Sort
        Collections.sort( documents , new Comparator<Tuple>() {
            @Override
            public int compare(Tuple a1, Tuple a2) {
                return Double.compare(a2.score, a1.score);
            }
        });

        Integer[] scores = new Integer[documents.size()];

        int value = 1;
        for(Tuple sortedDoc : documents) {
            scores[sortedDoc.doc] = value;
            value++;
        }

        return scores;
    }

    //Calculate Z
    public static double calcZ() throws NumberFormatException, IOException {
        int sum = 0;

        for(int score : pageranks)
            sum += score;

        return sum / pageranks.length;
    }

    public static double calcP(double z) throws NumberFormatException, IOException {
        if(pageranks.length != numviews.length)
            throw new IllegalStateException("Doc Sizes not equal");

        Double numer = 0.0;
        Double denomPR = 0.0;
        Double denomNV = 0.0;

        for(int i = 0; i < pageranks.length; i++) {
            numer += (pageranks[i] - z) * (numviews[i] - z);
            denomPR += Math.pow(pageranks[i] - z, 2);
            denomNV += Math.pow(numviews[i] - z, 2);
        }

        return (numer / (Math.sqrt(denomPR * denomNV)));
    }

    //TODO: Test
    public static void main(String[] args) throws NumberFormatException, IOException {
        //java edu.nyu.cs.cs2580.Spearman <PATH-TO-PAGERANKS> <PATH-TO-NUMVIEWS>

		/*Format:
		 * pr for doc1
		 * pr for doc2
		 */

        String pagerankFile = args[0];
        String numviewFile = args[1];

        BufferedReader pr = new BufferedReader(new FileReader(pagerankFile));
        BufferedReader nv = new BufferedReader(new FileReader(numviewFile));

        //Populate Values
        pageranks = generateScores(pr);
        numviews = generateScores(nv);

        //Calculate Z
        double z = calcZ();

        //calculate p
        System.out.println(calcP(z));
    }
}