package com.example.treasurehunt;

import java.util.ArrayList;
import java.util.List;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import android.Manifest;
import android.widget.EditText;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Random;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    EditText txtLatitud, txtLongitud;
    private static final int POINT_RADIUS_METERS = 100;
    private GoogleMap googleMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final long LOCATION_UPDATE_INTERVAL = 5000;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private List<Marker> markers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        txtLatitud = findViewById(R.id.txtLatitud);
        txtLongitud = findViewById(R.id.txtLongitud);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        requestLocationPermission();  // Solicitar permisos al iniciar la actividad
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Coordenas para centrar el mapa en la Región Metropolitana de Chile
        LatLng regionMetropolitana = new LatLng(-33.4691, -70.6420);

        // Ajusta el nivel de zoom para abarcar toda la Región Metropolitana
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(regionMetropolitana, 10));

        addRandomMarkers(regionMetropolitana);  // Añadir marcadores aleatorios
        showRealTimeLocation();  // Mostrar ubicación en tiempo real
        drawLineToNearestLocation(regionMetropolitana);
    }

    private void drawLineToNearestLocation(LatLng origin) {
        LatLng nearestLocation = findNearestLocation(origin);
        if (nearestLocation != null) {
            googleMap.addPolyline(new PolylineOptions()
                    .add(origin, nearestLocation)
                    .width(5)
                    .color(Color.BLUE));
        }
    }

    private LatLng findNearestLocation(LatLng origin) {
        // Aquí puedes implementar lógica para encontrar la ubicación más cercana
        // Puedes usar la distancia euclidiana o alguna otra métrica según tus necesidades.
        // En este ejemplo, se toma la primera ubicación aleatoria como la más cercana.
        LatLng nearestLocation = null;
        double minDistance = Double.MAX_VALUE;

        for (Marker marker : markers) {
            LatLng markerPosition = marker.getPosition();
            double distance = getDistance(origin, markerPosition);
            if (distance < minDistance) {
                minDistance = distance;
                nearestLocation = markerPosition;
            }
        }

        return nearestLocation;
    }

    private double getDistance(LatLng point1, LatLng point2) {
        // Calcula la distancia euclidiana entre dos puntos
        double lat1 = point1.latitude;
        double lon1 = point1.longitude;
        double lat2 = point2.latitude;
        double lon2 = point2.longitude;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Radio de la Tierra en metros (puedes ajustar según tus necesidades)
        double radius = 6371000;

        return radius * c;
    }

    private void addRandomMarkers(LatLng center) {
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            double latOffset = (random.nextDouble() - 0.5) * 0.2;
            double lngOffset = (random.nextDouble() - 0.5) * 0.2;
            double randomLat = center.latitude + latOffset;
            double randomLng = center.longitude + lngOffset;
            LatLng randomLocation = new LatLng(randomLat, randomLng);

            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(randomLocation)
                    .title("Ubicación " + (i + 1))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

            markers.add(marker);
        }
    }

    private void requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                // Permiso ya concedido, mostrar ubicación en tiempo real
                showRealTimeLocation();
            }
        }
    }

    private void showRealTimeLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);

            fusedLocationProviderClient.requestLocationUpdates(createLocationRequest(), new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    if (locationResult != null && locationResult.getLastLocation() != null) {
                        Location location = locationResult.getLastLocation();
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        googleMap.animateCamera(CameraUpdateFactory.newLatLng(currentLocation));
                        txtLatitud.setText(String.valueOf(location.getLatitude()));
                        txtLongitud.setText(String.valueOf(location.getLongitude()));

                        drawCircle(currentLocation);
                    }
                }
            }, null);
        }
    }

    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(LOCATION_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    private void drawCircle(LatLng center) {
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(center);
        circleOptions.radius(5000); // Radio en metros
        circleOptions.strokeWidth(3f);
        circleOptions.strokeColor(Color.RED);
        circleOptions.fillColor(Color.parseColor("#30FF0000"));
        googleMap.addCircle(circleOptions);
    }

    // Implementar onRequestPermissionsResult para manejar la respuesta del usuario a la solicitud de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, mostrar ubicación en tiempo real
                showRealTimeLocation();
            } else {
                // Permiso denegado, manejar esta situación según tus necesidades
            }
        }
    }


}

