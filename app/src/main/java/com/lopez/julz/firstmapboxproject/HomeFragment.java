package com.lopez.julz.firstmapboxproject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.utils.BitmapUtils;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.os.Looper.getMainLooper;
import static com.mapbox.core.constants.Constants.PRECISION_6;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineCap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

public class HomeFragment extends Fragment implements PermissionsListener, OnMapReadyCallback {

    public Context context;

    public MapView mapView;
    private PermissionsManager permissionsManager;
    private MapboxMap mapboxMap;
    private LocationComponent locationComponent;
    public BottomNavigationView bottomNavigationView;

    private SymbolManager symbolManager;
    private Symbol symbol;
    private static final String MAKI_ICON = "place-black-24dp";
    private static final String SELECT_ICON = "hu-main-2";
    //private static final String MAKI_ICON = "tw-provincial-expy-2";

    // OFFLINE MAP SELECTION
    private int regionSelected;
    private OfflineManager offlineManager;
    public static final String JSON_CHARSET = "UTF-8";
    public static final String JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME";
    private List<Point> routeCoordinates;
    private List<List<Point>> backspanList;
    private List<Feature> labels;

    public String destinationLat = "", destinationLon = "";

    // DETAILS
    public TextView latDisplay, longDisplay, altDisplay;

    private LocationEngine locationEngine;
    private static final long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private static final long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
    private LocationChangeListeningActivityLocationCallback callback =
            new LocationChangeListeningActivityLocationCallback(this);
    public Style styleMod;

    public String poleClass, project;

    public boolean isLongPressed = false;
    public LatLng startPoint;
    public String startPole = "";

    public boolean isDrivingModeActive = false;
    public boolean isDrivingStarted = false;
    private static final String ROUTE_LAYER_ID = "route-layer-id";
    private static final String ROUTE_SOURCE_ID = "route-source-id";
    private static final String ICON_LAYER_ID = "icon-layer-id";
    private static final String ICON_SOURCE_ID = "icon-source-id";
    private static final String RED_PIN_ICON_ID = "place-black-24dp";

    public MaterialCardView drivingModeOpts;
    public Button trackMode, addRoute;

    private Point origin;
    private Point destination;
    private DirectionsRoute currentRoute;
    private MapboxDirections client;
    public HomeFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // MAPBOX KEY
        context = getActivity();
        Mapbox.getInstance(context, getString(R.string.mapbox_access_token));

        View view = inflater.inflate(R.layout.fragment_home, container, false);


        mapView = (MapView) view.findViewById(R.id.mapViewHome);
        bottomNavigationView = (BottomNavigationView) view.findViewById(R.id.bottom_navigation);
        latDisplay = (TextView) view.findViewById(R.id.latDisplay);
        longDisplay = (TextView) view.findViewById(R.id.longDisplay);
        altDisplay = (TextView) view.findViewById(R.id.altDisplay);
        routeCoordinates = new ArrayList<>();
        backspanList = new ArrayList<>();
        labels = new ArrayList<>();
        drivingModeOpts = (MaterialCardView) view.findViewById(R.id.drivingModeOpts);
        trackMode = (Button) view.findViewById(R.id.trackMode);
        addRoute = (Button) view.findViewById(R.id.addRoute);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);


        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_refresh :
                        break;
                    case R.id.action_driving_mode :
                        setDrivingModeStatus(item);
                        break;
                }
                return true;
            }
        });

        trackMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isDrivingStarted) {
                    isDrivingStarted = false;
                    trackMode.setText("DRIVE");
                    CameraPosition position = new CameraPosition.Builder()
                            .target(new LatLng(locationComponent.getLastKnownLocation().getLatitude(), locationComponent.getLastKnownLocation().getLongitude()))
                            .zoom(15)
                            .tilt(0)
                            .padding(0, 0, 0, 0)
                            .build();
                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 600);
                    locationComponent.setRenderMode(RenderMode.GPS);
                } else {
                    isDrivingStarted = true;
                    trackMode.setText("STOP");
                    CameraPosition position = new CameraPosition.Builder()
                            .target(new LatLng(locationComponent.getLastKnownLocation().getLatitude(), locationComponent.getLastKnownLocation().getLongitude()))
                            .zoom(19)
                            .tilt(60)
                            .padding(0, 380, 0, 0)
                            .bearing(locationComponent.getLastKnownLocation().getBearing())
                            .build();
                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 600);
                    locationComponent.setRenderMode(RenderMode.GPS);
                }
            }
        });

        addRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newDestination();
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(new Style.Builder().fromUri("mapbox://styles/juliolopez/ckktd6tbv21vp18nznvfvyz5t"), new Style.OnStyleLoaded() {

            @Override
            public void onStyleLoaded(@NonNull final Style style) {
                enableLocationComponent(style);
                styleMod = style;

                offlineManager = OfflineManager.getInstance(context);

                symbolManager = new SymbolManager(mapView, mapboxMap, style);
                symbolManager.setIconAllowOverlap(true);
                symbolManager.setTextAllowOverlap(true);

            }
        });
    }

    public void setDrivingModeStatus(MenuItem item) {
        if (isDrivingModeActive) {
            isDrivingModeActive = false;
            clearMarkers(styleMod);
            Toast.makeText(context, "Driving Mode stopped", Toast.LENGTH_SHORT).show();
            item.setTitle("Driving Mode");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                drivingModeOpts.setTransitionVisibility(View.GONE);
            } else {
                drivingModeOpts.setVisibility(View.GONE);
            }

            if (isDrivingStarted) {
                isDrivingStarted = false;
                trackMode.setText("DRIVE");
                CameraPosition position = new CameraPosition.Builder()
                        .target(new LatLng(locationComponent.getLastKnownLocation().getLatitude(), locationComponent.getLastKnownLocation().getLongitude()))
                        .zoom(15)
                        .tilt(0)
                        .padding(0, 0, 0, 0)
                        .build();
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 600);
                locationComponent.setRenderMode(RenderMode.GPS);
            }
        } else {
            isDrivingModeActive = true;
            clearMarkers(styleMod);
            Toast.makeText(context, "Driving Mode started", Toast.LENGTH_SHORT).show();
            item.setTitle("Normal Mode");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                drivingModeOpts.setTransitionVisibility(View.VISIBLE);
            } else {
                drivingModeOpts.setVisibility(View.VISIBLE);
            }
            String[] destPoints = {destinationLat, destinationLon};
            if (null == destPoints) {
                Toast.makeText(context, "No destinations recorded yet! Click on NEW to add one.", Toast.LENGTH_SHORT).show();
            } else {
                startDriving(styleMod, Double.valueOf(destPoints[0]), Double.valueOf(destPoints[1]));
            }

        }
    }

    public void startDriving(Style style, Double lati, Double longi) {
        try {
            origin = Point.fromLngLat(longi, lati);

            destination = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(), locationComponent.getLastKnownLocation().getLatitude());

            initSource(style);
            initLayers(style);
            getRoute(mapboxMap, origin, destination);
        } catch (Exception e) {
            Log.e("ERR", e.getMessage());
        }
    }

    public void clearMarkers(Style style) {
        try {
            if (symbolManager != null) {
                symbolManager.deleteAll();
            }

            style.removeSource("line-source");
            style.removeLayer("linelayer");
            styleMod.removeSource("line-source");
            styleMod.removeLayer("linelayer");

            style.removeSource("labelsF");
            style.removeLayer("labelslayer");
            styleMod.removeSource("labelsF");
            styleMod.removeLayer("labelslayer");

            style.removeSource(ROUTE_SOURCE_ID);
            style.removeLayer(ROUTE_LAYER_ID);
            styleMod.removeSource(ROUTE_SOURCE_ID);
            styleMod.removeLayer(ROUTE_LAYER_ID);

            style.removeSource(ICON_SOURCE_ID);
            style.removeLayer(ICON_LAYER_ID);
            styleMod.removeSource(ICON_SOURCE_ID);
            styleMod.removeLayer(ICON_LAYER_ID);

            for (int y=0; y<backspanList.size(); y++) {
                style.removeSource("backspan" + y);
                style.removeLayer("backspan-layer" + y);
                styleMod.removeSource("backspan" + y);
                styleMod.removeLayer("backspan-layer" + y);
            }

            routeCoordinates.clear();
            backspanList.clear();
            labels.clear();
        } catch (Exception e) {
            Log.e("ERR", e.getMessage());
        }
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        try {
            // Check if permissions are enabled and if not request
            if (PermissionsManager.areLocationPermissionsGranted(context)) {

                // Get an instance of the component
                locationComponent = mapboxMap.getLocationComponent();

                // Set the LocationComponent activation options
                LocationComponentActivationOptions locationComponentActivationOptions =
                        LocationComponentActivationOptions.builder(context, loadedMapStyle)
                                .useDefaultLocationEngine(false)
                                .build();

                // Activate with options
                locationComponent.activateLocationComponent(locationComponentActivationOptions);

                // Enable to make component visible
                locationComponent.setLocationComponentEnabled(true);

                // Set the component's camera mode
                locationComponent.setCameraMode(CameraMode.TRACKING);

                // Set the component's render mode
                locationComponent.setRenderMode(RenderMode.COMPASS);

                initLocationEngine();
            } else {
                permissionsManager = new PermissionsManager(this);
                permissionsManager.requestLocationPermissions(getActivity());
            }
        } catch (Exception e) {
            Log.e("ERR_LOAD_MAP", e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(context, "Location explanation needed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        } else {
            Toast.makeText(context, "Permission not granted", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates(callback);
        }
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(context);

        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

        locationEngine.requestLocationUpdates(request, callback, getMainLooper());
        locationEngine.getLastLocation(callback);
    }

    private static class LocationChangeListeningActivityLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<HomeFragment> activityWeakReference;

        LocationChangeListeningActivityLocationCallback(HomeFragment activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        @Override
        public void onSuccess(LocationEngineResult result) {
            HomeFragment activity = activityWeakReference.get();

            if (activity != null) {
                Location location = result.getLastLocation();

                if (location == null) {
                    return;
                }

                // Create a Toast which displays the new location's coordinates
                activity.latDisplay.setText("Lat: " + result.getLastLocation().getLatitude());
                activity.longDisplay.setText("Long: " + result.getLastLocation().getLongitude());
                activity.altDisplay.setText("Alt: " + result.getLastLocation().getAltitude());

                /**
                 * DRIVING MODE
                 * Set camera position and bearing
                 */
                if (activity.isDrivingStarted) {
                    CameraPosition position = new CameraPosition.Builder()
                            .target(new LatLng(result.getLastLocation().getLatitude(), result.getLastLocation().getLongitude()))
                            .zoom(19)
                            .tilt(60)
                            .padding(0, 380, 0, 0)
                            .bearing(result.getLastLocation().getBearing())
                            .build();
                    activity.mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 600);
                    activity.locationComponent.setRenderMode(RenderMode.GPS);
                }

                // Pass the new location to the Maps SDK's LocationComponent
                if (activity.mapboxMap != null && result.getLastLocation() != null) {
                    activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                }
            }
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location can't be captured
         *
         * @param exception the exception message
         */
        @Override
        public void onFailure(@NonNull Exception exception) {
            HomeFragment activity = activityWeakReference.get();
            if (activity != null) {
                Toast.makeText(activity.context, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void downloadedRegionList() {
        // Build a region list when the user clicks the list button

        // Reset the region selected int to 0
        regionSelected = 0;
        if (offlineManager != null) {
            // Query the DB asynchronously
            offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
                @Override
                public void onList(final OfflineRegion[] offlineRegions) {
                    // Check result. If no regions have been
                    // downloaded yet, notify user and return
                    if (offlineRegions == null || offlineRegions.length == 0) {
                        Toast.makeText(context, "No Regions Yet", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Add all of the region names to a list
                    ArrayList<String> offlineRegionsNames = new ArrayList<>();
                    for (OfflineRegion offlineRegion : offlineRegions) {
                        offlineRegionsNames.add(getRegionName(offlineRegion));
                    }
                    final CharSequence[] items = offlineRegionsNames.toArray(new CharSequence[offlineRegionsNames.size()]);

                    // Build a dialog containing the list of regions
                    AlertDialog dialog = new AlertDialog.Builder(context)
                            .setTitle("Download Maps")
                            .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Track which region the user selects
                                    regionSelected = which;
                                }
                            })
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {

                                    Toast.makeText(context, items[regionSelected], Toast.LENGTH_LONG).show();

                                    // Get the region bounds and zoom
                                    LatLngBounds bounds = (offlineRegions[regionSelected].getDefinition()).getBounds();
                                    double regionZoom = (offlineRegions[regionSelected].getDefinition()).getMinZoom();

                                    // Create new camera position
                                    CameraPosition cameraPosition = new CameraPosition.Builder()
                                            .target(bounds.getCenter())
                                            .zoom(regionZoom)
                                            .build();

                                    // Move camera to new position
                                    mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                                }
                            })
                            .setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {

                                    offlineRegions[regionSelected].delete(new OfflineRegion.OfflineRegionDeleteCallback() {
                                        @Override
                                        public void onDelete() {
                                            Toast.makeText(context, "Region Deleted",
                                                    Toast.LENGTH_LONG).show();
                                        }

                                        @Override
                                        public void onError(String error) {
                                            Log.e( "Error: %s", error);
                                        }
                                    });
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    // When the user cancels, don't do anything.
                                    // The dialog will automatically close
                                }
                            }).create();
                    dialog.show();

                }

                @Override
                public void onError(String error) {
                    Log.e( "Error: %s", error);
                }
            });
        }

    }

    private String getRegionName(OfflineRegion offlineRegion) {
// Get the region name from the offline region metadata
        String regionName;

        try {
            byte[] metadata = offlineRegion.getMetadata();
            String json = new String(metadata, JSON_CHARSET);
            JSONObject jsonObject = new JSONObject(json);
            regionName = jsonObject.getString(JSON_FIELD_REGION_NAME);
        } catch (Exception exception) {
            Log.e("Failed to decode data", exception.getMessage());
            regionName = String.format("Region Name", offlineRegion.getID());
        }
        return regionName;
    }

    public void showDistance(String fromPole, String toPole, Double distance) {
        try {
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            DecimalFormat df = new DecimalFormat("#.##");
            builder.setTitle(df.format(distance) + " meters");

            builder.setMessage("Distance from pole " + fromPole + " to pole " + toPole);
            builder.setNegativeButton("CLOSE", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            builder.show();
        } catch (Exception e) {
            Log.e("ERR_SHOWING_DST", e.getMessage());
        }
    }

    /**
     * DRIVING MODE
     * These codes below are focused on the driving mode feature
     */
    private void initSource(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addSource(new GeoJsonSource(ROUTE_SOURCE_ID));

        GeoJsonSource iconGeoJsonSource = new GeoJsonSource(ICON_SOURCE_ID, FeatureCollection.fromFeatures(new Feature[] {
                Feature.fromGeometry(Point.fromLngLat(origin.longitude(), origin.latitude())),
                Feature.fromGeometry(Point.fromLngLat(destination.longitude(), destination.latitude()))}));
        loadedMapStyle.addSource(iconGeoJsonSource);
    }

    private void initLayers(@NonNull Style loadedMapStyle) {
        LineLayer routeLayer = new LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID);

        // Add the LineLayer to the map. This layer will display the directions route.
        routeLayer.setProperties(
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
                lineWidth(5f),
                lineColor(Color.parseColor("#002984"))
        );
        loadedMapStyle.addLayer(routeLayer);

        // Add the red marker icon image to the map
        loadedMapStyle.addImage(RED_PIN_ICON_ID, BitmapUtils.getBitmapFromDrawable(
                getResources().getDrawable(R.drawable.ic_baseline_place_24)));

        // Add the red marker icon SymbolLayer to the map
        loadedMapStyle.addLayer(new SymbolLayer(ICON_LAYER_ID, ICON_SOURCE_ID).withProperties(
                iconImage(RED_PIN_ICON_ID),
                iconIgnorePlacement(true),
                iconAllowOverlap(true),
                iconOffset(new Float[] {0f, -9f})));
    }

    private void getRoute(final MapboxMap mapboxMap, Point origin, Point destination) {
        client = MapboxDirections.builder()
                .origin(origin)
                .destination(destination)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .accessToken(getString(R.string.mapbox_access_token))
                .build();

        client.enqueueCall(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                // You can get the generic HTTP info about the response
                Log.e("Response code: ", response.message() + " , " + response.code());
                if (response.body() == null) {
                    Log.e("ERR", "No routes found, make sure you set the right user and access token.");
                    return;
                } else if (response.body().routes().size() < 1) {
                    Log.e("ERR_NO_RT", "No routes found");
                    return;
                }

                // Get the directions route
                currentRoute = response.body().routes().get(0);

                // Make a toast which displays the route's distance
                Toast.makeText(context, String.format(
                        "Distance from destination ",
                        currentRoute.distance()), Toast.LENGTH_SHORT).show();

                if (mapboxMap != null) {
                    mapboxMap.getStyle(new Style.OnStyleLoaded() {
                        @Override
                        public void onStyleLoaded(@NonNull Style style) {

                            // Retrieve and update the source designated for showing the directions route
                            GeoJsonSource source = style.getSourceAs(ROUTE_SOURCE_ID);

                            // Create a LineString with the directions route's geometry and
                            // reset the GeoJSON source for the route LineLayer source
                            if (source != null) {
                                source.setGeoJson(LineString.fromPolyline(currentRoute.geometry(), PRECISION_6));
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                Log.e("Error: ", throwable.getMessage());
                Toast.makeText(context, "Error: " + throwable.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void newDestination() {
        try {
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("New Destination");

            // Set up the input
            final EditText latitude = new EditText(context);
            latitude.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
            latitude.setHint("Latitude");

            final EditText longitude = new EditText(context);
            longitude.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
            longitude.setHint("Longitude");

            LinearLayout layout = new LinearLayout(context);
            layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(20, 0, 20, 5);
            layout.addView(latitude);
            layout.addView(longitude);

            builder.setView(layout);

            builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    destinationLat = latitude.getText().toString();
                    destinationLon = longitude.getText().toString();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            builder.show();
        } catch (Exception e) {
            Log.e("ERR", e.getMessage());
        }
    }
}

