package edu.nyu.cs.cs2580;

import java.util.List;
import java.util.Vector;

/**
 * Created by stephen on 12/5/16.
 */
public class HtmlGenerator {

    List<ScoredDocument> documents;
    StringBuilder builder;
    QueryBoolGeo qbg;

    public HtmlGenerator(List<ScoredDocument> docs, QueryBoolGeo query) {
        this.documents = docs;
        this.qbg = query;
        this.builder = new StringBuilder();
        this.construct(query);
    }

    private void construct(QueryBoolGeo query) {
        if (qbg._should_present) {
            constructSpatial();
        } else {
            constructNonSpatial();
        }
    }

    private void constructNonSpatial() {
        createNonSpatialHead();
        createNonSpatialBody();
    }

    private void constructSpatial() {
        createSpatialHead();
    }

    private void createNonSpatialBody() {
        builder.append("<body>\n" +
                "<div class=\"container\">\n" +
                "    <h3>");
        builder.append(this.qbg._query);
        builder.append("</h3>\n" +
                "    <input type=\"text\" class=\"form-control\" value=\"\" placeholder=\"Search here...\"\n" +
                "           id=\"search_bar\">\n" +
                "    <br>\n" +
                "\n" +
                "    <div class=\"col-md-12\">\n" +
                "        <table class=\"table table-hover\">\n" +
                "            <thead>\n" +
                "            <tr>\n" +
                "                <th>Document ID</th>\n" +
                "                <th>Title</th>\n" +
                "                <th>Score</th>\n" +
                "                <th>PageRank</th>\n" +
                "            </tr>\n" +
                "            </thead>\n" +
                "            <tbody>");

        for (ScoredDocument doc : this.documents) {
            builder.append(doc.asHtmlResult());
        }

        builder.append("</tbody>\n" +
                "        </table>\n" +
                "    </div>\n" +
                "</div>\n" +
                "\n" +
                "\n" +
                "</body>\n" +
                "</html>");
    }



    private void createNonSpatialHead() {
        builder.append("<html>\n" +
                "<head>\n" +
                "    <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\"\n" +
                "          integrity=\"sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u\" crossorigin=\"anonymous\">\n" +
                "    <script\n" +
                "        src=\"https://code.jquery.com/jquery-3.1.1.min.js\"\n" +
                "        integrity=\"sha256-hVVnYaiADRTO2PzUGmuLJr8BLUSjGIZsDYGmIJLv2b8=\"\n" +
                "        crossorigin=\"anonymous\"></script>\n" +
                "\n" +
                "    <script>\n" +
                "      var getStringForNewSearch = function(string) {\n" +
                "        var substring = window.location.search.substring(1);\n" +
                "        var params = substring.split('&');\n" +
                "        for (i = 0; i < params.length; i++) {\n" +
                "          var components = params[i].split('=');\n" +
                "          if (components[0].toLowerCase() == 'query') {\n" +
                "            params[i] = \"query=\" + string;\n" +
                "          }\n" +
                "        }\n" +
                "        return params.join(\"&\");\n" +
                "      }\n" +
                "\n" +
                "      var getQueryFromQueryString = function() {\n" +
                "        var substring = window.location.search.substring(1);\n" +
                "        var query_string = \"\";\n" +
                "        var params = substring.split('&');\n" +
                "        for (i = 0; i < params.length; i++) {\n" +
                "          var components = params[i].split('=');\n" +
                "          if (components[0].toLowerCase() == 'query' && components.length > 0) {\n" +
                "            query_string = decodeURIComponent(components[1]);\n" +
                "          }\n" +
                "        }\n" +
                "        return query_string;\n" +
                "      }\n" +
                "\n" +
                "        $('document').ready(function () {\n" +
                "            $(\"#search_bar\").val(getQueryFromQueryString());\n" +
                "            $(\"#search_bar\").keyup(function (event) {\n" +
                "                if (event.keyCode == 13) {\n" +
                "                    value = $(\"#search_bar\").val();\n" +
                "                    window.location.href = \"?\" + getStringForNewSearch(value);\n" +
                "                    console.log(value);\n" +
                "                }\n" +
                "            });\n" +
                "        });\n" +
                "    </script>\n" +
                "\n" +
                "    <title>Search Engines :: Group 05</title>\n" +
                "\n" +
                "</head>");
    }

    private void createSpatialHead() {
        builder.append("<html>\n" +
                "<head>\n" +
                "    <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\"\n" +
                "          integrity=\"sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u\" crossorigin=\"anonymous\">\n" +
                "    <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.0.2/leaflet.css\">\n" +
                "    <script src=\"https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.0.2/leaflet-src.js\"></script>\n" +
                "    <script\n" +
                "        src=\"https://code.jquery.com/jquery-3.1.1.min.js\"\n" +
                "        integrity=\"sha256-hVVnYaiADRTO2PzUGmuLJr8BLUSjGIZsDYGmIJLv2b8=\"\n" +
                "        crossorigin=\"anonymous\"></script>\n" +
                "\n" +
                "    <style>\n" +
                "        #map {\n" +
                "            width: 100%;\n" +
                "            height: 400px;\n" +
                "        }\n" +
                "    </style>\n" +
                "    <script>\n" +
                "      var getStringForNewSearch = function(string) {\n" +
                "        var substring = window.location.search.substring(1);\n" +
                "        var params = substring.split('&');\n" +
                "        for (i = 0; i < params.length; i++) {\n" +
                "          var components = params[i].split('=');\n" +
                "          if (components[0].toLowerCase() == 'query') {\n" +
                "            params[i] = \"query=\" + string;\n" +
                "          }\n" +
                "        }\n" +
                "        return params.join(\"&\");\n" +
                "      }\n" +
                "\n" +
                "      var getQueryFromQueryString = function() {\n" +
                "        var substring = window.location.search.substring(1);\n" +
                "        var query_string = \"\";\n" +
                "        var params = substring.split('&');\n" +
                "        for (i = 0; i < params.length; i++) {\n" +
                "          var components = params[i].split('=');\n" +
                "          if (components[0].toLowerCase() == 'query' && components.length > 0) {\n" +
                "            query_string = decodeURIComponent(components[1]);\n" +
                "          }\n" +
                "        }\n" +
                "        return query_string;\n" +
                "      }\n" +
                "\n" +
                "        $('document').ready(function () {\n" +
                "            $(\"#search_bar\").val(getQueryFromQueryString());\n" +
                "            $(\"#search_bar\").keyup(function (event) {\n" +
                "                if (event.keyCode == 13) {\n" +
                "                    value = $(\"#search_bar\").val();\n" +
                "                    window.location.href = \"?\" + getStringForNewSearch(value);\n" +
                "                    console.log(value);\n" +
                "                }\n" +
                "            });\n" +
                "        });\n" +
                "    </script>\n" +
                "    <title>Search Engines :: Group 05</title>\n" +
                "</head>");
    }

    private void createSpatialBody() {
        builder.append("<body>\n" +
                "<div class=\"container\">\n" +
                "    <h3>");
        builder.append(this.qbg._query);
        builder.append("</h3>\n" +
                "    <input type=\"text\" class=\"form-control\" value=\"\" placeholder=\"Search here...\"\n" +
                "           id=\"search_bar\">\n" +
                "    <br>\n" +
                "\n" +
                "    <div class=\"col-md-8\">\n" +
                "        <table class=\"table table-hover\">\n" +
                "            <thead>\n" +
                "            <tr>\n" +
                "                <th>Document ID</th>\n" +
                "                <th>Title</th>\n" +
                "                <th>Score</th>\n" +
                "                <th>PageRank</th>\n" +
                "            </tr>\n" +
                "            </thead>\n" +
                "            <tbody>");

        for (ScoredDocument doc : this.documents) {
            builder.append(doc.asHtmlResult());
        }

        builder.append("</tbody>\n" +
                "        </table>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"col-md-4\">\n" +
                "        <h4>Chicago</h4>\n" +
                "        <h5><div class=\"label label-primary\">Cook County</div></h5><h5><div class=\"label label-success\">Illinois</div></h5>\n" +
                "        <h5>Population <code>2949384</code></h5> \n" +
                "\n" +
                "        <div id=\"map\">\n" +
                "        </div>\n" +
                "        <br>\n" +
                "        <a class=\"btn btn-default\" href=\"#\" role=\"button\">Issue Query for Nearby Cities</a>\n" +
                "        <a class=\"btn btn-default\" href=\"#\" role=\"button\">Issue Query for Cook County</a>\n" +
                "        <a class=\"btn btn-default\" href=\"#\" role=\"button\">Issue Query for Illinois</a>\n" +
                "    </div>\n" +
                "\n" +
                "\n" +
                "</div>");

    }

    public String toString() {
        return this.builder.toString();
    }


}

