package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is
 * ["new york city"], the presence of the phrase "new york city" must be
 * recorded here and be used in indexing and ranking.
 */
public class QueryPhrase extends Query {

  public HashSet<String> stopWords;

  public QueryPhrase(String query) {
    super(query);
    
    stopWords = new HashSet<>();
    //Load stopwords
    loadStopWords("english.stop.txt");
  }
  
  private void loadStopWords(String stopWordsList) {
	  try {
		  BufferedReader br = new BufferedReader(new FileReader(stopWordsList));
		  String word;
		  while((word = br.readLine()) != null)
			  stopWords.add(word.toLowerCase().trim());
		  br.close();
	  }catch(Exception e) {
		  e.printStackTrace();
	  }
  }

  @Override
  public void processQuery() {



  }
}
