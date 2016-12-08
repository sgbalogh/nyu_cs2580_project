package edu.nyu.cs.cs2580.utilities.geodbdriver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;

/**
 * Created by stephen on 12/7/16.
 */

public class Connector {

    private Connection db;

    public Connector() {
        try {
            this.db = DriverManager.getConnection("jdbc:postgresql://localhost:5432/geospatial");
        } catch (SQLException e) {
            this.db = null;
        }
    }

    public void RunQuery() throws SQLException {
        StringBuilder toPrint = new StringBuilder();
        for (int i = 1; i < 17000; i++) {
            PreparedStatement st = this.db.prepareStatement(
                    "SELECT g1.field_1 As gref_geonames_id, g2.field_1 As gref_geonames_id" +
                            " FROM cities1000_usa As g1, cities1000_usa As g2 WHERE g1.gid" +
                            " = ? and g1.gid <> g2.gid and ST_DWithin(g1.geom, g2.geom, 50000)" +
                            " ORDER BY g2.field_15 desc LIMIT 20;");
            st.setInt(1, i);
            ResultSet rs = st.executeQuery();
            System.out.println(i);
            Integer[] returned;
            StringBuilder entry = new StringBuilder();
            while (rs.next()) {
                returned = parseRow(rs);
                if (entry.toString().equals("")) {
                    entry.append(returned[0]).append(",").append(returned[1]);
                } else {
                    entry.append(":").append(returned[1]);
                }
            }
            toPrint.append(entry.toString()).append("\n");
        }
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("nearest_neighbors_50km.csv"));
            out.write(toPrint.toString());  //Replace with the string
            out.close();
        }
        catch (IOException e)
        {
            System.out.println("Exception!");

        }

    }

    private Integer[] parseRow(ResultSet rs) throws SQLException {
        Integer[] toReturn = new Integer[2];
        Integer ref_geonames_id = rs.getInt(1);
        Integer find_geonames_id = rs.getInt(2);
        toReturn[0] = ref_geonames_id;
        toReturn[1] = find_geonames_id;
        return toReturn;
    }
}