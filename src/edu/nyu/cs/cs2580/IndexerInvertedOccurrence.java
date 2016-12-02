package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.jsoup.Jsoup;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedOccurrence extends Indexer implements Serializable {

	private static final long serialVersionUID = 1L;
	private String _postings_list_dir = _options._indexPrefix + "/pl_occ/";

	//All Documents
	private ArrayList<DocumentIndexed> _docs;

	private HashMap<String, TermInfo> _dictionary; //Term to (ID, Doc Freq, Corp Freq)
	private ArrayList<String> sortByIndex;

	//Non-unique word count
	public Long temp_totalTermFrequency;

	//Sharding: TODO: tune
	private short shards = 1000;
	private int bufferSize = 4500000; //Flush buffer after size: Tune

	//cache
	private HashMap<Integer, int[]> _cache;
	private int cacheSize = 10000; //Number of terms in Cache

	public IndexerInvertedOccurrence(Options options) {
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
		
		temp_totalTermFrequency = 0L;
  
		//Create Index Directory
		File indexDirectory = new File(_postings_list_dir);
		if(indexDirectory .exists()) {
			//Delete all contents
			clearIndex(indexDirectory);
			System.out.println("Finished Clearing");
		} else {
			indexDirectory.mkdir();
			System.out.println("Created Directory");
		}
		
	  File corpusDirectory = new File(_options._corpusPrefix);

	  if(!corpusDirectory.exists() || !corpusDirectory.isDirectory())
		  throw new IllegalStateException("Corpus Directory must exist!");
	  
	  TermInfo currTermInfo = null;
	  
	  //Estimation
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
	        	
	        	//Update map
	        	for(String word: docWords) {
	        		if(!_dictionary.containsKey(word)) {//unique word
	        			int temp_term_id = _dictionary.size();
	        			_dictionary.put(word, new TermInfo(temp_term_id, doc_id));
	        			sortByIndex.add(word);
	        			bufferEst += (2 * Integer.BYTES); //each new docid + count + corpFreq
	        		} else { //update term/doc count
	        			currTermInfo = _dictionary.get(word);
	        			if(doc_id != currTermInfo.lastDocId) {
	        				currTermInfo.lastDocId = doc_id;
	        				currTermInfo.docFreq++;
	        	        	bufferEst += (2 * Integer.BYTES); //each new docid + count
	        			}
	        			currTermInfo.corpFreq++;
	        		}
	        		bufferEst += Integer.BYTES; //each occurence
	        	}
	        }
	  }
	  System.out.println("Test");
	  
	  //Sort Ids by size
	  Collections.sort(sortByIndex, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return (_dictionary.get(o2).docFreq + _dictionary.get(o2).corpFreq) 
						- (_dictionary.get(o1).docFreq + _dictionary.get(o1).corpFreq);
			}
	  });
	  
	  //Readjust/sum estimates, create files, create threshold
	  long runningSum = 0;
	  RandomAccessFile raIndFile = null;
	  TermInfo updateTerm;
	  int shardFile = 0;
	  bufferEst = bufferEst / shards;
	  String newIndexFile;
	  
	  System.out.println(bufferEst + " " + _docs.size());
	  
	  for(int index = 0; index < sortByIndex.size(); index++) {
		  updateTerm = _dictionary.get(sortByIndex.get(index));
		  
		  updateTerm.lastDocId = -1;
		  
		  //Update Ids, threshold, and estimates/lastbyte
		  updateTerm.id = index;
		  long termEst = ((updateTerm.docFreq * 2) + updateTerm.corpFreq) * (Integer.BYTES); 
		  //(DocId + Occurence) + Doc Occurences
		  
		  //Dynamic Sharding
		  //If runningSum is above threshold, create new index file
		  if(Math.abs(bufferEst - runningSum) < Math.abs(bufferEst - runningSum - termEst)) { 
			  newIndexFile = _postings_list_dir + Integer.toString(shardFile);
			  new File(newIndexFile).createNewFile();
			  raIndFile = new RandomAccessFile( newIndexFile , "rw");
			  raIndFile.setLength(runningSum);
			  raIndFile.close();
			  
			  shardFile++; 
			  /*if(shardFile >= shards) {
				  System.out.println("Dynamic Sharding not working");
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
	  HashMap<Integer, List<Integer>> docBuffer;
	  int flag = 0;
    
	  //Build postings lists: Doc1 Count Occ1 Occ2 Doc2 Count...
	  //Shard based on counts
	  //Go through each file and flush when max memory is full (Buffer)
	  for (DocumentIndexed doc: _docs) {
		  	File fileEntry = new File(doc.fileName);
		  	int doc_id = doc._docid;
		  	System.out.println("Parsed: " + doc_id);

		  	docBuffer = new HashMap<>();
		  	
	      	//Update treemap
		  	int wordIndex = 0;
	      	for(String word: parseDocument(fileEntry)) {
		        //Append to Doc buffer
      			int termId = _dictionary.get(word).id;
      			
      			if(!docBuffer.containsKey(termId)) 
      				docBuffer.put(termId, new ArrayList<>());	
		        
      			docBuffer.get(termId).add(wordIndex);
      			
      			flag++;
      			wordIndex++;
	      	}
	      	
	      	//Update actual buffer
	      	for(Integer termId: docBuffer.keySet()) {
     			if(!buffer.containsKey(termId)) 
      				buffer.put(termId, new ArrayList<>());	
     			
     			buffer.get(termId).add(doc_id);
     			buffer.get(termId).add(docBuffer.get(termId).size());
     			buffer.get(termId).addAll(docBuffer.get(termId));
	      	}
	      	
	      	//Check if full after each word, if full, then flush
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
	  	//System.out.println(documentTermFrequency("50 Cent", 0));
	  	//System.out.println(Arrays.toString(loadPostingFile("the")));
	  	//System.out.println(Arrays.toString(loadPostingFile("hills")));
	  	//System.out.println(Arrays.toString(next("when the sun hits the hills just right", 1)));
	  	//System.out.println(Arrays.toString(loadPostingFile("right", -1)));
	  	//System.out.println(next("the", 100)._docid);
	  	//System.out.println(next("the", -1)._docid);
	  	//System.out.println(next("the", 266)._docid);
	  	//System.out.println(next("the", 267));
	  
	  //Store serialized versions of _docs, _dictionary, _threshold, _totalTermFrequency
	  String indexFileName = _options._indexPrefix + "/occurrence.idx";
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
		System.out.println("Start Flushing");
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
	public int[] loadPostingFile(String term) {
		if(!_dictionary.containsKey(term))
			return null;
		
		int term_id = _dictionary.get(term).id;
		
		if(_cache.containsKey(term_id))
			return _cache.get(term_id);
		
		manageCache();
		  
		TermInfo retrieveTerm = _dictionary.get(term);
		  
		int size = (int) ((retrieveTerm.lastbyte - retrieveTerm.start) / Integer.BYTES);
		int[] postingList = new int[size];
		  
		
		try {
			RandomAccessFile raf = new RandomAccessFile(_postings_list_dir + Integer.toString(retrieveTerm.file), "r");

			//find bounds
			int posL = 0;
			for(long pos = retrieveTerm.start; pos < retrieveTerm.lastbyte; pos += Integer.BYTES) {
				raf.seek(pos);
				postingList[posL] = raf.readInt();
				posL++;
			}
			  
			raf.close();
			_cache.put(term_id, postingList);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  
		return postingList;
	}
	
	@Override
	public void loadIndex() throws IOException, ClassNotFoundException {
		//Serialize DS: 
		String indexFileName = _options._indexPrefix + "/occurrence.idx";
		System.out.println("Load index from: " + indexFileName);
	
		ObjectInputStream reader = new ObjectInputStream(new FileInputStream(indexFileName));
		IndexerInvertedOccurrence loaded = (IndexerInvertedOccurrence) reader.readObject();
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
  
	  //Manage cache, remove term with lowest size of terms
	  //TODO: Optimize by removing # terms from cache
	  private void manageCache() {
		  if(_cache.size() > cacheSize) {
			  int lowestsize = Integer.MAX_VALUE;
			  Integer removeKey = null;
			  for(Integer key: _cache.keySet()) {
				  if(lowestsize > _cache.get(key).length) {
					  lowestsize = _cache.get(key).length;
					  removeKey = key;
				  }
			  }
			  _cache.remove(removeKey);
		  }
	  }

	  //Recursive form of next Doc
	  public Document nextDoc(Query query, int docid) {
	    boolean isSame = true;
	    int maxDocId = -1;
	
	    for(String term : query._tokens) {
	      int[] doc = next(term, docid);
	
	      if(doc == null || doc[0] > _docs.size())
	        return null;
	
	      if(maxDocId == -1) //First Assignment
	        maxDocId = doc[0];
	
	      //If not equal to max Doc Id, not the same
	      if(maxDocId != doc[0]) {
	        isSame = false;
	        maxDocId = Math.max(doc[0], maxDocId);
	      }
	    }
	
	    //If all the same return
	    if(isSame)
	      return _docs.get(maxDocId);
	
	    //recurse
	    return nextDoc(query, maxDocId - 1);
	  }

  //TODO: Work on Heavily: Linear
  public int[] next(String term, int docId) {
    int[] nextDoc;
    String[] phrase = term.split("\\s+");

    if(phrase.length > 1) {
      nextDoc = new int[phrase.length + 1];
    } else {
      nextDoc = new int[2];
    }

    int thresholdDocId = docId;

    boolean isSame = false;
    boolean inOrder = false;

    while(!isSame || !inOrder) {
      int maxDocId = -1;
      isSame = true;
      inOrder = false;

      for(int termPos = 0 ; termPos < phrase.length; termPos++) {
        int[] posting_list = loadPostingFile(phrase[termPos]);

        if(posting_list == null) {
          return null;
        }

        //Linear Scan
        while(posting_list[nextDoc[termPos + 1]] <= thresholdDocId) {
          nextDoc[termPos + 1] += 2 + posting_list[nextDoc[termPos + 1] + 1];
          if(nextDoc[termPos + 1] >= posting_list.length) {
            return null;
          }
        }

        nextDoc[0] = posting_list[nextDoc[termPos + 1]];

        if(maxDocId == -1) {
          maxDocId = nextDoc[0];
        } else if(maxDocId != nextDoc[0]) {
          isSame = false;
          maxDocId = Math.max(nextDoc[0], maxDocId);
        }
      }
      
      thresholdDocId = maxDocId - 1;

      if(isSame){
        if(inOrder(term, nextDoc))
          inOrder = true;
        else
          thresholdDocId++;
      }
  
    }

    return nextDoc;
  }

  //Bounded Binary Search: Because words are less frequent
  public boolean inOrder( String phrase, int[] docPos ) {
    if(!phrase.contains(" ")) //Just a single term
      return loadPostingFile(phrase)[docPos[1] + 1] > 0;
      
    return documentPhraseFrequency(phrase, docPos, false) > 0;
  }

  //Binary Search in Occurrences
  public int nextOcc(int[] occurrences, int threshold , int low, int high) {
	  	//Binary search over postinglist
	  	int mid;

		while(low < high) {
		  mid = (low + high) / 2;
		  if(occurrences[mid] <= threshold) {
		    low = mid + 1;
		  } else {
		    high = mid;
		  }
		}
		
		//Cannot go higher
		if(occurrences[high] <= threshold)
		  return -1;
		
		if(occurrences[low] > threshold) {
		  return low;
		}
		
		return high;
  }

  @Override
  public int corpusTermFrequency(String term) {
    if(term.contains(" ")){ //Work on Phrase
      int count = 0;
      int[] doc = new int[]{-1, -1};
     
	  while((doc = next(term, doc[0])) != null) {
	    count += documentPhraseFrequency(term, doc, true);
	  }
      
      return count;
    }

    return _dictionary.get(term).corpFreq;
  }

  @Override
  public int corpusDocFrequencyByTerm(String term) {
    if(term.contains(" ")){ //Work on Phrase
	      int count = 0;
	      int[] doc = new int[]{-1, -1};
	
	      while((doc = next(term, doc[0])) != null) {
	    	  if(documentPhraseFrequency(term, doc, false) > 0)
	    		  count++;
		  }
	
	      return count;
    }

    return _dictionary.get(term).docFreq;
  }

  @Override
  public int documentTermFrequency(String term, int docid) {
    int[] doc = next(term, docid - 1);

    //docid does not contain term frequency
    if(doc[0] != docid)
      return 0;

    //Not Phrase
    if (!(term.contains(" "))) {
      int[] postings_list = loadPostingFile(term);

      if (postings_list == null)
        return 0;

      return postings_list[doc[1] + 1];
    } else { // else it is a phrase
      return documentPhraseFrequency(term, doc, true);
    }
  }

  //Get Document Term Frequency given a document/pos in postings list and term
  public int documentPhraseFrequency(String term, int[] doc, boolean all) {
    String[] phrase = term.split("\\s+");

    //Get Starting Index
    int[] docPos = new int[phrase.length];
    System.arraycopy(doc, 1, docPos, 0, phrase.length);

    //Get Ending Positions
    int[] end = new int[docPos.length];
    for(int termPos = 0; termPos < phrase.length; termPos++) {
      int[] postingsList = loadPostingFile(phrase[termPos]);
      end[termPos] = docPos[termPos] + 1 + postingsList[docPos[termPos] + 1];

      //Start at the first Occurrence
      docPos[termPos] += 2;
    }

    //Last Called Position: 
    int[] lastPos = new int[phrase.length]; //make into hashmap
    System.arraycopy(docPos, 0, lastPos, 0, docPos.length);

    int maxPos = 0;
    int count = 0;

    int startIndex = docPos[0];
    
    //Until First Word's Terms are Exhausted
    while(startIndex != -1) {
      boolean flag = true;

      int firstWordOcc = loadPostingFile(phrase[0])[startIndex];
      maxPos = firstWordOcc + 1; //So it won't pick the same position again

      HashMap<String, Integer> lastSeen = new HashMap<>();
      
      //Go through Remaining Terms
      for(int termPos = 1; termPos < phrase.length; termPos++) { //remaining Terms
			//Get Next Occurrence or term Greater than that of the first term
			//Handle duplicates
			int termOccIndex = 0;
			if(lastSeen.containsKey(phrase[termPos])) {
				termOccIndex = lastSeen.get(phrase[termPos]) + 1;
			} else {
				termOccIndex = nextOcc(loadPostingFile(phrase[termPos]),
			            firstWordOcc, lastPos[termPos], end[termPos]);
				lastSeen.put(phrase[termPos], termOccIndex);
			}
        
	        if(termOccIndex == -1 || termOccIndex > end[termPos]) {
	          return count;
	        }
	
	        lastPos[termPos] = termOccIndex; //Update last Call
	
	        //Get the actual Occurrence
	        int nextWordOcc = loadPostingFile(phrase[termPos])[termOccIndex];
	
	        maxPos = Math.max(nextWordOcc - termPos, maxPos);
	
	        //nextWordOcc needs to be exactly termPos greater
	        if(nextWordOcc - firstWordOcc != termPos) {
	          flag = false;
	          break;
	        }
      }

      //If entire phrase works
      if(flag) {
        count++;
        //If just need to know phrase exists
        if(!all)
          return count;
        maxPos++;
      }

      //Fix Find new occurrence
      startIndex = nextOcc( loadPostingFile(phrase[0]),
              maxPos - 1, lastPos[0], end[0]);
      lastPos[0] = startIndex;
    }

    return count;
  }
}