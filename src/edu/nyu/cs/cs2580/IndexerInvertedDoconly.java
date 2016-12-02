package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.jsoup.Jsoup;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedDoconly extends Indexer implements Serializable {

	private static final long serialVersionUID = 1L;
	private String _postings_list_dir = _options._indexPrefix + "/pl_doc_only/";
  
	//All Documents
	private ArrayList<DocumentIndexed> _docs;
  
	private HashMap<String, TermInfo> _dictionary; //Term to (ID, Doc Freq, Corp Freq)
	private ArrayList<String> sortByIndex;
  
	//Non-unique word count
	public Long temp_totalTermFrequency = 0L;
  
	//Sharding: TODO: tune
	private short shards = 1000;
	private int bufferSize = 5000000; //Flush buffer after size: Tune
  
	//cache
	private HashMap<String, int[]> _cache;
	private int cacheSize = 10000; //Number of terms in Cache
  
	public IndexerInvertedDoconly(Options options) {
		super(options);
		System.out.println("Using Indexer: " + this.getClass().getSimpleName());
	  }
  
	  public static void clearIndex(File folder) {
		    File[] files = folder.listFiles();
		    if(files!=null) { //some JVMs return null for empty dirs
		        for(File f: files) {
		             f.delete();
		        }
		    }
		}
	
	  @Override
	  public void constructIndex() throws Exception {
		    //Instantiate Docs
		    _docs = new ArrayList<>();
		    _dictionary = new HashMap<>();
		    sortByIndex = new ArrayList<>();
		    
			//Create Index Directory
			File indexDirectory = new File(_postings_list_dir);
			if(indexDirectory .exists()) {
				//Delete all contents
				clearIndex(indexDirectory);
				System.out.println("Finished Clearing");
			} else
				indexDirectory.mkdir();
			
		  File corpusDirectory = new File(_options._corpusPrefix);
	
		  if(!corpusDirectory.exists() || !corpusDirectory.isDirectory())
			  throw new IllegalStateException("Corpus Directory must exist!");
		  
		  TermInfo currTermInfo = null;
		  
		  long bufferEst = 0;
		  
		  //Build Documents + get all unique words
		  //Estimate byte size
		  for (File fileEntry : corpusDirectory.listFiles()) {
		        if (fileEntry.isFile()) {
		        	//Parse/Build Document
	  	        	String[] docWords = parseDocument(fileEntry);
	  	        	
	  	        	if(docWords.length <= 1)
	  	        		continue;
	  	        	
	  	        	//Parse/Build Document
	  	        	int doc_id = _docs.size();
		        	
		        	//Update corpus count
		        	temp_totalTermFrequency += docWords.length;
		        	
		        	_docs.add(buildDocument(fileEntry, doc_id, docWords.length));
		        	
		        	//Update treemap
		        	for(String word: docWords) {
		        		if(!_dictionary.containsKey(word)) {//unique word
		        			int temp_term_id = _dictionary.size();
		        			_dictionary.put(word, new TermInfo(temp_term_id, doc_id));
		        			sortByIndex.add(word);
			        		bufferEst += Integer.BYTES;
		        		} else { //update term/doc count
		        			currTermInfo = _dictionary.get(word);
		        			if(doc_id != currTermInfo.lastDocId) {
		        				currTermInfo.lastDocId = doc_id;
		        				currTermInfo.docFreq++;
				        		bufferEst += Integer.BYTES;
		        			}
		        			currTermInfo.corpFreq++;
		        		}
		        	}
		        }
		  }
		  
		  //Sort Ids by size
		  Collections.sort(sortByIndex, new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					return _dictionary.get(o2).docFreq - _dictionary.get(o1).docFreq;
				}
		  });
		  
		  System.out.println(bufferEst + " " + _docs.size() + " " + _dictionary.size());
		  
		  //Readjust/sum estimates, create files, create threshold
		  long runningSum = 0;
		  RandomAccessFile raIndFile = null;
		  TermInfo updateTerm;
		  int shardFile = 0;
		  bufferEst = bufferEst / shards;
		  String newIndexFile;
		  
		  System.out.println(bufferEst + " " + _docs.size() + " " + _dictionary.size());
		  
		  for(int index = 0; index < sortByIndex.size(); index++) {
			  updateTerm = _dictionary.get(sortByIndex.get(index));
			  
			  updateTerm.lastDocId = -1;
			  
			  //Update Ids, threshold, and estimates/lastbyte
			  updateTerm.id = index;
			  long termEst = updateTerm.docFreq * (Integer.BYTES); //DocId
			  
			  //Dynamic Sharding
			  //If runningSum is above threshold, create new index file
			  if(Math.abs(bufferEst - runningSum) < Math.abs(bufferEst - runningSum - termEst)) { 
				  newIndexFile = _postings_list_dir + Integer.toString(shardFile);
				  new File(newIndexFile).createNewFile();
				  raIndFile = new RandomAccessFile( newIndexFile , "rw");
				  raIndFile.setLength(runningSum);
				  raIndFile.close();
				  
				  shardFile++; //Last word that fits in document
				  /*if(shardFile >= shards) {
					  System.out.println("Dynamic Sharding exceeded" + index);
					  return;
				  }*/
				  
				  runningSum = 0;
			  }
	
			  updateTerm.lastbyte = runningSum;
			  if(updateTerm.start < 0) { //not set
				  updateTerm.start = runningSum;	 
			  }
			  updateTerm.file = shardFile;
			  
			  runningSum += termEst;
		  }
		  
		  //garbage collection will free more space hopefully
		  raIndFile = null;
		  updateTerm = null;
		  
		  TreeMap<Integer, List<Integer>> buffer = new TreeMap<>();
		  int flag = 0;
	      
		  //Build postings lists: Id Doc1 Doc2 ...
		  //Shard based on counts
		  //Go through each file and flush when max memory is full (Buffer)
		  for (DocumentIndexed doc: _docs) {
			  	File fileEntry = new File(doc.fileName);
			  	int doc_id = doc._docid;
			  	System.out.println("Parsed: " + doc_id);
	
	        	//Update treemap
	        	for(String word: parseDocument(fileEntry)) {
		            //Append to buffer
	        		int lastDocForWord = _dictionary.get(word).lastDocId;
	
	        		if(doc_id != lastDocForWord) {
	        			int termId = _dictionary.get(word).id;
	        			if(!buffer.containsKey(termId)) 
	        				buffer.put(termId, new ArrayList<>());
		        		
		        		//TODO: figure out num_occurences: Buffer to Buffer
		        		
			        	//Check if full after each word, if full, then flush
	        			buffer.get(termId).add(doc_id);
	        			_dictionary.get(word).lastDocId = doc_id;
	        			flag++;
	        		}
	        	}
	        	
	        	if(flag > bufferSize) {
	        		flush(buffer);
	        		flag = 0;
	        	}
		  }
		  
		  	flush(buffer);
		  
		  	//Clear Buffer
		  	buffer = null;
		  	
		  	System.out.println("Finished");
		  	
		  	//TESTING: Load Index
		  	//_cache = new HashMap<>();
		  	//System.out.println(Arrays.toString(loadPostingFile("the", -1)));
		  	//System.out.println(next("the", 100)._docid);
		  	//System.out.println(next("the", -1)._docid);
		  	//System.out.println(next("the", 266)._docid);
		  	//System.out.println(next("the", 267));
		  
		  	//Store serialized versions of _docs, _dictionary, _threshold, _totalTermFrequency
			String indexFileName = _options._indexPrefix + "/doconly.idx";
			File indexFile = new File(indexFileName);
			if(indexFile.exists())
				indexFile.delete();
			indexFile.createNewFile();
			
			System.out.println("Store index to: " + indexFileName);
			ObjectOutputStream writer =
			        new ObjectOutputStream(new FileOutputStream(indexFileName));
			writer.writeObject(this);
			writer.close();
	  }
	
	  private String[] parseDocument(File doc) throws Exception {
		  return cleanDocument(Jsoup.parse(doc, "UTF-8").text()).split("\\s+");
	  }
	  
	  private DocumentIndexed buildDocument(File doc, int doc_id, int count) throws Exception {
		    DocumentIndexed document = new DocumentIndexed(doc_id, count);
		    
			String doc_title = Jsoup.parse(doc, "UTF-8").title();
			document.setTitle(doc_title);
			document.setUrl("https://en.wikipedia.org/wiki/" + doc.getName());
			
			//Set FileName
			document.fileName = doc.getAbsolutePath();
			
		    return document;
	  }
	
	  private String cleanDocument(String document_body) {
	    return document_body.replaceAll("[^A-Za-z0-9\\s]", "").toLowerCase();
	  }
	  
	  //Focus on flush contents
	  private void flush(TreeMap<Integer, List<Integer>> buffer) throws IOException {
		  TermInfo updateTerm;
		  RandomAccessFile raIndFile = null;
		  
		  int lastFile = -1;
	
		  for(Entry<Integer, List<Integer>> termPostingList: buffer.entrySet()) {
			  updateTerm = _dictionary.get(sortByIndex.get(termPostingList.getKey()));
			  
			  //Get File
			  if(lastFile != updateTerm.file) {
				  raIndFile = new RandomAccessFile(_postings_list_dir + Integer.toString(updateTerm.file), "rw");
				  lastFile = updateTerm.file;
			  }
			  
			  //Jump to Last Byte
			  for(Integer docId: termPostingList.getValue()) {
				  raIndFile.seek(updateTerm.lastbyte);
				  raIndFile.writeInt(docId);
				  updateTerm.lastbyte += Integer.BYTES;
			  }
		  }
		  
		  //Clean Buffer
		  buffer.clear();
		  System.out.println("Flushed");
	  }
	  
	  //Individual Term load
	  public int[] loadPostingFile(String term, int doc_id) throws IOException {
		  if(!_dictionary.containsKey(term))
			  return null;
		  
		  TermInfo retrieveTerm = _dictionary.get(term);
		  
		  int size = (int) ((retrieveTerm.lastbyte - retrieveTerm.start) / Integer.BYTES);
		  int[] postingList = new int[size];
		  
		  RandomAccessFile raf = new RandomAccessFile(_postings_list_dir + Integer.toString(retrieveTerm.file), "r");
		  
		  //find bounds
		  int posL = 0;
		  for(long pos = retrieveTerm.start; pos < retrieveTerm.lastbyte; pos += Integer.BYTES) {
			  raf.seek(pos);
			  postingList[posL] = raf.readInt();
			  posL++;
		  }
		  
		  raf.close();
		  
		  return postingList;
	  }
	
	  @Override
	  public void loadIndex() throws IOException, ClassNotFoundException {
		  	//Serialize DS: 
		  	String indexFileName = _options._indexPrefix + "/doconly.idx";
		    System.out.println("Load index from: " + indexFileName);
	
		    ObjectInputStream reader = new ObjectInputStream(new FileInputStream(indexFileName));
		    IndexerInvertedDoconly loaded = (IndexerInvertedDoconly) reader.readObject();
		    reader.close();
		    
		    // Key DataStructures
		    this._docs = loaded._docs;
		    this._dictionary = loaded._dictionary; 
		    this.sortByIndex = loaded.sortByIndex;
		    
		    //Non-unique word count
		    this._totalTermFrequency = loaded.temp_totalTermFrequency;
	
		    System.out.println(Integer.toString(_docs.size()) + " documents loaded " +
		            "with " + Long.toString(_dictionary.size()) + " terms!");
	
		    //Garbage Collection drop loaded object
		    loaded = null;
		    
		    //TODO: Load whatever I can into cache
		    _cache = new HashMap<>();
	  }
	
	  @Override
	  public Document getDoc(int docid) {
	    return _docs.get(docid);
	  }
	
	  /**
	   * In HW2, you should be using {@link DocumentIndexed}
	   */
	  @Override
	  public Document nextDoc(Query query, int docid) {
		  if(query._tokens.size() > 1)
			  SearchEngine.Check(false, "Not implemented for phrases.");
	      return next(query._tokens.get(0), docid);
	  }
	
	  // Grab next document from postings list for a term
	  public Document next(String term, int docid) {
		  if(!_dictionary.containsKey(term))
			  return null;
		  
		  int[] postinglist;
		  
		  if(_cache.containsKey(term)){
			  postinglist = _cache.get(term);
		  } else {
			  try {
				  manageCache();
				  postinglist = loadPostingFile(term, docid);
				  _cache.put(term, postinglist);
			  } catch (IOException e) {
				  e.printStackTrace();
				  return null;
			  }
		  }
		  
		  //Binary search over postinglist
		  int low = 0;
		  int high = postinglist.length - 1;
		  int mid;
		  
		  while (low < high) {
		        mid = (low + high) / 2;
		        if(docid < postinglist[mid])
		        	high = mid;
		        else if (docid >= postinglist[mid])
		        	low = mid + 1;
		  }
		 
		  //If highest document is still lower just stop
		  if(postinglist[high] <= docid) {
			  return null;
		  }
		  
		  return getDoc(postinglist[high]);
	  }
	  
	  //Manage cache, remove term with lowest size of terms
	  private void manageCache() {
		  if(_cache.size() + 1 > cacheSize) {
			  int lowestsize = Integer.MAX_VALUE;
			  String removeKey = null;
			  for(String key: _cache.keySet()) {
				  if(lowestsize > _cache.get(key).length) {
					  lowestsize = _cache.get(key).length;
					  removeKey = key;
				  }
			  }
			  _cache.remove(removeKey);
		  }
	  }
	
	  @Override
	  public int corpusDocFrequencyByTerm(String term) {
		  return _dictionary.get(term).corpFreq;
	  }
	
	  @Override
	  public int corpusTermFrequency(String term) {
		  //load document/line and read second character
		  return _dictionary.get(term).docFreq;
	  }
	
	  @Override
	  public int documentTermFrequency(String term, int docid) {
	    SearchEngine.Check(false, "Not implemented!");
	    return 0;
	  }
}