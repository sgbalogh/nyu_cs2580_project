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
    if (_query == null || _query.length() == 0) {
      return;
    }

    Stemmer stemmer = new Stemmer();
    StringBuilder buffer;
    
    //Split by spaces or quotes
    Matcher m = Pattern.compile("([^\"\\s]+|[^\"]\\S+|\".*?\")\\s*").matcher(_query);
    while (m.find()) {
      String token = m.group(1).toLowerCase().replaceAll("[^A-Za-z0-9\"\\s]", "");
      
      //If starts with quotes, it's a phrase
      if(token.startsWith("\"")) {
        token = token.replace("\"", "").trim();
        
        //Remove Stopwords from phrase and stem
        buffer = new StringBuilder();
        for(String str: token.split("\\s+")) {
        	if(stopWords.contains(str))
        		continue;
        	
        	if(buffer.length() != 0)
        		buffer.append(" ");
        	
        	stemmer.add(str.toCharArray(), str.length());
        	stemmer.stem();
        	buffer.append(stemmer.toString());
        }
        token = buffer.toString();
      } else {
        token = token.trim();

        //Do not include standard stop words if not phrase
        if(stopWords.contains(token))
          continue;
        
    	stemmer.add(token.toCharArray(), token.length());
    	stemmer.stem();
    	token = stemmer.toString();
      }

      if(token.length() > 0)
        _tokens.add(token);

    }
  }
}
