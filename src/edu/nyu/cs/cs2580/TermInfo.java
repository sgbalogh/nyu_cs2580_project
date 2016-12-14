package edu.nyu.cs.cs2580;

import java.io.Serializable;

public class TermInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	//Id, Doc Freq, Term Freq
	public int id;
	public int docFreq;
	public int corpFreq;

	public int lastDocId;

	//leverage: 4 bytes per int
	public long startPosting = -1; //where posting list starts
	public long lastbytePosting; //Last Byte written

	public long estimatePosting;
	//Estimated Byte Size: 4 + docsize * (4) + 1 = lastByte - start

	public int file; //Sharded File

	public TermInfo(int id, int doc_id) {
		this.id = id;
		this.docFreq = 1;
		this.corpFreq = 1;
		lastDocId = doc_id;
	}

}
