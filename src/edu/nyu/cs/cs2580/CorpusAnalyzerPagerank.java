package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;
import edu.nyu.cs.cs2580.SearchEngine.Options;
import org.jsoup.Jsoup;

/**
 * @CS2580: Implement this class for HW3.
 */
public class CorpusAnalyzerPagerank extends CorpusAnalyzer {

  Map<Integer,Double> R_Calculated1 = new HashMap<>();
  Map<Integer,Double> R_final = new HashMap<>();
  Vector<Double> R_final_toSave = new Vector<>();
  //Map<Integer,Double> R_Calculated11 = new HashMap<>();
  Double lambda1 = 0.1, lambda2 = 0.9;
  Map<String,Integer> corpus_documents = new HashMap<>();
  Map<String,Integer> corpus_documents2 = new HashMap<>();

  List<File> all_documents = new ArrayList<>();
  List<File> all_documents_sgb = new ArrayList<>();
  Integer len;
  Integer filtered_length=0;

  public class Graph_Structure<V> {

    Integer no_of_vertices= 0;

    public class Edge<V>{
      private V vertex;

      public Edge(V v){
        vertex = v;
      }

      public V getVertex() {
        return vertex;
      }


    }

    private Map<V, List<Edge<V>>> adjoining_nodes = new HashMap<V, List<Edge<V>>>();


    public void add(V vertex) {
      if (adjoining_nodes.containsKey(vertex))
        return;
      adjoining_nodes.put(vertex, new ArrayList<Edge<V>>());
      no_of_vertices +=1;
    }


    public Integer getNumberOfVertices(){
      return no_of_vertices;
    }


    public List<V> getVerticesSet(){
      List<V> setToReturn = new ArrayList<V>();
      for(Map.Entry<V, List<Edge<V>>> entry: adjoining_nodes.entrySet()) {
        setToReturn.add(entry.getKey());
      }
      return setToReturn;
    }

    public boolean contains(V vertex) {
      return adjoining_nodes.containsKey(vertex);
    }

    public void add(V start, V end) {
      this.add(start);
      this.add(end);
      adjoining_nodes.get(start).add(new Edge<V>(end));
    }

    public int no_of_outbound_nodes(int vertex) {
      return adjoining_nodes.get(vertex).size();
    }


    public List<V> outbound_nodes(V vertex) {
      List<V> list = new ArrayList<V>();
      for(Edge<V> e: adjoining_nodes.get(vertex))
        list.add(e.vertex);
      return list;
    }



    public List<V> inbound_nodes(V inboundVertex) {
      List<V> inList = new ArrayList<V>();
      for (V to : adjoining_nodes.keySet()) {
        for (Edge e : adjoining_nodes.get(to))
          if (e.vertex.equals(inboundVertex))
            inList.add(to);
      }
      return inList;
    }


    public void mergeNodes_redirect_special(V from,V to){

      List<V> list_of_node_to_remove_inbound = new ArrayList<V>();
      List<V> list_of_node_to_add_inbound = new ArrayList<V>();
      List<V> list_of_node_to_remove_outbound = new ArrayList<V>();
      List<V> list_of_node_to_add_outbound = new ArrayList<V>();
      list_of_node_to_remove_inbound=inbound_nodes(from);
      list_of_node_to_add_inbound=inbound_nodes(to);

      list_of_node_to_remove_outbound=outbound_nodes(from);
      list_of_node_to_add_outbound=outbound_nodes(to);


      int size1=list_of_node_to_remove_inbound.size();
      int size2=list_of_node_to_add_inbound.size();
      int size3=list_of_node_to_remove_outbound.size();
      int size4=list_of_node_to_add_outbound.size();


      for(int i=0; i<size1; i++){
        if(!(list_of_node_to_remove_inbound.get(i)).equals(to)){
          if(! isEdge(list_of_node_to_remove_inbound.get(i),to)){
            add(list_of_node_to_remove_inbound.get(i),to);
          }
        }
        remove_edge(list_of_node_to_remove_inbound.get(i), from);

      }

      for(int i=0; i< size3; i++){
        if(! isEdge(to,list_of_node_to_remove_outbound.get(i))){
          if( ! to.equals(list_of_node_to_remove_outbound.get(i))){
            add(to,list_of_node_to_remove_outbound.get(i));
          }
        }
      }


      adjoining_nodes.remove(from);
      no_of_vertices -=1;
    }

    public void remove_edge(V from, V to){
      List<Edge<V>> lis = adjoining_nodes.get(from);
      for (int i=0; i<lis.size(); i++) {
        if (((lis.get(i)).getVertex()).equals(to)) {
          lis.remove(i);
        }

      }
      adjoining_nodes.replace(from,lis);

    }


    public boolean isEdge(V from, V to) {
      for(Edge<V> e :  adjoining_nodes.get(from)){
        if(e.vertex.equals(to))
          return true;
      }
      return false;
    }

  }


  public Graph_Structure<Integer> corpus_graph = new Graph_Structure<Integer>();
  public CorpusAnalyzerPagerank(Options options) {
    super(options);
  }

  public void remove_from_list(String removeThis){


    int index_to_remove=0;
    for(File f: all_documents){
      if(f.getName().equalsIgnoreCase(removeThis)){

        all_documents.remove(index_to_remove);

        return;
      }
      index_to_remove++;
    }
  }
  
	private String[] parseDocument(File doc) throws Exception {
  		return cleanDocument(Jsoup.parse(doc, "UTF-8").text()).split("\\s+");
  	}

  	private String cleanDocument(String document_body) {
  		return document_body.replaceAll("[^A-Za-z0-9\\s]", "").toLowerCase();
  	}

  /**
   * This function processes the corpus as specified inside {@link _options}
   * and extracts the "internal" graph structure from the pages inside the
   * corpus. Internal means we only store links between two pages that are both
   * inside the corpus.
   *
   * Note that you will not be implementing a real crawler. Instead, the corpus
   * you are processing can be simply read from the disk. All you need to do is
   * reading the files one by one, parsing them, extracting the links for them,
   * and computing the graph composed of all and only links that connect two
   * pages that are both in the corpus.
   *
   * Note that you will need to design the data structure for storing the
   * resulting graph, which will be used by the {@link compute} function. Since
   * the graph may be large, it may be necessary to store partial graphs to
   * disk before producing the final graph.
   *
   * @throws IOException
   */
  @Override
  public void prepare() throws IOException {
    

    System.out.println("Preparing " + this.getClass().getName());
    String corpusDirectory = _options._corpusPrefix;
    File folder = new File(corpusDirectory);
    File[] all_documents2 = folder.listFiles();
    System.out.println("Listed files in dir: " + all_documents2.length);

    for (File fileEntry : new File(_options._corpusPrefix).listFiles()) {
        if (fileEntry.isFile()) {
        	try {
	        	String[] docWords = parseDocument(fileEntry);

	        	if(docWords.length <= 1) {
	        		//System.out.println(fileEntry.getName());
	        		continue;
	        	}

	        	all_documents.add(fileEntry);
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
        }
    }
    
    String testing = "";

    //here we are assuming that the corrupt version of a file(if any), occurs before its correct html version
    for (int i = 0; i < all_documents.size(); i++) {
      corpus_documents.put(all_documents.get(i).getName(), 1);
    }

    for (int i = 0; i < all_documents.size(); i++) {
    	testing = all_documents.get(i).getName();

        corpus_documents2.put(testing.toLowerCase(), all_documents_sgb.size());
        all_documents_sgb.add(all_documents.get(i));
    }

    System.out.println("Length of corpus: " + corpus_documents2.size());
    System.out.println("Length of corpus 2: " + all_documents_sgb.size());

    filtered_length=corpus_documents2.size();
    len = corpus_documents2.size();
    int trrr=0;
    //System.out.println("254: "+all_documents.get(254));
    System.out.println("Len equal to: " + len);
    for (Integer j = 0; j < len ; j++) {
      System.out.println("Starting on "+ j);
      corpus_graph.add(j);
      HeuristicLinkExtractor hle = new HeuristicLinkExtractor((all_documents_sgb.get(j)));
      String ss = null;
      ss = hle.getNextInCorpusLinkTarget();
      while (ss != null) {
        if(!ss.startsWith(".")) {
          File fCheck = new File(corpusDirectory + "/" + ss);
          File fCheck2 = new File(corpusDirectory + "/" + ss + ".html");
          if (fCheck2.exists()) {
            ss = ss + ".html";
            if(! corpus_graph.contains(corpus_documents2.get(ss.toLowerCase()))){
              corpus_graph.add(corpus_documents2.get(ss.toLowerCase()));
            }
            if (! corpus_graph.isEdge(j,corpus_documents2.get(ss.toLowerCase()))){
              if(! j.equals(corpus_documents2.get(ss.toLowerCase()))) {
                corpus_graph.add(j, corpus_documents2.get(ss.toLowerCase()));
              }

            }
          }

          else if (fCheck.exists() || fCheck2.exists()) {
            if(! corpus_graph.contains(corpus_documents2.get(ss.toLowerCase()))){
              corpus_graph.add(corpus_documents2.get(ss.toLowerCase()));
            }
            if (! corpus_graph.isEdge(j,corpus_documents2.get(ss.toLowerCase()))){
              if(! j.equals(corpus_documents2.get(ss.toLowerCase()))) {
                corpus_graph.add(j, corpus_documents2.get(ss.toLowerCase()));
              }
            }

          }

        }
        ss = hle.getNextInCorpusLinkTarget();
      }
      System.out.println("out degree: "+ j+": "+corpus_graph.no_of_outbound_nodes(j));
    }

    //adjusting redirects*************************************************************************************
    System.out.println("Starting to correct for redirects.");

    int docs_count = corpus_documents2.size();
    for(int j=0; j< docs_count; j++) {
      String doc_title = Jsoup.parse((all_documents_sgb.get(j)), "UTF-8").title();
      doc_title = doc_title.replaceAll(" - Wikipedia, the free encyclopedia","");
      doc_title = doc_title.replaceAll(" ","_");
      doc_title=doc_title.replaceAll(":","_");
      doc_title = doc_title.toLowerCase();
      if (!doc_title.equalsIgnoreCase(((all_documents_sgb.get(j)).getName())) && ! (doc_title+".html").equalsIgnoreCase((all_documents_sgb.get(j)).getName())) {
        if(corpus_documents2.containsKey(doc_title)){
          corpus_graph.mergeNodes_redirect_special(corpus_documents2.get(((all_documents_sgb.get(j)).getName()).toLowerCase()),corpus_documents2.get(doc_title));
        }
        else if(corpus_documents2.containsKey(doc_title+".html")){
          corpus_graph.mergeNodes_redirect_special(corpus_documents2.get(((all_documents_sgb.get(j)).getName()).toLowerCase()),corpus_documents2.get(doc_title+".html"));
        }
        else{
          corpus_documents2.put(doc_title,len);
          File file_for_redirects_not_present_before = new File(corpusDirectory+"/"+doc_title); //just for future reference
          all_documents_sgb.add(file_for_redirects_not_present_before);
          corpus_graph.add(corpus_documents2.get(doc_title));   //adding a new vetrex/node to the graph
          corpus_graph.mergeNodes_redirect_special(corpus_documents2.get(((all_documents_sgb.get(j)).getName()).toLowerCase()),corpus_documents2.get(doc_title));
        }

        len +=1;
      }
    }

  
    return;
  }

  /**
   * This function computes the PageRank based on the internal graph generated
   * by the {@link prepare} function, and stores the PageRank to be used for
   * ranking.
   *
   * Note that you will have to store the computed PageRank with each document
   * the same way you do the indexing for HW2. I.e., the PageRank information
   * becomes part of the index and can be used for ranking in serve mode. Thus,
   * you should store the whatever is needed inside the same directory as
   * specified by _indexPrefix inside {@link _options}.
   *
   * @throws IOException
   */
  @Override
  public void compute() throws IOException{

    System.out.println("Computing using " + this.getClass().getName());

    System.out.println("No of vertices/nodes/pages: " + corpus_graph.getNumberOfVertices());
    Map<Integer,Double> I_ini = new HashMap<>();
    //start with each page being equally likely

    List<Integer> set_of_vertices =corpus_graph.getVerticesSet();
    Integer P = corpus_graph.getNumberOfVertices();

    for(int k=0; k<P; k++){
      I_ini.put(set_of_vertices.get(k),1.0 / (corpus_graph.getNumberOfVertices()).doubleValue());
    }

    //each page has lambda/|P| chance of random selection
    for(int k=0; k<P; k++){
      R_Calculated1.put(set_of_vertices.get(k),lambda1 / (corpus_graph.getNumberOfVertices()).doubleValue());
    }

    //iteration 1
    for(int v=0; v<set_of_vertices.size();v++){
      List<Integer> Q = corpus_graph.outbound_nodes(set_of_vertices.get(v));
      if(Q.size() > 0 ) {
        for (int m = 0; m < Q.size(); m++) {
          R_Calculated1.replace(Q.get(m), R_Calculated1.get(Q.get(m)) + (((1.0 - lambda1)* I_ini.get(Q.get(m))) / Q.size()));
        }
      }
      else {
        for (int n = 0; n < Q.size(); n++) {
          R_Calculated1.replace(Q.get(n), R_Calculated1.get(Q.get(n)) + (((1.0 - lambda1)* I_ini.get(Q.get(n))) / P.doubleValue()));
        }
      }

      //I_ini.set(v,R_Calculated1.get(v)); //do this only for more than two iterations
    }

    //iteration 2
    for(int i=0; i<set_of_vertices.size(); i++){
      I_ini.replace(set_of_vertices.get(i), R_Calculated1.get(set_of_vertices.get(i)));
    }

    //iteration 2 begins
    for(int v=0; v<set_of_vertices.size();v++){
      List<Integer> Q = corpus_graph.outbound_nodes(set_of_vertices.get(v));

      if(Q.size() > 0 ) {
        for (int m = 0; m < Q.size(); m++) {
          R_Calculated1.replace(Q.get(m), R_Calculated1.get(Q.get(m)) + (((1.0 - lambda1)* I_ini.get(Q.get(m))) / Q.size()));
        }
      }
      else {
        for (int n = 0; n < Q.size(); n++) {
          R_Calculated1.replace(Q.get(n), R_Calculated1.get(Q.get(n)) + (((1.0 - lambda1)* I_ini.get(Q.get(n))) / P.doubleValue()));
        }
      }

    }





    int coun = 0;
    for(int h=0; h< corpus_graph.getNumberOfVertices(); h++){
      if(set_of_vertices.get(h) < 9960){
        //if(R_Calculated1.containsKey(set_of_vertices.get(h))){
        R_final.put(set_of_vertices.get(h),R_Calculated1.get(set_of_vertices.get(h)));
        coun +=1;
        //}
      }
    }
    for(int i=0; i<filtered_length; i++){
      if(! R_final.containsKey(i)){
        R_final.put(i,0.0);
      }
      R_final_toSave.add(R_final.get(i));
    }

    //using the values obtained after one iteration with lambda=0.1
    System.out.println("TEST");
    FileOutputStream fOut = new FileOutputStream(this._options._corpusPrefix.replace("data/", "") + "_pr_map.ser");
    ObjectOutputStream R_object = new ObjectOutputStream(fOut);
    R_object.writeObject(R_final_toSave);
    R_object.close();

    savingInTextFile();
    System.out.println("corpus size: "+corpus_documents.size());
    System.out.println("R_final_toSave: "+R_final_toSave.size());
    System.out.println("all docs size: "+all_documents.size());
    System.out.println("graph size: "+corpus_graph.getNumberOfVertices());


    System.out.println("number of valid page ranks: "+coun);
    return;
  }

  public void savingInTextFile(){
    File f = new File(this._options._corpusPrefix.replace("data/", "") + "_pagerank.txt");
    FileWriter fw = null;
    try{
      fw = new FileWriter(f,true);
    }catch (IOException e){
      e.printStackTrace();
    }
    BufferedWriter bw = new BufferedWriter(fw);
    StringBuilder data = new StringBuilder();

    //for(Map.Entry<Integer,Double> entry: R_final_toSave.entrySet()) {
    //  data.append(entry.getKey()+" "+entry.getValue()+"\n");
    //}

    for(int i=0; i< R_final_toSave.size(); i++){
      data.append(R_final_toSave.get(i)+"\n");
    }

    try{
      bw.write(data.toString());
      bw.close();
    } catch (IOException e){
      e.printStackTrace();
    }
  }


  /**
   * During indexing mode, this function loads the PageRank values computed
   * during mining mode to be used by the indexer.
   *
   * @throws IOException
   */
  @Override
  public Object load() throws IOException {
    System.out.println("Loading using " + this.getClass().getName());

    FileInputStream fis = new FileInputStream(this._options._corpusPrefix.replace("data/", "") + "_pr_map.ser");
    ObjectInputStream ois = new ObjectInputStream(fis);
    Vector<Double> map = null;
    try {
      map = (Vector<Double>) ois.readObject();
    } catch (Exception e) {
      e.printStackTrace();
    }
    ois.close();

    return map;
  }
}
