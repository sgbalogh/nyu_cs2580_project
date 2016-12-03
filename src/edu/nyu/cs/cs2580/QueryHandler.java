package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.Map;
import java.util.Vector;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * Handles each incoming query, students do not need to change this class except
 * to provide more query time CGI arguments and the HTML output.
 *
 * N.B. This class is not thread-safe.
 *
 * @author congyu
 * @author fdiaz
 */

//TODO: DEAL WITH CGI ARGUMENT that signifies whether or not expansion has already been performed
  // e.g. ...&expanded=true
class QueryHandler implements HttpHandler {

  /**
   * CGI arguments provided by the user through the URL. This will determine
   * which Ranker to use and what output format to adopt. For simplicity, all
   * arguments are publicly accessible.
   */
  public static class CgiArguments {
    // The raw user query
    public String _query = "";
    // How many results to return
    private int _numResults = 10;

    // How many terms to return
    private int _numTerms= 10;

    // The type of the ranker we will be using.
    public enum RankerType {
      NONE,
      FULLSCAN,
      CONJUNCTIVE,
      FAVORITE,
      COSINE,
      PHRASE,
      QL,
      LINEAR,
      COMPREHENSIVE,
    }
    public RankerType _rankerType = RankerType.NONE;

    // The output format.
    public enum OutputFormat {
      TEXT,
      HTML,
      TSV,
    }
    public OutputFormat _outputFormat = OutputFormat.TEXT;

    public CgiArguments(String uriQuery) {
      String[] params = uriQuery.split("&");
      for (String param : params) {
        String[] keyval = param.split("=", 2);
        if (keyval.length < 2) {
          continue;
        }
        String key = keyval[0].toLowerCase();
        String val = keyval[1];
        if (key.equals("query")) {
          _query = val;
        } else if (key.equals("numdocs")) {
          try {
            _numResults = Integer.parseInt(val);
          } catch (NumberFormatException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }
        } else if (key.equals("numterms")) {
          try {
            _numTerms = Integer.parseInt(val);
          } catch (NumberFormatException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }
        } else if (key.equals("ranker")) {
          try {
            _rankerType = RankerType.valueOf(val.toUpperCase());
          } catch (IllegalArgumentException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }
        } else if (key.equals("format")) {
          try {
            _outputFormat = OutputFormat.valueOf(val.toUpperCase());
          } catch (IllegalArgumentException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }
        }
      }  // End of iterating over params
    }
  }

  // For accessing the underlying documents to be used by the Ranker. Since
  // we are not worried about thread-safety here, the Indexer class must take
  // care of thread-safety.
  private Indexer _indexer;
  private LocationParser _location_parser;

  public QueryHandler(Options options, Indexer indexer, SpatialEntityKnowledgeBase gkb) {
    _indexer = indexer;
    _location_parser = new LocationParser(indexer, gkb);
    // Create LocationParser here, feed it a knowledge base reference
    // GKB created (loaded from disk) in SearchEngine
  }

  private void respondWithMsg(HttpExchange exchange, final String message)
          throws IOException {
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/plain");
    exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
    OutputStream responseBody = exchange.getResponseBody();
    responseBody.write(message.getBytes());
    responseBody.close();
  }

  private void respondWithTSVFile(String ranker, final String message, HttpExchange exchange) throws IOException {
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/plain");
    exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
    String outputFile = "prfoutput.tsv";
    File file = new File(outputFile);
    //if file doesn't exists, then create it
    if(!file.exists()){
      file.createNewFile();
    }
    //true = append file
    FileWriter fileWritter = new FileWriter(outputFile,true);
    BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
    //QUERY<TAB>DOCUMENTID-1<TAB>TITLE<TAB>SCORE
    bufferWritter.write(message);
    bufferWritter.close();
    OutputStream responseBody = exchange.getResponseBody();
    responseBody.write(message.getBytes());
    responseBody.close();
  }

  private void respondWithHtml(HttpExchange exchange, final String message)
          throws IOException {
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/html");
    exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
    OutputStream responseBody = exchange.getResponseBody();
    responseBody.write(message.getBytes());
    responseBody.close();
  }

  //TODO: create map from data stored in GeoEntities (within QBG object)
  private void constructMapWidget() {

  }


  private void constructTextOutput(
          final Vector<ScoredDocument> docs, StringBuffer response) {
    for (ScoredDocument doc : docs) {
      response.append(response.length() > 0 ? "\n" : "");
      response.append(doc.asTextResult());
    }
    response.append(response.length() > 0 ? "\n" : "");
  }

  //Construct PRF Output
  private void constructPRFTextOutput(Vector<ScoredDocument> docs, StringBuffer response, Query query, int numTerms) {
    //Process Probabilities
    PRFCalculator psfCalc = new PRFCalculator(_indexer, docs, query);
    Map<String, Double> termProbabilities = psfCalc.generateOutput();

    //Term
    for (String term : psfCalc.sortedTerms(termProbabilities, numTerms)) {
      response.append(response.length() > 0 ? "\n" : "");
      response.append(term).append("\t").append(termProbabilities.get(term));
    }
    response.append(response.length() > 0 ? "\n" : "");
  }

  public void constructHtmlOutput(final Vector<ScoredDocument> docs, StringBuffer response) {

  }

  public void handle(HttpExchange exchange) throws IOException {
    String requestMethod = exchange.getRequestMethod();
    if (!requestMethod.equalsIgnoreCase("GET")) { // GET requests only.
      return;
    }

    // Print the user request header.
    Headers requestHeaders = exchange.getRequestHeaders();
    System.out.print("Incoming request: ");
    for (String key : requestHeaders.keySet()) {
      System.out.print(key + ":" + requestHeaders.get(key) + "; ");
    }
    System.out.println();

    // Validate the incoming request.
    String uriQuery = exchange.getRequestURI().getQuery();
    String uriPath = exchange.getRequestURI().getPath();
    if (uriPath == null || uriQuery == null) {
      respondWithMsg(exchange, "Something wrong with the URI!");
    }
    if (uriPath.equals("/search") || uriPath.equals("/prf")) {
      System.out.println("Query: " + uriQuery);

      // Process the CGI arguments.
      CgiArguments cgiArgs = new CgiArguments(uriQuery);
      if (cgiArgs._query.isEmpty()) {
        respondWithMsg(exchange, "No query is given!");
      }

      // Create the ranker.
      Ranker ranker = Ranker.Factory.getRankerByArguments(
              cgiArgs, SearchEngine.OPTIONS, _indexer);
      if (ranker == null) {
        respondWithMsg(exchange,
                "Ranker " + cgiArgs._rankerType.toString() + " is not valid!");
      }

      // Processing the query.

      //===============================================
      // USE LOCATION PARSER
      // Feed it a string of the entire user-issued query
      //===============================================
      Query processedQuery = new QueryPhrase(cgiArgs._query);
      processedQuery.processQuery();


      // Ranking.
      //TODO: implement this
      // LIST OF GEOENTITIES IS WITHIN QBG
      Vector<ScoredDocument> scoredDocs =
              ranker.runQuery(processedQuery, cgiArgs._numResults);

      StringBuffer response = new StringBuffer();
      switch (cgiArgs._outputFormat) {
        case TEXT:
          if(uriPath.equals("/prf")) //Process Pseudo Relevance Feedback
            constructPRFTextOutput(scoredDocs, response, processedQuery, cgiArgs._numTerms );
          else if(uriPath.equals("/search"))
            constructTextOutput(scoredDocs, response);

          respondWithMsg(exchange, response.toString());
          break;
        case HTML:
          constructHtmlOutput(scoredDocs, response);
          respondWithHtml(exchange, response.toString());
          break;
        case TSV:
            if(uriPath.equals("/prf")) //Process Pseudo Relevance Feedback
                constructPRFTextOutput(scoredDocs, response, processedQuery, cgiArgs._numTerms );
            else
            	constructTextOutput(scoredDocs, response);
          respondWithTSVFile(cgiArgs._rankerType.name(), response.toString(), exchange);
          break;
        default:
          // nothing
      }
      respondWithMsg(exchange, response.toString());
      System.out.println("Finished query: " + cgiArgs._query);
    } else {
      respondWithMsg(exchange, "Only /search is handled!");
    }
  }
}