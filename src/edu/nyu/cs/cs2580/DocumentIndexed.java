package edu.nyu.cs.cs2580;

/**
 * @CS2580: implement this class for HW2 to incorporate any additional
 * information needed for your favorite ranker.
 */
public class DocumentIndexed extends Document {
  private static final long serialVersionUID = 9184892508124423115L;

  //Total Number of words in Document
  public long _numWords;
  public String fileName;

  public DocumentIndexed(int docid, long numWords) {
    super(docid);
    this._numWords = numWords;
  }
}