README - Group 05

To compile on CIMS machines:
  > module load java-1.8
  > cd group05_hw3/
  > javac -cp jsoup-1.10.1.jar  src/edu/nyu/cs/cs2580/*.java

To mine:
  > java -cp  jsoup-1.10.1.jar:src edu.nyu.cs.cs2580.SearchEngine --mode=mining --options=conf/engine.conf
  ## Mining mode stores two serialized objects to ./data/index
  ## It also stores text file representations of PageRank and NumViews scores

To index:
  > java -cp  jsoup-1.10.1.jar:src edu.nyu.cs.cs2580.SearchEngine --mode=index --options=conf/engine.conf

To serve:
  > java -cp  jsoup-1.10.1.jar:src edu.nyu.cs.cs2580.SearchEngine --mode=serve --port=25805 --options=conf/engine.conf

To compute Spearman correllation:
  > cd group05_hw3/
  > java -cp src edu.nyu.cs.cs2580.Spearman data/index/wiki_pagerank.txt data/index/wiki_numviews.txt
  ## Our Spearman value is: 0.4109313301720696

To compute Bhattacharyya coefficients
  > cd group05_hw3/
  > vim queries.tsv ## Place queries in this file, one per line
  > bash Hw3Driver.sh ## We provide a script to iterate through queries and produce prf-*.tsv and qsim.tsv files_nice


Justification of lambda values for PageRank:
The “damping factor” lambda is a tricky entity that may make the resulting pagerank values to behave differently. 
For eg. with lambda being as low as 0.1, the pagerank values may get repeatedly over-shot, with high frequencies below and above the average ,
 i.e. the numbers swing about the average like a pendulum. With high damping factor like 0.9, rather the numbers may take longer to settle. 
 However, the numbers won’t undergo such drastic ups and downs across the average values. Also, the number of iterations eventually ensure the normalization of the pagerank values.
  So two iterations could be preferred over one iteration, with a higher damping-factor of 0.9, which is analogous to choosing the better of the two extreme values of 0.1 and 0.9.