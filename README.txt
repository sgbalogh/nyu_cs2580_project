README - Group 05

To compile on CIMS machines:
  > module load java-1.8
  > cd group05_hw3/
  > javac -cp jsoup-1.10.1.jar  src/edu/nyu/cs/cs2580/*.java

To index:
  > java -cp  jsoup-1.10.1.jar:src edu.nyu.cs.cs2580.SearchEngine --mode=index --options=conf/engine.conf

To serve:
  > java -cp  jsoup-1.10.1.jar:src edu.nyu.cs.cs2580.SearchEngine --mode=serve --port=25805 --options=conf/engine.conf

NOTES:

1) Geospatial seed data is located in data/geospatial –– this includes:
  - US county-level information : 'admin2_data_usa.csv'
  - US state / global "admin1" region data : 'admin1_data.csv'
  - US city (with population ≥ 1000) data : 'cities1000_usa_3857.csv'
  - A serialized graph between cities and the ≤ 20 nearby cities within a 50km buffer : 'nearest_neighbors_50km.csv'
2) Geospatial data is assembled from the CSV files above by the LocationLoader class, which is run
  by the SearchEngine on --serve mode

Making use of geospatial expansion:
a) Given a query with an explicit location term (e.g. "california silicon valley"), the ranker collects documents
and evaluates whether or not to present alternate/expanded locations that have better results for the query
(e.g. "san francisco silicon valley", "san jose silicon valley", etc)
  - if the query issued by the user is deemed to be optimal, with respect to locations, then no map widget will
  appear, and we assume that the user is uninterested in seeing potential sites for geospatial expansion

b) If a query contains a location that is ambiguous (e.g. "mercer county education"), and it is not possible
for the search engine to make a reasonable decision with regards to disambiguating the location term, then
a user will be asked to manually disambiguate between candidate locations by clicking one of a set of points
presented to them on a map

c) For all queries with strong associations with locations (as determined by the indexer), we present at the footer
of the results HTML page a list of most probable locations –– this data can be used to correct queries that have
incorrect spatial terms; it also suggests new exploratory queries
  - e.g., "space needle" will suggest the location "seattle"; "surfing" will suggest various coastal locations
