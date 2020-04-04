package principal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.GeoJSONReader;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.AbstractShapeMarker;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.MultiMarker;
import de.fhpotsdam.unfolding.providers.EsriProvider;
import de.fhpotsdam.unfolding.providers.GeoMapApp;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.providers.Microsoft;
import de.fhpotsdam.unfolding.providers.OpenStreetMap;
import de.fhpotsdam.unfolding.utils.MapUtils;
import parsing.ParseFeed;
import processing.core.PApplet;

/** EarthquakeCityMap
 * An application with an interactive map displaying earthquake data.
 * Author: UC San Diego Intermediate Software Development MOOC team
 * @author Gabriel Otani Pereira
 * Date: April 12, 2018
 * */
public class EarthquakeCityMap extends PApplet {	
	private short mapProviderSelector = 1;
	private String mapProviderName;
	
	private String earthquakesURL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.atom";
	
	// The files containing city names and info and country names and info
	private String cityFile = "city-data.json";
	private String countryFile = "countries.geo.json";
	
	// The map
	private UnfoldingMap map;
	
	// Markers for each city
	private List<Marker> cityMarkers;
	// Markers for each earthquake
	private List<Marker> quakeMarkers;

	// A List of country markers
	private List<Marker> countryMarkers;
	
	// NEW IN MODULE 5
	private CommonMarker lastSelected;
	private CommonMarker lastClicked;
	
	public void setup() {
//		frame.setTitle("Earthquake Map");
		size(1280, 720, OPENGL);
		
		// Loads Markers data
		List<Feature> countries = GeoJSONReader.loadData(this, countryFile);
		countryMarkers = MapUtils.createSimpleMarkers(countries);
		
		List<Feature> cities = GeoJSONReader.loadData(this, cityFile);
		cityMarkers = new ArrayList<Marker>();
		for(Feature city : cities)
		  cityMarkers.add(new CityMarker(city));
		
	    // Setup map and inserts markers
		loadMap();

	    // Assignments
	    printQuakes();
	    sortAndPrint(10);
	}
	
	private void loadMap() {
		// Selects Map Tiles Provider
		if (mapProviderSelector > 5)
			mapProviderSelector = 1;
		switch (mapProviderSelector) {
		case 1:
			map = new UnfoldingMap(this, 0, 0, 1280, 720, new Microsoft.HybridProvider());
			mapProviderName = "Microsoft's Hybrid";
			break;
		case 2:
			map = new UnfoldingMap(this, 0, 0, 1280, 720, new OpenStreetMap.OpenStreetMapProvider());
			mapProviderName = "OpenStreetMap";
			break;
		case 3:
			map = new UnfoldingMap(this, 0, 0, 1280, 720, new Google.GoogleMapProvider());
			mapProviderName = "Google";
			break;
		case 4:
			map = new UnfoldingMap(this, 0, 0, 1280, 720, new EsriProvider.WorldStreetMap());
			mapProviderName = "Leaflet's WorldStreetMap";
			break;
		case 5:
			map = new UnfoldingMap(this, 0, 0, 1280, 720, new GeoMapApp.TopologicalGeoMapProvider());
			mapProviderName = "GeoMapApp's Topological";
			break;
		}
		//System.out.println("Loading #" + mapProviderSelector + ": " + mapProviderName + "\nwith earthquake data from \"" + earthquakesURL + "\"");
		// Enable events on map
		MapUtils.createDefaultEventDispatcher(this, map);
		
		// Reads new earthquake data and fills quakeMarkers
	    List<PointFeature> earthquakes = ParseFeed.parseEarthquake(this, earthquakesURL);
	    quakeMarkers = new ArrayList<Marker>();
	    for(PointFeature feature : earthquakes)
		  if(isLand(feature)) //check if LandQuake
		    quakeMarkers.add(new LandQuakeMarker(feature));
		  else // OceanQuakes
		    quakeMarkers.add(new OceanQuakeMarker(feature));
	    
	    // Add Markers to map
	    map.addMarkers(quakeMarkers);
	    map.addMarkers(cityMarkers);
	}

	public void draw() {
		background(50);
		map.draw();
		addKey();
	}
	
	private void sortAndPrint(int numToPrint) {
		Object[] array = quakeMarkers.toArray();
		Arrays.sort(array, Collections.reverseOrder());
		for (int i = 0; i < numToPrint; i++) {
			if (i == array.length)
				return;
			//System.out.println("#" + (i+1) + ": " + ((EarthquakeMarker)array[i]).getTitle());
		}
	}
	
	/** Event handler that gets called automatically when the 
	 * mouse moves.
	 */
	@Override
	public void mouseMoved()
	{
		// clear the last selection
		if (lastSelected != null) {
			lastSelected.setSelected(false);
			lastSelected = null;
		
		}
		selectMarkerIfHover(quakeMarkers);
		selectMarkerIfHover(cityMarkers);
		//loop();
	}
	
	// If there is a marker selected 
	private void selectMarkerIfHover(List<Marker> markers)
	{
		// Abort if there's already a marker selected
		if (lastSelected != null) {
			return;
		}
		
		for (Marker m : markers) 
		{
			CommonMarker marker = (CommonMarker)m;
			if (marker.isInside(map,  mouseX, mouseY)) {
				lastSelected = marker;
				marker.setSelected(true);
				return;
			}
		}
	}
	
	/** The event handler for mouse clicks
	 * It will display an earthquake and its threat circle of cities
	 * Or if a city is clicked, it will display all the earthquakes 
	 * where the city is in the threat circle
	 */
	@Override
	public void mouseClicked()
	{
		if (mouseX > 30 && mouseX < 190 && mouseY > 55 && mouseY < 310) { // If the click was in the button area (skips useless comparisons)
			// Earthquake Data Feed buttons
			if (mouseY > 55 && mouseY < 85)
				earthquakesURL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_month.atom";
			else if (mouseY > 90 && mouseY < 110)
				earthquakesURL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_week.atom";
			else if (mouseY > 125 && mouseY < 155)
				earthquakesURL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_day.atom";
			else if (mouseY > 160 && mouseY < 190)
				earthquakesURL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_hour.atom";
			
			// Map Provider button
			else if (mouseY > 260 && mouseY < 280)
				mapProviderSelector++;
			else
				return;
			loadMap();
		}
		
		if (lastClicked != null) {
			unhideMarkers();
			lastClicked = null;
		}
		else if (lastClicked == null) 
		{
			checkEarthquakesForClick();
			if (lastClicked == null) {
				checkCitiesForClick();
			}
		}
	}
	
	// Helper method that will check if a city marker was clicked on
	// and respond appropriately
	private void checkCitiesForClick()
	{
		if (lastClicked != null) return;
		// Loop over the earthquake markers to see if one of them is selected
		for (Marker marker : cityMarkers) {
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = (CommonMarker)marker;
				// Hide all the other earthquakes and hide
				for (Marker mhide : cityMarkers) {
					if (mhide != lastClicked) {
						mhide.setHidden(true);
					}
				}
				for (Marker mhide : quakeMarkers) {
					EarthquakeMarker quakeMarker = (EarthquakeMarker)mhide;
					if (quakeMarker.getDistanceTo(marker.getLocation()) 
							> quakeMarker.threatCircle()) {
						quakeMarker.setHidden(true);
					}
				}
				return;
			}
		}		
	}
	
	// Helper method that will check if an earthquake marker was clicked on
	// and respond appropriately
	private void checkEarthquakesForClick()
	{
		if (lastClicked != null) return;
		// Loop over the earthquake markers to see if one of them is selected
		for (Marker m : quakeMarkers) {
			EarthquakeMarker marker = (EarthquakeMarker)m;
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = marker;
				// Hide all the other earthquakes and hide
				for (Marker mhide : quakeMarkers) {
					if (mhide != lastClicked) {
						mhide.setHidden(true);
					}
				}
				for (Marker mhide : cityMarkers) {
					if (mhide.getDistanceTo(marker.getLocation()) 
							> marker.threatCircle()) {
						mhide.setHidden(true);
					}
				}
				return;
			}
		}
	}
	
	// loop over and unhide all markers
	private void unhideMarkers() {
		for(Marker marker : quakeMarkers) {
			marker.setHidden(false);
		}
			
		for(Marker marker : cityMarkers) {
			marker.setHidden(false);
		}
	}
	
	// helper method to draw key in GUI
	private void addKey() {
		int y = 10;
		textAlign(LEFT, CENTER);
		
		// ==== Data Date Buttons ====
		
		// Box
		strokeWeight(0);
		fill(255, 200);
		rect(10, y, 200, 200, 20);
		strokeWeight(1);
		
		// Title
		fill(0); textSize(16);
		y += 20; text("Earthquake Data", 40+(150-textWidth("Earthquake Data"))/2-5, y);
		fill(255); textSize(13);
		
		// Buttons
		textSize(14);
		y += 25;
		if (earthquakesURL.charAt(62) == 'm') fill(150);
		else fill(255);
		rect(30, y, 160, 30);
		fill(0); text("Past Month", 110-textWidth("Past Month")/2, y+15);

		if (earthquakesURL.charAt(62) == 'w') fill(150);
		else fill(255);
		y += 35; rect(30, y, 160, 30);
		fill(0); text("Past Week", 110-textWidth("Past Week")/2, y+15);

		if (earthquakesURL.charAt(62) == 'd') fill(150);
		else fill(255);;
		y += 35; rect(30, y, 160, 30);
		fill(0); text("Past Day", 110-textWidth("Past Day")/2, y+15);

		if (earthquakesURL.charAt(62) == 'h') fill(150);
		else fill(255);
		y += 35; rect(30, y, 160, 30);
		fill(0); text("Past Hour", 110-textWidth("Past Hour")/2, y+15);
		
		// ==== Map Provider Button ====
		y += 60;
		// Box
		strokeWeight(0);
		fill(255, 200);
		rect(10, y, 200, 80, 20);
		strokeWeight(1);

		// Title
		fill(0); textSize(16);
		y += 20; text("Map Provider", 40+(150-textWidth("Map Provider"))/2-5, y);
		fill(255); textSize(13);
		
		// Button
		y += 20; rect(30, y, 160, 20);
		textSize(12);
		fill(0); text(mapProviderName, 110-textWidth(mapProviderName)/2, y+10);
		
		// ==== Map Key ====
		y += 50;
		// Box
		strokeWeight(0);
		fill(255, 200);
		rect(10, y, 200, 400, 20);
		strokeWeight(1);
		
		// Title Earthquakes
		y += 20; 
		fill(0); textSize(16); 
		text("Earthquakes", 40+(150-textWidth("Earthquakes"))/2-5, y); 
		fill(255); textSize(13);
		
		// Depth colors
		fill(255, 0, 200, 255);
		y += 30; ellipse(40, y, 15, 15);
		fill(0); text("Deep (≥300m)", 60, y);
		fill(150, 0, 255, 200);
		y += 30; ellipse(40, y, 15, 15);
		fill(0); text("Intermediate (≥70m)", 60, y);
		fill(0, 100, 255, 200);
		y += 30; ellipse(40, y, 15, 15);
		fill(0); text("Moderate (≥5m)", 60, y);
		fill(0, 100, 0, 200);
		y += 30; ellipse(40, y, 15, 15);
		fill(0); text("Shallow (≥4m)", 60, y);
		
		// Land or Ocean
		fill(255);
		y += 40; ellipse(40, y, 15, 15);
		fill(0); text("On land", 60, y);
		fill(255);
		y += 30; rect(40-7.5f, y-7.5f, 15, 15);
		fill(0); text("On water", 60, y);
		
		// Age decoration
		fill(255);
		stroke(0);
		y += 40; ellipse(40, y, 15, 15);
		strokeWeight(3);
		stroke(255, 0, 0);
		line(40-7.5f, y-7.5f, 40+7.5f, y+7.5f);
		line(40+7.5f, y-7.5f, 40-7.5f, y+7.5f);
		fill(0); text("Past Hour", 60, y);
		
		fill(255);
		stroke(0);
		strokeWeight(1);
		y += 30; ellipse(40, y, 15, 15);
		stroke(0);
		line(40-7.5f, y-7.5f, 40+7.5f, y+7.5f);
		line(40+7.5f, y-7.5f, 40-7.5f, y+7.5f);
		fill(0); text("Past Day", 60, y);
		
		// Title Cities
		y += 30; 
		fill(0); textSize(16);
		text("Cities", 40+(150-textWidth("Cities"))/2-5, y);
		fill(255); textSize(13);
		
		// Coastal or high-lying
		fill(30, 150, 150);
		y += 40; triangle(40-7, y-12.124f, 40, y, 40+7, y-12.124f);
		fill(0); text("Coastal", 60, y-12.124f/2);
		fill(30, 150, 150);
		y += 30; triangle(40-7, y, 40, y-12.124f, 40+7, y);
		fill(0); text("High-lying", 60, y-12.124f/2);
		
		// Author name
		fill(30, 150, 150);
		textSize(14);
		text("Gabriel Otani Pereira - April 2019", width - textWidth("Gabriel Otani Pereira - April 2019") - 10, height - 14);
	}

	
	
	// Checks whether this quake occurred on land.  If it did, it sets the 
	// "country" property of its PointFeature to the country where it occurred
	// and returns true.  Notice that the helper method isInCountry will
	// set this "country" property already.  Otherwise it returns false.
	private boolean isLand(PointFeature earthquake) {
		
		// IMPLEMENT THIS: loop over all countries to check if location is in any of them
		// If it is, add 1 to the entry in countryQuakes corresponding to this country.
		for (Marker country : countryMarkers) {
			if (isInCountry(earthquake, country)) {
				return true;
			}
		}
		
		// not inside any country
		return false;
	}
	
	// prints countries with number of earthquakes
	// You will want to loop through the country markers or country features
	// (either will work) and then for each country, loop through
	// the quakes to count how many occurred in that country.
	// Recall that the country markers have a "name" property, 
	// And LandQuakeMarkers have a "country" property set.
	private void printQuakes() {
		int totalWaterQuakes = quakeMarkers.size();
		for (Marker country : countryMarkers) {
			String countryName = country.getStringProperty("name");
			int numQuakes = 0;
			for (Marker marker : quakeMarkers)
			{
				EarthquakeMarker eqMarker = (EarthquakeMarker)marker;
				if (eqMarker.isOnLand()) {
					if (countryName.equals(eqMarker.getStringProperty("country"))) {
						numQuakes++;
					}
				}
			}
			if (numQuakes > 0) {
				totalWaterQuakes -= numQuakes;
				//System.out.println(countryName + ": " + numQuakes);
			}
		}
		//System.out.println("OCEAN QUAKES: " + totalWaterQuakes);
	}
	
	
	
	// helper method to test whether a given earthquake is in a given country
	// This will also add the country property to the properties of the earthquake feature if 
	// it's in one of the countries.
	// You should not have to modify this code
	private boolean isInCountry(PointFeature earthquake, Marker country) {
		// getting location of feature
		Location checkLoc = earthquake.getLocation();

		// some countries represented it as MultiMarker
		// looping over SimplePolygonMarkers which make them up to use isInsideByLoc
		if(country.getClass() == MultiMarker.class) {
				
			// looping over markers making up MultiMarker
			for(Marker marker : ((MultiMarker)country).getMarkers()) {
					
				// checking if inside
				if(((AbstractShapeMarker)marker).isInsideByLocation(checkLoc)) {
					earthquake.addProperty("country", country.getProperty("name"));
						
					// return if is inside one
					return true;
				}
			}
		}
			
		// check if inside country represented by SimplePolygonMarker
		else if(((AbstractShapeMarker)country).isInsideByLocation(checkLoc)) {
			earthquake.addProperty("country", country.getProperty("name"));
			
			return true;
		}
		return false;
	}
	
	public static void main (String[] args) {
		//Add main method for running as application
		PApplet.main("principal.EarthquakeCityMap");
	}
}
