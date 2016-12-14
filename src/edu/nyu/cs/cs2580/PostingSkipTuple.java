package edu.nyu.cs.cs2580;

import java.io.Serializable;
import java.util.List;

public class PostingSkipTuple implements Serializable {

    private static final long serialVersionUID = 1L;

    public Integer[] postingList;
    public Integer[] skipList;

    public PostingSkipTuple(List<Integer> postingList, List<Integer> skipList) {
        this.postingList = postingList.toArray(new Integer[postingList.size()]);
        this.skipList = skipList.toArray(new Integer[skipList.size()]);
    }
}
