package com.graphhopper.resources;

import com.graphhopper.GraphHopper;
import com.graphhopper.util.Helper;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.util.AccessFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;
import com.graphhopper.util.FetchMode;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path("/geocode")
@Produces(MediaType.APPLICATION_JSON)
public class LocalGeocodingResource {
    private final GraphHopper graphHopper;

    public LocalGeocodingResource(GraphHopper graphHopper) {
        this.graphHopper = graphHopper;
    }

    @GET
    public List<Map<String, Object>> geocode(
            @QueryParam("q") String query,
            @QueryParam("limit") @DefaultValue("5") int limit) {
        
        if (Helper.isEmpty(query)) {
            throw new IllegalArgumentException("q parameter cannot be empty");
        }

        List<Map<String, Object>> hits = new ArrayList<>();
        LocationIndex locationIndex = graphHopper.getLocationIndex();
        BaseGraph graph = graphHopper.getBaseGraph();
        AllEdgesIterator edgeIterator = graph.getAllEdges();
        
        int counter = 0;
        while (edgeIterator.next() && counter < limit) {
            EdgeIteratorState edge = graph.getEdgeIteratorState(edgeIterator.getEdge(), Integer.MIN_VALUE);
            String name = edge.getName();
            if (name != null && name.toLowerCase().contains(query.toLowerCase())) {
                PointList points = edge.fetchWayGeometry(FetchMode.ALL);
                if (!points.isEmpty()) {
                    int middleIndex = points.size() / 2;
                    double lat = points.getLat(middleIndex);
                    double lon = points.getLon(middleIndex);
                    
                    Snap snap = locationIndex.findClosest(lat, lon, EdgeFilter.ALL_EDGES);
                    if (snap.isValid()) {
                        Map<String, Object> hit = Map.of(
                            "name", name,
                            "point", Map.of(
                                "lat", lat,
                                "lng", lon
                            )
                        );
                        hits.add(hit);
                        counter++;
                    }
                }
            }
        }
        
        return hits;
    }
}