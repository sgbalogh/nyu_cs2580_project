package edu.nyu.cs.cs2580.utilities.geodbdriver;

/**
 * Created by stephen on 12/7/16.
 */

import java.sql.SQLException;

public class Runner {

    public static void main(String[] args) {

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        Connector mine = new Connector();
        try {
            mine.RunQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


}


