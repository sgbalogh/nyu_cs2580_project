package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class Bhattacharyya {

    private static HashMap<String, String> queryFiles;

    public static double BhattacharyyaCoeff(String query1, String query2) throws IOException {
        //TODO: Assume V is set of terms, Get Intersection

        String line = null;

        Set<String> terms = new HashSet<>();

        //Query 1
        HashMap<String, Double> termProb1 = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(queryFiles.get(query1)));

        while((line = br.readLine()) != null) {
            String[] termVal = line.split("\t");
            terms.add(termVal[0]);
            termProb1.put(termVal[0], Double.parseDouble(termVal[1]));
        }

        br.close();

        //Query 2
        HashMap<String, Double> termProb2 = new HashMap<>();
        br = new BufferedReader(new FileReader(queryFiles.get(query2)));

        while((line = br.readLine()) != null) {
            String[] termVal = line.split("\t");
            terms.add(termVal[0]);
            termProb2.put(termVal[0], Double.parseDouble(termVal[1]));
        }

        br.close();

        //Sum
        double sum = 0.0;
        for(String term: terms) {
            if(termProb1.containsKey(term) && termProb2.containsKey(term))
                sum += Math.sqrt(termProb1.get(term) * termProb1.get(term));
        }

        return sum;
    }

    public static void main(String[] args) throws NumberFormatException, IOException {
        // java -cp src edu.nyu.cs.cs2580.Bhattacharyya <PATH-TO-PRF-OUTPUT> <PATH-TO-OUTPUT>
        //$q:$prfout > prf.tsv
        //Filename is query
        // <QUERY-1><QUERY-2><COEFFICIENT>

        String prfOutput = args[0];
        String outputFile = args[1];

        queryFiles = new HashMap<>();

        //Parse querys and filename
        BufferedReader br = new BufferedReader(new FileReader(prfOutput));
        String line = null;
        while((line = br.readLine()) != null) {
            String[] queryFilename = line.split(":");
            queryFiles.put(queryFilename[0], queryFilename[1]);
        }
        br.close();

        //Calculate Coefficients
        StringBuilder result = new StringBuilder();
        for(String query1 : queryFiles.keySet())
            for(String query2 : queryFiles.keySet())
                if(!query1.equals(query2))
                    result.append(query1).append("\t").append(query2).append("\t")
                            .append(BhattacharyyaCoeff(query1, query2)).append("\n");

        //Write output
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
        bw.write(result.toString());
        bw.close();
    }
}