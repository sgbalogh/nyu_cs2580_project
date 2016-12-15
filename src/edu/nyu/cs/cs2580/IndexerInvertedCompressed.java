package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends Indexer implements Serializable {

	private static final long serialVersionUID = 1L;
	private String _postings_list_dir = _options._indexPrefix + "/pl_compressed/";

	//All Documents
	private ArrayList<DocumentIndexed> _docs;

	private HashMap<String, TermInfo> _dictionary; //Term to (ID, Doc Freq, Corp Freq)
	private HashMap<Integer, Integer> _geoDocsOccurred; //GeoID => Inverse Count
	private ArrayList<String> sortByIndex;

	private Long temp_totalTermFrequency = 0L;
	private double temp_avgDocLength = 0.0;
	
	//Sharding: Can Tune
	private short shards = 1000;
	private int bufferSize = 4500000; //Flush buffer after size: Tune

	//cache
	private HashMap<Integer, PostingSkipTuple> _posting_cache;
	private HashMap<String, Integer> _doccount_cache;
	private HashMap<String, Integer> _termcount_cache; //TODO

	private int cacheSize = 1000000; //Number of terms in Cache

	//Clear out a Folder
	public static void clearIndex(File folder) {
		if(folder.exists()) {
			File[] files = folder.listFiles();
			if(files!=null) { //some JVMs return null for empty dirs
				for(File f: files) {
					f.delete();
				}
			}
		}
	}
	
	public IndexerInvertedCompressed(Options options) {
		super(options);
		System.out.println("Using Indexer: " + this.getClass().getSimpleName());
	}

	private long vbyteEstimate(int value) {
		return ((32 - Integer.numberOfLeadingZeros(value) - 1) / 7) + 1;
	}

	//Encode value and add to postinglist
	private void updateVbyteEncode( List<Byte> postingList, Integer value) {
		int temp_value = value;
		byte[] vbyteEncoded = new byte[5];
		boolean stop = true;
		for(int i = 4; i >= 0; i--) {
			byte buffer = (byte)(temp_value & 0x7F);
			temp_value = temp_value >> 7;

			if(stop) {
				buffer |= 0x80; //Set first bit
				stop = false;
			}

			vbyteEncoded[i] = buffer;
		}

		//append without heading zeros
		stop = false;
		for(byte val: vbyteEncoded) {
			if(val != 0 || stop) {
				postingList.add(val);
				stop = true;
			}
		}
	}

	private void loadStopWords(HashSet<String> stopWords, String stopWordsList) {
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

	//Get all files in a nested Data Structure
	//TODO: Deal with duplicates in Titles
	private List<File> nestedFiles(File corpusDirectory) {
		List<File> files = new LinkedList<>();
		for(File en : corpusDirectory.listFiles()) {
			if(en.isDirectory()) {
				files.addAll(nestedFiles(en));
			} else if(en.isFile() && !en.isHidden() && !en.getName().startsWith(".")){
				files.add(en);
			}
		}
		return files;
	}

	@Override
	public void constructIndex() {
	}
	
	public void constructIndex(SpatialEntityKnowledgeBase gkb) throws Exception {
		//=================================Instantiate Docs

		_docs = new ArrayList<>();
		_dictionary = new HashMap<>();
		_geoDocsOccurred = new HashMap<>();
		sortByIndex = new ArrayList<>();

		_totalTermFrequency = 0L;

		//===================================Create PageRank

		//Page Rank
		//System.out.println("Loading pagerank scores...");
		//CorpusAnalyzerPagerank pageranker = (CorpusAnalyzerPagerank) this._corpusAnalyzer;
		//@SuppressWarnings("unchecked")
		//Vector<Double> _doc_pagerank = (Vector<Double>) pageranker.load();

		//==================================Create Posting List and skip Pointers Directory

		File postingListDirectory = new File(_postings_list_dir);

		if(postingListDirectory .exists()) {
			//Delete all contents
			clearIndex(postingListDirectory);
			System.out.println("Finished Cleaning Index Directory(s)");
		} else {
			postingListDirectory.mkdirs();
		}

		//====================================Load StopWords + Stemmer

		HashSet<String> stopWords = new HashSet<>();
		loadStopWords(stopWords, "english.stop.txt");

		//Stemmer stemmer = new Stemmer();

		//============================================================= Load Corpus Directory

		File corpusDirectory = new File(_options._corpusPrefix);
		System.out.println("Corpus Directory: " + _options._corpusPrefix);

		if(!corpusDirectory.exists() || !corpusDirectory.isDirectory())
			throw new IllegalStateException("Corpus Directory must exist!");


		//============================================================== Phase 1: Estimate Posting List Size

		System.out.println("Estimation/Geo-Ids Phase");

		TermInfo currTermInfo = null;
		HashMap<String, Integer> wordCount = null;
		HashMap<Integer, Integer> geoCount = null;
		
		//=============================Geo List File Initiator
		String geoListsName = _postings_list_dir + "geoIds";
		File geoListsFile = new File(geoListsName);
		if(!geoListsFile.exists()) {
			System.out.println("Cleaned GeoList");
			geoListsFile.delete();
		}
		geoListsFile.createNewFile();
		
		List<Byte> geoList = new LinkedList<Byte>();
		//====================================================
			
		//Estimation
		long bufferEst = 0;
		long runningGeoSum = 0;
		int progress = 0;

		//Go through all Files in Corpus
		for (File fileEntry : nestedFiles(corpusDirectory)) { //corpusDirectory.listFiles()) {
			if (fileEntry.isFile()) {
				//TODO: Ignore Articles with the same titles

				String[] docWords = parseDocument(fileEntry);

				//Skip Corrupted Files
				if(docWords.length <= 1)
					continue;
				
				System.out.println("Estimated : " + progress);
				progress++;

				//Parse/Build Document
				int doc_id = _docs.size();

				//Update corpus count
				temp_totalTermFrequency += docWords.length;

				//TODO: actually deal with pagerank
				DocumentIndexed currDoc = buildDocument(fileEntry, doc_id, docWords.length, 0, 0 );
				_docs.add(currDoc);
				temp_avgDocLength += docWords.length;

				wordCount = new HashMap<>();
				geoCount = new HashMap<>();
				
				//Trigram Buffer
				LinkedList<String> trigramBuffer = new LinkedList<>();

				//Update map
				int occurrence = 0;
				for(String word: docWords) {
					//Stopwords
					if(stopWords.contains(word)) {
						continue;
					}
					
					//============================= Update Location count
					//Update Trigram buffer
					trigramBuffer.add(word);
					if(trigramBuffer.size() > 3)
						trigramBuffer.remove();
					
					String term = null;
					for(String gram : trigramBuffer) {
						if(term == null)
							term = gram;
						else
							term += " " + gram;
						for(GeoEntity ge : gkb.getCandidates(term)) {
							if(geoCount.containsKey(ge.getId()))
								geoCount.put(ge.getId(), geoCount.get(ge.getId()) + 1);
							else
								geoCount.put(ge.getId(), 1);
						}
					}
					//==============================================
					
					//Stem
					//stemmer.add(word.toCharArray(),word.length());
					//stemmer.stem();
					//word = stemmer.toString();

					if(!_dictionary.containsKey(word)) {
						//unique word add to Dictionary
						int temp_term_id = _dictionary.size();
						_dictionary.put(word, new TermInfo(temp_term_id, doc_id));
						sortByIndex.add(word);

						_dictionary.get(word).estimatePosting += vbyteEstimate(occurrence); //+ corpFreq
					} else {
						//update term/doc count
						currTermInfo = _dictionary.get(word);
						currTermInfo.corpFreq++;
						currTermInfo.estimatePosting += vbyteEstimate(occurrence); //+ corpFreq
					}

					//Every Occurrence
					bufferEst += vbyteEstimate(occurrence); //+ corpFreq
					occurrence++;

					//Update Word Count
					if(!wordCount.containsKey(word)) {
						wordCount.put(word, 1);
					} else {
						wordCount.put(word, wordCount.get(word) + 1);
					}
				}
				
				//================================== Update Folder
				currDoc.startingBit = runningGeoSum;
				currDoc.endingBit = runningGeoSum;
				
				Iterator<Entry<Integer, Integer>> iter = geoCount.entrySet().iterator();
				while(iter.hasNext()) {
					if(iter.next().getValue() < 3) //Don't really consider any locations that occurred less than 3 times
						iter.remove();
				}
				
				for(Entry<Integer, Integer> gc: geoCount.entrySet()) {
					//If it's state does not also exist in the document, skip
					GeoEntity ge = gkb.getDefinedLocation(gc.getKey());

					if((ge.type.equals("CITY") &&  !geoCount.containsKey(ge.parent.parent.getId())) ||
							(ge.type.equals("COUNTY") && !geoCount.containsKey(ge.parent.getId())))
						continue;
					
					//Update VByte Encoded List
					updateVbyteEncode( geoList, gc.getKey());
					updateVbyteEncode( geoList, gc.getValue());
					
					//Update ScoredDoc
					long estimate = vbyteEstimate(gc.getKey()) + vbyteEstimate(gc.getValue());
					currDoc.endingBit += estimate;
					runningGeoSum += estimate;
					
					//Update Global Count
					if(_geoDocsOccurred.containsKey(gc.getKey())) {
						_geoDocsOccurred.put(gc.getKey(), _geoDocsOccurred.get(gc.getKey()) + gc.getValue());
					} else {
						_geoDocsOccurred.put(gc.getKey(), gc.getValue());
					}
				}
				
				//Flush after 1000 documents
				if(progress % 3000 == 0 && progress > 0) {
					flushGeoList(geoList, geoListsName);
				}
				//==================================================

				//Update all the counts
				for(String word: wordCount.keySet()) {
					currTermInfo = _dictionary.get(word);
					currTermInfo.docFreq++;
					bufferEst += vbyteEstimate(wordCount.get(word));
					currTermInfo.estimatePosting += (vbyteEstimate(doc_id) + vbyteEstimate(wordCount.get(word))); //docid + count
				}
			}
		}
		
		//Flush Remaining
		flushGeoList(geoList, geoListsName);
		
		//System.out.println(_geoDocsOccurred.size());
		
		//======================================================== Phase 2: Build Files and Shard + Get Boundaries for Both Lists

		System.out.println("Sorting Phase");

		//Sort Ids by size: So it is as even as possible
		Collections.sort(sortByIndex, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return (int) (_dictionary.get(o2).estimatePosting - _dictionary.get(o1).estimatePosting);
			}
		});

		//Readjust/sum estimates, create files, create threshold
		long runningSum = 0;
		RandomAccessFile raIndFile = null;
		TermInfo updateTerm;
		int shardFile = 0;

		bufferEst = bufferEst / shards; //General Byte Size of each shard
		String newIndexFile;

		_numDocs = _docs.size();

		System.out.println("Number of Docs: " + _docs.size());
		System.out.println("Number of Unique Words: " + sortByIndex.size());

		//Go Through all Words
		for(int index = 0; index < sortByIndex.size(); index++) {
			updateTerm = _dictionary.get(sortByIndex.get(index));

			updateTerm.lastDocId = -1;

			//Update Ids, threshold, and estimates/lastbyte
			updateTerm.id = index;
			long termEst = updateTerm.estimatePosting;

			//========================
			//    Dynamic Sharding
			//========================
			if(Math.abs(bufferEst - runningSum) < Math.abs(bufferEst - runningSum - termEst)) {
				//If runningSum is above threshold, create new index file

				//This code builds previous index_file
				newIndexFile = _postings_list_dir + Integer.toString(shardFile);
				new File(newIndexFile).createNewFile();
				raIndFile = new RandomAccessFile( newIndexFile , "rw");
				raIndFile.setLength(runningSum);
				raIndFile.close();

				shardFile++;

				runningSum = 0;
			}

			updateTerm.lastbytePosting = runningSum;
			if(updateTerm.startPosting < 0) { //not set
				updateTerm.startPosting = runningSum;
			}

			updateTerm.file = shardFile;

			runningSum += termEst;
		}

		//garbage collection will free more space hopefully
		raIndFile = null;
		updateTerm = null;

		//============================================ Phase 3: Build Posting Lists + Skip Pointers
		TreeMap<Integer, List<Byte>> buffer = new TreeMap<>();
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

			//Update Map
			int wordIndex = 0;

			for(String word: parseDocument(fileEntry)) {
				//Stopwords
				if(stopWords.contains(word)) {
					continue;
				}

				//TODO: Look for locations to suggest
				//for( gkb.getCandidates(term))

				//Stem
				//stemmer.add(word.toCharArray(),word.length());
				//stemmer.stem();
				//word = stemmer.toString();

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

				updateVbyteEncode(buffer.get(termId), doc_id);
				updateVbyteEncode(buffer.get(termId), docBuffer.get(termId).size());
				for(Integer val: docBuffer.get(termId)) {
					updateVbyteEncode(buffer.get(termId), val);
				}
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

		//======================================= Testing Area

		//TESTING: Load Index
		//_posting_cache = new HashMap<>();
		//System.out.println(_dictionary.get("the").docFreq);
		//System.out.println(geoDocCount(4942618));
		
		//Bench-mark
		//System.out.println(documentTermFrequency("hello world", 2));
		//System.out.println(corpusDocFrequencyByTerm("java sings hello world please"));
		//System.out.println(corpusTermFrequency("hello world"));

		//======================================= Phase 4: Serialize This Object

		//Store serialized versions of _docs, _dictionary, _threshold, _totalTermFrequency
		String indexFileName = _options._indexPrefix + "/compressed.idx";
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

	//Parse the Document Specifically from Wikipedia
	private String[] parseDocument(File doc) throws Exception {
		Elements title = Jsoup.parse(doc, "UTF-8").select("title");
		Elements paragraphs = Jsoup.parse(doc, "UTF-8").select("p");
		Elements infobox = Jsoup.parse(doc, "UTF-8").select(".infobox");
		
		String docText = cleanDocument(title.text()) + " " 
							+ cleanDocument(infobox.text()) + " " 
							+ cleanDocument(paragraphs.text());

		return docText.split("\\s+");
	}

	private DocumentIndexed buildDocument(File doc, int doc_id, int count, double pagerank, int numviews) throws Exception {
		DocumentIndexed document = new DocumentIndexed(doc_id, count);

		String doc_title = Jsoup.parse(doc, "UTF-8").title();
		document.setTitle(doc_title);
		document.setUrl("https://en.wikipedia.org/wiki/" + doc.getName());

		//Set FileName
		document.fileName = doc.getAbsolutePath();

		//Set PageRank + NumViews
		document.setPageRank((float) pagerank);
		document.setNumViews(numviews);

		return document;
	}

	private String cleanDocument(String document_body) {
		return document_body.replaceAll("[^A-Za-z0-9\\s]", " ").toLowerCase();
	}

	//Focus on flush contents
	private void flush(TreeMap<Integer, List<Byte>> buffer) throws IOException {
		System.out.println("Start Flushing");
		TermInfo updateTerm;
		RandomAccessFile raIndFile = null;

		int lastFile = -1;

		for(Entry<Integer, List<Byte>> termPostingList: buffer.entrySet()) {
			updateTerm = _dictionary.get(sortByIndex.get(termPostingList.getKey()));

			//Get File
			if(lastFile != updateTerm.file) {
				if(raIndFile != null)
					raIndFile.close();
				raIndFile = new RandomAccessFile(_postings_list_dir + Integer.toString(updateTerm.file), "rw");
				lastFile = updateTerm.file;
			}

			//Jump to Last Byte
			for(Byte docId: termPostingList.getValue()) {
				raIndFile.seek(updateTerm.lastbytePosting);
				raIndFile.writeByte(docId);
				raIndFile.seek(updateTerm.lastbytePosting);
				updateTerm.lastbytePosting++;
			}

		}

		raIndFile.close();

		//Clean Buffer
		buffer.clear();
		System.out.println("Flushed");
	}
	
	//Focus on flush contents
	private void flushGeoList(List<Byte> buffer, String fileName) throws IOException {
		System.out.println("Start Flushing GeoList");
		File geoListFile = new File(fileName);
	    long lastByte = geoListFile.length();
		RandomAccessFile raIndFile = new RandomAccessFile(geoListFile, "rw");

		for(Byte toWrite: buffer) {
			raIndFile.seek(lastByte);
			raIndFile.writeByte(toWrite);
			lastByte++;
		}

		raIndFile.close();

		//Clean Buffer
		buffer.clear();
		System.out.println("Flushed GeoList");
	}

	//Individual Term load
	public PostingSkipTuple loadPostingFile(String term) {

		if(!_dictionary.containsKey(term))
			return null;

		int term_id = _dictionary.get(term).id;

		if(_posting_cache.containsKey(term_id)) {
			return _posting_cache.get(term_id);
		}

		manageCache();

		TermInfo retrieveTerm = _dictionary.get(term);

		ArrayList<Integer> postingList = new ArrayList<>();
		ArrayList<Integer> skipList = new ArrayList<>();

		int buffer = 0;
		int shift = 0;
		boolean controlBit;
		byte vbyte;

		try {
			RandomAccessFile raf = new RandomAccessFile(_postings_list_dir + Integer.toString(retrieveTerm.file), "r");

			int counter = 0;

			//Decode
			for(long pos = retrieveTerm.startPosting; pos < retrieveTerm.lastbytePosting; pos++) {
				raf.seek(pos);
				vbyte = raf.readByte();
				controlBit = ((vbyte >> 7) & 1) > 0;
				//zero out control bit
				vbyte = (byte) (vbyte & 0x7F);
				buffer = (buffer << shift) + vbyte;
				if(controlBit) {//Last bit is set so last

					//Add Skip Pointer
					if(counter == 0) {
						skipList.add(postingList.size());
						counter = -1; //Next Number is the size of
					} else if(counter == -1) {
						counter = buffer;
					} else {
						counter--;
					}

					//Add Buffer to Posting List
					postingList.add(buffer);

					buffer = 0;
					shift = 0;
				} else {
					shift = 7;
				}
			}
			raf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		PostingSkipTuple pst = new PostingSkipTuple(postingList, skipList);

		//Turn postingList array into output
		_posting_cache.put(term_id, pst);

		return pst;
	}

	@Override
	public void loadIndex() throws IOException, ClassNotFoundException {
		//Serialize DS:
		String indexFileName = _options._indexPrefix + "/compressed.idx";
		System.out.println("Load index from: " + indexFileName);

		ObjectInputStream reader = new ObjectInputStream(new FileInputStream(indexFileName));
		IndexerInvertedCompressed loaded = (IndexerInvertedCompressed) reader.readObject();
		reader.close();

		// Key DataStructures
		this._docs = loaded._docs;
		this._dictionary = loaded._dictionary;
		this.sortByIndex = loaded.sortByIndex;
		this._numDocs = _docs.size();
		
		//Load Geo Doc Count
		this._geoDocsOccurred = loaded._geoDocsOccurred;

		//Non-unique word count
		this._totalTermFrequency = loaded.temp_totalTermFrequency;
		
		//Avg Doc Length
		this._avgDocLength = loaded.temp_avgDocLength / _numDocs;

		System.out.println(Integer.toString(_docs.size()) + " documents loaded " +
				"with " + Long.toString(_dictionary.size()) + " terms!");

		//Garbage Collection drop loaded object
		loaded = null;

		_posting_cache = new HashMap<>();
		_doccount_cache = new HashMap<>();
		_termcount_cache = new HashMap<>();

		//======================================== Testing

		//=========================================
	}

	@Override
	public Document getDoc(int docid) {
		return _docs.get(docid);
	}

	//Manage cache, remove term with lowest size of terms
	//TODO: Optimize by removing # terms from cache
	private void manageCache() {
		if(_posting_cache.size() > cacheSize) {
			int lowestsize = Integer.MAX_VALUE;
			Integer removeKey = null;
			for(Integer key: _posting_cache.keySet()) {
				if(lowestsize > _posting_cache.get(key).skipList.length) {
					lowestsize = _posting_cache.get(key).skipList.length;
					removeKey = key;
				}
			}
			_posting_cache.remove(removeKey);
		}
	}

	//Wrapper of nextDoc
	public Document nextDoc(Query query, int docid) {
		try {
			boolean isSame = true;
			int maxDocId = -1;

			//Just makes sure they are all in the same document
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
		} catch( Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	//Skip Pointers
	public int[] next(String term, int docId) {
		try {
			//Single Term or Phrase that must be in order

			int[] nextDoc;
			String[] phrase = term.split("\\s+");//Split on Space

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

				for(int termPos = 0 ; termPos < phrase.length; termPos++) {
					PostingSkipTuple posting_skip_list = loadPostingFile(phrase[termPos]);

					if(posting_skip_list == null) {
						return null;
					}

					//SkipList find element greater than thresholdDocId
					int ind = findNextDocAbove(posting_skip_list, thresholdDocId);
					if(ind == -1)
						return null;

					nextDoc[termPos + 1] = posting_skip_list.skipList[ind];

					nextDoc[0] = posting_skip_list.postingList[nextDoc[termPos + 1]];

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
		} catch( Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	//Returns all the unique Words in Corpus
	public int uniqueWords() {
		return _dictionary.size();
	}

	//Bounded Binary Search: Because words are less frequent
	public boolean inOrder( String phrase, int[] docPos ) {
		if(!phrase.contains(" ")) //Just a single term
			return loadPostingFile(phrase).postingList[docPos[1] + 1] > 0;

		return documentPhraseFrequency(phrase, docPos, false) > 0;
	}

	//Binary Search in Occurrences
	public int nextOcc(Integer[] occurrences, int threshold , int low, int high) {
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
		if(occurrences[high] <= threshold) {
			return -1;
		}

		if(occurrences[low] > threshold) {
			return low;
		}

		return high;
	}

	//Binary Search in SkipList
	public int findNextDocAbove(PostingSkipTuple pst, int threshold) {
		//Binary search over skiplist
		int mid;
		int low = 0;
		int high = pst.skipList.length - 1;

		while(low < high) {
			mid = (low + high) / 2;
			if(pst.postingList[pst.skipList[mid]] <= threshold) {
				low = mid + 1;
			} else {
				high = mid;
			}
		}

		//Cannot go higher
		if(pst.postingList[pst.skipList[high]] <= threshold) {
			return -1;
		}

		if(pst.postingList[pst.skipList[low]] > threshold) {
			return low;
		}

		return pst.skipList[high];
	}

	@Override
	public int corpusTermFrequency(String term) {
		if(_termcount_cache.containsKey(term))
			return _termcount_cache.get(term);

		if(term.contains(" ")){ //Work on Phrase
			int count = 0;
			int[] doc = new int[]{-1, -1};

			while((doc = next(term, doc[0])) != null) {
				count += documentPhraseFrequency(term, doc, true);
			}

			_termcount_cache.put(term, count);
			return count;
		}

		if (_dictionary.containsKey(term)) {
			return _dictionary.get(term).corpFreq;
		} else {
			return 0;
		}
	}

	@Override
	public int corpusDocFrequencyByTerm(String term) {
		if(_doccount_cache.containsKey(term))
			return _doccount_cache.get(term);

		if(term.contains(" ")){ //Work on Phrase
			int count = 0;
			int[] doc = new int[]{-1, -1};

			//Go through each doc that contains
			while((doc = next(term, doc[0])) != null) {
				count++;
			}

			_doccount_cache.put(term, count);

			return count;
		}

		if (_dictionary.containsKey(term)) {
			return _dictionary.get(term).docFreq;
		} else {
			return 0;
		}
	}

	@Override
	public int documentTermFrequency(String term, int docid) {

		int[] doc = next(term, docid - 1);

		//docid does not contain term frequency
		if(doc == null || doc[0] != docid)
			return 0;

		//Not Phrase
		if (!(term.contains(" "))) {
			PostingSkipTuple pst = loadPostingFile(term);

			if (pst == null)
				return 0;

			return pst.postingList[doc[1] + 1];
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
			Integer[] postingsList = loadPostingFile(phrase[termPos]).postingList;
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

			int firstWordOcc = loadPostingFile(phrase[0]).postingList[startIndex];
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
					termOccIndex = nextOcc(loadPostingFile(phrase[termPos]).postingList,
							firstWordOcc, lastPos[termPos], end[termPos]);
					lastSeen.put(phrase[termPos], termOccIndex);
				}

				if(termOccIndex == -1 || termOccIndex > end[termPos]) {
					return count;
				}

				lastPos[termPos] = termOccIndex; //Update last Call

				//Get the actual Occurrence
				int nextWordOcc = loadPostingFile(phrase[termPos]).postingList[termOccIndex];

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
				if(!all) {
					return count;
				}
				maxPos++;
			}

			//Fix Find new occurrence
			startIndex = nextOcc( loadPostingFile(phrase[0]).postingList,
					maxPos - 1, lastPos[0], end[0]);
			lastPos[0] = startIndex;
		}

		return count;
	}
	
	//================================================= Experimental Suggestion Engine
	public HashMap<Integer, Integer> geoCounts(Integer docId) {
		try {
			//Get doc
			DocumentIndexed doc = (DocumentIndexed) getDoc(docId);
			
			//Load values from posting list and parse into hashmap
			RandomAccessFile raf = new RandomAccessFile(_postings_list_dir + "geoIds", "r");
	
			int counter = 0;
			Byte vbyte;
			boolean controlBit;
			int buffer = 0;
			short shift = 0;
			
			HashMap<Integer, Integer> counts = new HashMap<>();
			Integer geoID = -1;
	
			//Decode
			for(long pos = doc.startingBit; pos < doc.endingBit; pos++) {
				raf.seek(pos);
				vbyte = raf.readByte();
				controlBit = ((vbyte >> 7) & 1) > 0;
				//zero out control bit
				vbyte = (byte) (vbyte & 0x7F);
				buffer = (buffer << shift) + vbyte;
				if(controlBit) {//Last bit is set so last
	
					//Add Skip Pointer
					if(counter == 0) {
						geoID = buffer;
						counter = 1; //Next Number is the count
					} else {
						counts.put(geoID, buffer);
						counter = 0;
					}
	
					buffer = 0;
					shift = 0;
				} else {
					shift = 7;
				}
			}
			raf.close();
			return counts;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Integer geoDocCount(Integer geoId) {
		return _geoDocsOccurred.get(geoId);
	}
}