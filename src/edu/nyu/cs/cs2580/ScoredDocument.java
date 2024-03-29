package edu.nyu.cs.cs2580;

/**
 * Document with score.
 *
 * @author fdiaz
 * @author congyu
 */
class ScoredDocument implements Comparable<ScoredDocument> {
  private Document _doc;
  private double _score;

  public ScoredDocument(Document doc, double score) {
    _doc = doc;
    _score = score;
  }

  public String asTextResult() {
    StringBuffer buf = new StringBuffer();
    buf.append(_doc._docid).append("\t");
    buf.append(_doc.getTitle()).append("\t");
    buf.append(_score).append("\t");
    buf.append(_doc.getPageRank()).append("\t");
    buf.append(_doc.getNumViews());
    return buf.toString();
  }

  public Integer getDocID() {
    return _doc._docid;
  }

  /**
   * @CS2580: Student should implement {@code asHtmlResult} for final project.
   */
  public String asHtmlResult() {
    StringBuilder buf = new StringBuilder();
    buf.append("<tr>");
    //buf.append("<td>").append(_query).append("</td>");
    buf.append("<td><code>").append(_doc._docid).append("</code></td>");
    buf.append("<td><a href=\"").append(_doc.getUrl().replace("_wiki_", "")).append("\">").append(_doc.getTitle()).append("</a></td>");
    buf.append("<td>").append(_score).append("</td>");
    //buf.append("<td>").append(_doc.getNumViews()).append("</td>");
    buf.append("<td>").append(_doc.getPageRank()).append("</td>");
    buf.append("</tr>");
    return buf.toString();
  }

  @Override
  public int compareTo(ScoredDocument o) {
    if (this._score == o._score) {
      return 0;
    }
    return (this._score > o._score) ? 1 : -1;
  }

  public double getScore() {
	return _score;
  }
  
  public void setScore(double score) {
	_score = score;
  }
  
  public Document getDoc() {
	return _doc;
  }
}