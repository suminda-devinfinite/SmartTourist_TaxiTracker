package com.example.infowindowdemo;

import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.*;

import java.util.HashMap;
import java.util.Map;

import static android.view.View.*;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * Created by lionsroarsumi on 17-Mar-17.
 */

public class MapFragment
        extends
        com.google.android.gms.maps.MapFragment
        implements
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        OnClickListener {

    private static Spot[] SPOTS_ARRAY = new Spot[]{
            new Spot("Taxi A", new LatLng(7.3666670, 81.8000000)),
            new Spot("Taxi B", new LatLng(7.2833330, 81.6666670)),
            new Spot("Taxi C", new LatLng(7.3009720, 81.8553060)),
            new Spot("Taxi D", new LatLng(7.4166670, 81.8166670)),
            new Spot("Taxi E", new LatLng(7.3314000, 81.8334000)),
    };

    
    private static final int POPUP_POSITION_REFRESH_INTERVAL = 16;
    
    private static final int ANIMATION_DURATION = 500;

    private Map<Marker, Spot> spots;

    
    private LatLng trackedPosition;

    
    private Handler handler;

    
    private Runnable positionUpdaterRunnable;

    
    private int popupXOffset;
    private int popupYOffset;
    
    private int markerHeight;
    private AbsoluteLayout.LayoutParams overlayLayoutParams;

    
    private ViewTreeObserver.OnGlobalLayoutListener infoWindowLayoutListener;

    
    private View infoWindowContainer;
    private TextView textView;
    private TextView button;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        spots = new HashMap<>();
        markerHeight = getResources().getDrawable(R.drawable.pin).getIntrinsicHeight();
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment, null);

        FrameLayout containerMap = (FrameLayout) rootView.findViewById(R.id.container_map);
        View mapView = super.onCreateView(inflater, container, savedInstanceState);
        containerMap.addView(mapView, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        GoogleMap map = getMap();
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(7.34, 81.79), 10.75f));
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);

        map.clear();
        spots.clear();
        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.pin);
        for (Spot spot : SPOTS_ARRAY) {
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(spot.getPosition())
                    .title("Title")
                    .snippet("Subtitle")
                    .icon(icon));
            spots.put(marker, spot);
        }

        infoWindowContainer = rootView.findViewById(R.id.container_popup);
        
        infoWindowLayoutListener = new InfoWindowLayoutListener();
        infoWindowContainer.getViewTreeObserver().addOnGlobalLayoutListener(infoWindowLayoutListener);
        overlayLayoutParams = (AbsoluteLayout.LayoutParams) infoWindowContainer.getLayoutParams();

        textView = (TextView) infoWindowContainer.findViewById(R.id.textview_title);
        button = (TextView) infoWindowContainer.findViewById(R.id.button_view_article);
        button.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        
        handler = new Handler(Looper.getMainLooper());
        positionUpdaterRunnable = new PositionUpdaterRunnable();
        handler.post(positionUpdaterRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        infoWindowContainer.getViewTreeObserver().removeGlobalOnLayoutListener(infoWindowLayoutListener);
        handler.removeCallbacks(positionUpdaterRunnable);
        handler = null;
    }

    @Override
    public void onClick(View v)
    {
        String name = (String) v.getTag();
        Uri sms_uri = Uri.parse("smsto:+94717407115");
        Intent sms_intent = new Intent(Intent.ACTION_SENDTO, sms_uri);
        sms_intent.putExtra("sms_body", "Hi there ! Can you get me  ?" + name);
        startActivity(sms_intent);



    }

    @Override
    public void onMapClick(LatLng latLng) {
        infoWindowContainer.setVisibility(INVISIBLE);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        GoogleMap map = getMap();
        Projection projection = map.getProjection();
        trackedPosition = marker.getPosition();
        Point trackedPoint = projection.toScreenLocation(trackedPosition);
        trackedPoint.y -= popupYOffset / 2;
        LatLng newCameraLocation = projection.fromScreenLocation(trackedPoint);
        map.animateCamera(CameraUpdateFactory.newLatLng(newCameraLocation), ANIMATION_DURATION, null);

        Spot spot = spots.get(marker);
        textView.setText(spot.getName());
        button.setTag(spot.getName());

        infoWindowContainer.setVisibility(VISIBLE);

        return true;
    }

    private class InfoWindowLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
        @Override
        public void onGlobalLayout() {
            
            popupXOffset = infoWindowContainer.getWidth() / 2;
            popupYOffset = infoWindowContainer.getHeight();
        }
    }

    private class PositionUpdaterRunnable implements Runnable {
        private int lastXPosition = Integer.MIN_VALUE;
        private int lastYPosition = Integer.MIN_VALUE;

        @Override
        public void run() {
            
            handler.postDelayed(this, POPUP_POSITION_REFRESH_INTERVAL);

            
            if (trackedPosition != null && infoWindowContainer.getVisibility() == VISIBLE) {
                Point targetPosition = getMap().getProjection().toScreenLocation(trackedPosition);

                
                if (lastXPosition != targetPosition.x || lastYPosition != targetPosition.y) {
                    
                    overlayLayoutParams.x = targetPosition.x - popupXOffset;
                    overlayLayoutParams.y = targetPosition.y - popupYOffset - markerHeight -30;
                    infoWindowContainer.setLayoutParams(overlayLayoutParams);

                    
                    lastXPosition = targetPosition.x;
                    lastYPosition = targetPosition.y;
                }
            }
        }
    }
}
