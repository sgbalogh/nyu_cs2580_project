package edu.nyu.cs.cs2580;

import java.util.ArrayList;
import java.util.List;

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
        this.construct();
    }

    private void construct() {
        switch (qbg._presentation_mode) {
            case NONE:
                constructNonSpatial();
                break;
            case AMBIGUOUS:
                constructAmbiguous();
                break;
            case EXPANSION:
                constructSpatial();
                break;
        }
    }

    private void constructAmbiguous() {
        createSpatialHead();
        createAmbiguousBody();
    }

    private void constructNonSpatial() {
        createNonSpatialHead();
        createNonSpatialBody();
    }

    private String generateGeoJSONforAmbiguous() {
        return SpatialEntityKnowledgeBase.makeDisambiguateGeoJSON(qbg.get_candidate_geo_entities());
    }

    private String generateGeoJSONforExpansion() {
        List<GeoEntity> combined = new ArrayList<GeoEntity>();
        combined.addAll(qbg.get_candidate_geo_entities());
        combined.addAll(qbg.get_expanded_geo_entities());
        return SpatialEntityKnowledgeBase.makeGeoJSON(combined);
    }

    private void constructSpatial() {
        createSpatialHead();
        createSpatialBody();
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
                "       var non_spatial_query = \"");

                // HERE we want to insert the non-spatial terms:
                builder.append(qbg.getSupportingTokens().toString().replaceAll("[^A-Za-z0-9]", " ").trim());

                builder.append("\";\n" +
                "      var button_link = \"./search?\" + getStringForNewSearch(non_spatial_query) + \"&best=true&place=");
                builder.append(qbg.get_candidate_geo_entities().get(0).getId());
                builder.append("\";\n" +
                "\n" +
                "        $('document').ready(function () {\n" +
                "            $('#button_zone').append(\"<a class=\\\"btn btn-default\\\" href=\\\"\" + button_link + \"\\\" role=\\\"button\\\">See all results for general area</a>\");\n" +
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

    private void createAmbiguousBody() {
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
                "        <h4>Which <code>");
        builder.append(qbg.get_candidate_geo_entities().get(0).getName());
        builder.append("</code> did you mean?</h4><h5>Click on one to select...</h5>\n" +
                "        <div id=\"map\">\n" +
                "        </div><br>\n" +
                "<div class=\"well well-sm\">\n" +
                        "          <ol id=\"amb_links\">\n" +
                        "          </ol>\n" +
                        "        </div>" +
                "    </div>\n" +
                "\n" +
                "\n" +
                "</div>");
        builder.append("</div>\n" +
                "\n" +
                "<script>\n" +
                "\n" +
                "    var geojson = ");
        builder.append(this.generateGeoJSONforAmbiguous());
        builder.append(";\n" +
                "\n" +
                "    var tiles = L.tileLayer('http://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png', {\n" +
                "        attribution: '&copy; <a href=\"http://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors, &copy; <a href=\"https://carto.com/attributions\">CARTO</a>',\n" +
                "        detectRetina: true\n" +
                "    });\n" +
                "\n" +
                "    var latlng = L.latLng(40.7, -74);\n" +
                "\n" +
                "    var map = L.map('map', {center: latlng, zoom: 7, layers: [tiles]}).fitBounds(L.geoJSON(geojson).getBounds());\n" +
                "\n" +
                "\n" +
                "    var marker_primary = {\n" +
                "        radius: 20,\n" +
                "        fillColor: \"#f49242\",\n" +
                "        color: \"#965018\",\n" +
                "        weight: 3,\n" +
                "        opacity: .6,\n" +
                "        fillOpacity: 0.8\n" +
                "    };\n" +
                "\n" +
                "    var marker_expanded = {\n" +
                "        radius: 15,\n" +
                "        fillColor: \"#6098ff\",\n" +
                "        color: \"#6098ff\",\n" +
                "        weight: 2,\n" +
                "        opacity: .8,\n" +
                "        fillOpacity: 0.8\n" +
                "    };\n" +
                "    //L.circle([ -90.19789, 38.62727], 50000).addTo(map);\n" +
                "    L.geoJSON(geojson, {\n" +
                "        pointToLayer: function (feature, latlng) {\n" +
                "            switch (feature.properties.type) {\n" +
                "                case \"candidate\":\n" +
                "                {\n" +
                "                    return L.circleMarker(latlng, marker_primary)\n" +
                "                }\n" +
                "                    ;\n" +
                "                case \"expanded\":\n" +
                "                {\n" +
                "                    return L.circleMarker(latlng, marker_expanded)\n" +
                "                }\n" +
                "            }\n" +
                "        },\n" +
                "onEachFeature: function (feature, layer) {\n" +
                "            var non_location_q = \"");
                // INSERT NON LOCATION QUERY HERE
                builder.append(qbg.getSupportingTokens().toString().replaceAll("[^A-Za-z0-9]", " ").trim());

                builder.append("\";\n" +
                "            var request_url = getStringForNewSearch(encodeURI(non_location_q)) + \"&place=\" + feature.id + \"&uname=\" + feature.properties.addl_terms;\n" +
                "            var popup_text = feature.properties.type == 'primary' ? '<b>' + feature.properties.name + '</b>' : feature.properties.name;\n" +
                "            var link_text = feature.properties.name;\n" +
                "            if (feature.properties.state != null) {\n" +
                "              popup_text += \"<br><b>\" + feature.properties.state + \"</b>\";\n" +
                "              link_text += \", \" + feature.properties.state;\n" +
                "            }\n" +
                "            $('#amb_links').append(\"<li><a href=\\\"\" + \"./search?\" + request_url + \"\\\">\" + link_text + \"</a></li>\");\n" +
                "            layer.bindPopup(popup_text);\n" +
                "            layer.on('mouseover', function (e) {\n" +
                "                this.openPopup();\n" +
                "            });\n" +
                "            layer.on('mouseout', function (e) {\n" +
                "                this.closePopup();\n" +
                "            });\n" +
                "            layer.on('click', function (e) {\n" +
                "              console.log(request_url);\n" +
                "              window.location.href = \"./search?\" + request_url;\n" +
                "            })\n" +
                "        }\n" +
                "    }).addTo(map);" +
                "\n" +
                "</script>\n" +
                "\n" +
                "\n" +
                "</body>\n" +
                "</html>");

    }

    private void createSpatialBody() {
        builder.append("<body>" +
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
                "        <h3><code><b>");
        builder.append(qbg.get_candidate_geo_entities().get(0).getName());
        builder.append("</b>");
        if (!qbg.get_expanded_geo_entities().get(0).getStateName().equals("")) {
            builder.append(", ");
            builder.append(qbg.get_candidate_geo_entities().get(0).getStateName());
        }
        builder.append("</code></h3>\n" +
                "        <div id=\"population\">\n" +
                "        </div>\n" +
                "        <h5>These nearby locations (<i>in blue</i>) appear to have relevant documents...</h5>\n" +
                "        <div id=\"map\">\n" +
                "        </div>\n" +
                "        <br>\n" +
                "        <div id=\"button_zone\">\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "\n" +
                "</div>");
        builder.append("</div>\n" +
                "\n" +
                "<script>\n" +
                "\n" +
                "    var geojson = ");
        builder.append(this.generateGeoJSONforExpansion());
        builder.append(";\n" +
                "\n" +
                "    var tiles = L.tileLayer('http://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png', {\n" +
                "        attribution: '&copy; <a href=\"http://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors, &copy; <a href=\"https://carto.com/attributions\">CARTO</a>',\n" +
                "        detectRetina: true\n" +
                "    });\n" +
                "\n" +
                "    var latlng = L.latLng(40.7, -74);\n" +
                "\n" +
                "    var map = L.map('map', {center: latlng, zoom: 7, layers: [tiles]}).fitBounds(L.geoJSON(geojson).getBounds());\n" +
                "\n" +
                "\n" +
                "    var marker_primary = {\n" +
                "        radius: 20,\n" +
                "        fillColor: \"#03993f\",\n" +
                "        color: \"#00471c\",\n" +
                "        weight: 3,\n" +
                "        opacity: .6,\n" +
                "        fillOpacity: 0.8\n" +
                "    };\n" +
                "\n" +
                "    var marker_expanded = {\n" +
                "        radius: 15,\n" +
                "        fillColor: \"#6098ff\",\n" +
                "        color: \"#6098ff\",\n" +
                "        weight: 2,\n" +
                "        opacity: .8,\n" +
                "        fillOpacity: 0.8\n" +
                "    };\n" +
                "    //L.circle([ -90.19789, 38.62727], 50000).addTo(map);\n" +
                "    L.geoJSON(geojson, {\n" +
                "        pointToLayer: function (feature, latlng) {\n" +
                "            switch (feature.properties.type) {\n" +
                "                case \"primary\":\n" +
                "                {\n" +
                "                    return L.circleMarker(latlng, marker_primary)\n" +
                "                }\n" +
                "                    ;\n" +
                "                case \"expanded\":\n" +
                "                {\n" +
                "                    return L.circleMarker(latlng, marker_expanded)\n" +
                "                }\n" +
                "            }\n" +
                "        },\n" +
                "        onEachFeature: function (feature, layer) {\n" +
                "            layer.bindPopup(feature.properties.type == 'primary' ? '<b>' + feature.properties.name + '</b>' : feature.properties.name);\n" +
                "            layer.on('mouseover', function (e) {\n" +
                "                this.openPopup();\n" +
                "            });\n" +
                "            layer.on('mouseout', function (e) {\n" +
                "                this.closePopup();\n" +
                "            });\n" +
                "            layer.on('click', function (e) {\n" +
                "                window.location.href = \"./search?hi there\"\n" +
                "            })\n" +
                "        }\n" +
                "    }).addTo(map);\n" +
                "\n" +
                "</script>\n" +
                "\n" +
                "\n" +
                "</body>\n" +
                "</html>");

    }

    public String toString() {
        return this.builder.toString();
    }


}

