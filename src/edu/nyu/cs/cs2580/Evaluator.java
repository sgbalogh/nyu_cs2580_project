package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;

/**
 * Evaluator for HW1.
 *
 * @author fdiaz
 * @author congyu
 */
class Evaluator {
  private static StringBuilder allMetrics;
  private static String _rankerType;

  /**
   * Usage: java -cp src edu.nyu.cs.cs2580.Evaluator [labels] [metric_id]
   */

  public static void main(String[] args) throws IOException {
    Map<String, DocumentRelevances> judgments =
            new HashMap<String, DocumentRelevances>();
    SearchEngine.Check(args.length == 3, "Must provide labels, metric_id, and Ranker Type!");
    allMetrics = new StringBuilder();
    _rankerType = args[2].trim();
    readRelevanceJudgments(args[0], judgments);
    evaluateStdin(Integer.parseInt(args[1]), judgments);
  }

  public static void readRelevanceJudgments(
          String judgeFile, Map<String, DocumentRelevances> judgements)
          throws IOException {
    String line = null;
    BufferedReader reader = new BufferedReader(new FileReader(judgeFile));
    while ((line = reader.readLine()) != null) {
      // Line format: query \t docid \t grade
      Scanner s = new Scanner(line).useDelimiter("\t");
      String query = s.next();
      DocumentRelevances relevances = judgements.get(query);
      if (relevances == null) {
        relevances = new DocumentRelevances();
        judgements.put(query, relevances);
      }
      relevances.addDocument(Integer.parseInt(s.next()), s.next());
      s.close();
    }
    reader.close();
  }

  // @CS2580: implement various metrics inside this function
  public static void evaluateStdin(
          int metric, Map<String, DocumentRelevances> judgments)
          throws IOException {

        /* SGB: Below is a modification to read from a file, instead of sys in */
        /* Will be replaced */
    BufferedReader reader =
            new BufferedReader(new InputStreamReader(System.in));
    //File modFile = new File("/Users/stephen/git/nyu_cs2580/data/out.txt");
    //BufferedReader reader = new BufferedReader(new FileReader(modFile));

    List<Integer> results = new ArrayList<Integer>();
    String line = null;
    String currentQuery = "";
    while ((line = reader.readLine()) != null) {
      Scanner s = new Scanner(line).useDelimiter("\t");
      final String query = s.next();
      if (!query.equals(currentQuery)) {
        if (results.size() > 0) {
          applyMetrics(metric, currentQuery, results, judgments);
          allMetrics.setLength(0);
          results.clear();
        }
        currentQuery = query;
      }
      results.add(Integer.parseInt(s.next()));
      s.close();
    }
    reader.close();
    if (results.size() > 0) {
      applyMetrics(metric, currentQuery, results, judgments);
    }
  }

  private static void applyMetrics(int metric, String currentQuery, List<Integer> results, Map<String, DocumentRelevances> judgments) throws IOException {
    allMetrics.append(currentQuery);

    //Print all
    switch (metric) {
      case -1:
        evaluateQueryInstructor(currentQuery, results, judgments);
        break;
      case 0:
        runMetric0(currentQuery, results, judgments);
        break;
      case 1:
        runMetric1(currentQuery, results, judgments);
        break;
      case 2:
        runMetric2(currentQuery, results, judgments);
        break;
      case 3:
        runMetric3(currentQuery, results, judgments);
        break;
      case 4:
        runMetric4(currentQuery, results, judgments);
        break;
      case 5:
        runMetric5(currentQuery, results, judgments);
        break;
      case 6:
        runMetric6(currentQuery, results, judgments);
        break;
      case 7: //Run all of them
        runMetric0(currentQuery, results, judgments);
        runMetric1(currentQuery, results, judgments);
        runMetric2(currentQuery, results, judgments);
        runMetric3(currentQuery, results, judgments);
        runMetric4(currentQuery, results, judgments);
        runMetric5(currentQuery, results, judgments);
        runMetric6(currentQuery, results, judgments);
        break;
      default:
        System.err.println("Requested metric not implemented!");
    }

    //Print out results of query
    printEvalResults(currentQuery, "results/hw1.3-" + _rankerType + ".tsv");
  }

  public static void evaluateQueryInstructor(
          String query, List<Integer> docids,
          Map<String, DocumentRelevances> judgments) {
    double R = 0.0;
    double N = 0.0;
    for (int docid : docids) {
      DocumentRelevances relevances = judgments.get(query);
      if (relevances == null) {
        System.out.println("Query [" + query + "] not found!");
      } else {
        if (relevances.hasRelevanceForDoc(docid)) {
          R += relevances.getRelevanceForDoc(docid);
        }
        ++N;
      }
    }
    System.out.println(query + "\t" + Double.toString(R / N));
  }

  private static Double fMeasure(String query, List<Integer> results,
                                 Map<String, DocumentRelevances> judgments, double a, int k, Vector<Double[]> matrix) {

    double prec = getPrecisionAt(k, matrix);
    double rec = getRecallAt(k, matrix);

    if (isZero(prec) || isZero(rec))
      return 0.0;

    //F-Measure = (a * (1 / Prec) + (1-a) * (1 / Rec)) ^ -1
    return 1 / (a * (1 / prec) + (1 - a) * (1 / rec));
  }

  private static boolean isZero(Double num) {
    return Math.abs(num) < 3 * Double.MIN_VALUE;
  }

  private static Double normalizedDiscountCumulativeGain(String query, List<Integer> results,
                                                         Map<String, DocumentRelevances> judgments, int k) {
    Double DCG = 0.0;
    Double iDCG = 0.0;


    for (int i = 0; i < Math.min(k, results.size()); i++) {
      //Get Score -> Gain for Document
      int docId = results.get(i);

      double docScore = judgments.get(query).getGain(docId);

      //Get Log Placement base 2
      double denom = Math.log(i + 2) / Math.log(2);

      //Update Scores
      DCG += (docScore / denom);
      iDCG += (10 / denom); //Perfect Score for Normalization
    }

    return DCG / iDCG;
  }

  private static void runMetric0(String query, List<Integer> docids,
                                 Map<String, DocumentRelevances> judgments) {
        /* Returns precision & 1, 5, 10 */
    Vector<Double[]> matrix = createPrecisionRecallMatrix(query, docids, judgments);
    double P1 = getPrecisionAt(1, matrix);
    double P5 = getPrecisionAt(5, matrix);
    double P10 = getPrecisionAt(10, matrix);
    //System.out.println(query + "\t" + P1 + "," + P5 + "," + P10);
    allMetrics.append("\t" + P1 + "\t" + P5 + "\t" + P10);
  }

  private static void runMetric1(String query, List<Integer> docids,
                                 Map<String, DocumentRelevances> judgments) {
    Vector<Double[]> matrix = createPrecisionRecallMatrix(query, docids, judgments);
    double R1 = getRecallAt(1, matrix);
    double R5 = getRecallAt(5, matrix);
    double R10 = getRecallAt(10, matrix);

    //System.out.println(query + "\t" + R1 + "," + R5 + "," + R10);
    allMetrics.append("\t" + R1 + "\t" + R5 + "\t" + R10);
  }

  private static void runMetric2(String query, List<Integer> results,
                                 Map<String, DocumentRelevances> judgments) {
    int[] indexes = {1, 5, 10};
    double a = 0.5;

    Vector<Double[]> matrix = createPrecisionRecallMatrix(query, results, judgments);

    for (int k : indexes) {
      Double answer = fMeasure(query, results, judgments, a, k, matrix);
      allMetrics.append("\t").append(answer);
    }
  }


  private static void runMetric3(String query, List<Integer> docids,
                                 Map<String, DocumentRelevances> judgments) {
    Vector<Double> interpolated = createInterpolatedPRMatrix(createPrecisionRecallMatrix(query, docids, judgments));
    String toPrint = "";
    for (Double value : interpolated) {
      if (!toPrint.equals("")) {
        toPrint += "\t";
      }
      toPrint += value.toString();
    }
    // System.out.println(toPrint);
    allMetrics.append("\t").append(toPrint);
  }

  private static void runMetric4(String query, List<Integer> docids,
                                 Map<String, DocumentRelevances> judgments) {

    Vector<Double[]> PRmatrix = createPrecisionRecallMatrix(query, docids, judgments);
    int length = PRmatrix.size();
    double totalPrecision = 0.0;
    for (int i = 0; i < length; i++) {
      totalPrecision += PRmatrix.get(i)[0];
    }
    double avgP;
    avgP = totalPrecision / (double) length;
    //System.out.println(query + "\t Average Precision = " + avgP);
    allMetrics.append("\t").append(avgP);

  }

  private static void runMetric5(String query, List<Integer> results,
                                 Map<String, DocumentRelevances> judgments) { //Normalized Discounted Cumulative Gain
    int[] indexes = {1, 5, 10};

    for (int k : indexes) { //Calculate Scores for 1,5,10
      Double answer = normalizedDiscountCumulativeGain(query, results, judgments, k);
      allMetrics.append("\t").append(answer);
    }
  }

  private static void runMetric6(String query, List<Integer> docids,
                                 Map<String, DocumentRelevances> judgments) {
    double recip_rank = getReciprocalRank(query, docids, judgments);
    // System.out.println(query + "\t" + recip_rank);
    allMetrics.append("\t").append(recip_rank);
  }

  private static double getPrecisionAt(int rank, Vector<Double[]> matrix) {
    return matrix.get((rank - 1))[0];
  }

  private static double getRecallAt(int rank, Vector<Double[]> matrix) {
    return matrix.get((rank - 1))[1];
  }

  private static double getReciprocalRank(String query, List<Integer> docids,
                                          Map<String, DocumentRelevances> judgments) {
    int counter = 1;
    for (int docid : docids) {
      DocumentRelevances relevances = judgments.get(query);
      if (relevances == null) {
        System.out.println("Query [" + query + "] not found!");
      } else {
        if (relevances.hasRelevanceForDoc(docid)) {
          if (relevances.getRelevanceForDoc(docid) > 0) {
            return (double) 1 / counter;
          }
        }
      }
      counter++;
    }
    return 0.0; // If no relevant docs found
  }

  private static Vector<Double[]> createPrecisionRecallMatrix(String query, List<Integer> docids,
                                                              Map<String, DocumentRelevances> judgments) {

    double total_relevant_docs = (double) judgments.get(query).total_relevant;

    Vector<Double[]> toReturn = new Vector<>();

    double relevant_docs = 0.0;
    int counter = 1;
    for (int docid : docids) {
      DocumentRelevances relevances = judgments.get(query);
      if (relevances == null) {
        System.out.println("Query [" + query + "] not found!");
      } else {
        if (relevances.hasRelevanceForDoc(docid)) {
          relevant_docs += relevances.getRelevanceForDoc(docid);
        }
        Double[] pair = new Double[2]; // [Precision, Recall]
        pair[0] = relevant_docs / counter;
        if (total_relevant_docs == 0) {
          pair[1] = 0.0;
        } else {
          pair[1] = relevant_docs / total_relevant_docs;
        }
        toReturn.add(pair);
      }
      counter++;
    }
    return toReturn;
  }

  private static Vector<Double> createInterpolatedPRMatrix(Vector<Double[]> PRMatrix) {
    Vector<Double> toReturn = new Vector<>();
    for (int i = 0; i <= 10; i++) {
      double desired_recall_pt = (double) i / 10;
      double interp_precision_pt = maxPrecisionHigherRecall(PRMatrix, desired_recall_pt);
      toReturn.add(interp_precision_pt);
    }
    return toReturn;
  }

  private static double maxPrecisionHigherRecall(Vector<Double[]> PRMatrix, double recall_pt) {
    double max_precision = 0.0;
    for (int i = 0; i < PRMatrix.size(); i++) {
      double observed_precision = PRMatrix.get(i)[0];
      double observed_recall = PRMatrix.get(i)[1];
      if (observed_recall >= recall_pt) {
        if (observed_precision > max_precision) {
          max_precision = observed_precision;
        }
      }
    }
    return max_precision;
  }

  private static void printEvalResults(String query, String outputFile) throws IOException {
    if (outputFile == null)
      return;

    File file = new File(outputFile);

    //if file doesn't exists, then create it
    if (!file.exists()) {
      file.createNewFile();
    }

    //true = append file
    FileWriter fileWritter = new FileWriter(outputFile, true);
    BufferedWriter bufferWritter = new BufferedWriter(fileWritter);

    //QUERY<TAB>METRIC_1<TAB>...<TAB>METRIC_N
    bufferWritter.write(allMetrics.toString() + "\n");

    bufferWritter.close();
  }

  public static class DocumentRelevances {
    private Map<Integer, Double> relevances = new HashMap<Integer, Double>();

    public int total_relevant = 0;

    //Gains data structures: DocID -> Gains
    private Map<Integer, Double> gains = new HashMap<Integer, Double>();

    public DocumentRelevances() {
    }

    private static double convertToBinaryRelevance(String grade) {
      if (grade.equalsIgnoreCase("Perfect") ||
              grade.equalsIgnoreCase("Excellent") ||
              grade.equalsIgnoreCase("Good")) {
        return 1.0;
      }
      return 0.0;
    }

    private static double convertToGain(String grade) {
      if (grade.equalsIgnoreCase("Perfect")) {
        return 10.0;
      } else if (grade.equalsIgnoreCase("Excellent")) {
        return 7.0;
      } else if (grade.equalsIgnoreCase("Good")) {
        return 5.0;
      } else if (grade.equalsIgnoreCase("Fair")) {
        return 1.0;
      }
      return 0.0;
    }

    public void addDocument(int docid, String grade) {
      relevances.put(docid, convertToBinaryRelevance(grade));
      //Add grade as well
      gains.put(docid, convertToGain(grade));
      if (convertToBinaryRelevance(grade) > 0) {
        total_relevant++;
      }
    }

    public boolean hasRelevanceForDoc(int docid) {
      return relevances.containsKey(docid);
    }

    public double getGain(int docid) {
      if (!gains.containsKey(docid))
        return 0.0;
      return gains.get(docid);
    }

    public double getRelevanceForDoc(int docid) {
      return relevances.get(docid);
    }
  }
}