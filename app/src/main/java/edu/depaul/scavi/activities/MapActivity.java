package edu.depaul.scavi.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import edu.depaul.scavi.R;
import edu.depaul.scavi.data.Clue;
import edu.depaul.scavi.data.ScavengerHunt;
import edu.depaul.scavi.data.Session;
import edu.depaul.scavi.data.User;
import edu.depaul.scavi.networking.NetworkManager;

public class MapActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    String url = "http://peter1123.pythonanywhere.com/ScaviHunt/default/huntsession.json/?user_id=%d&hunt_id=%s";
    String updateUrl = "http://peter1123.pythonanywhere.com/ScaviHunt/default/updatesession.json/?user_id=%d&hunt_id=%s&current_clue_number=%d";
    JsonObjectRequest request;
    Gson gson = new Gson();

    ScavengerHunt scavengerHunt;
    Session session;
    Clue[] clues;
    int currentClueIndex = 1;
    ProgressDialog dialog;

    public static Intent getIntent(Context c, ScavengerHunt hunt) {
        Intent i = new Intent(c, MapActivity.class);
        i.putExtra("hunt", hunt);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        setUpMapIfNeeded();

        scavengerHunt = (ScavengerHunt)getIntent().getSerializableExtra("hunt");

        request = new JsonObjectRequest(Request.Method.GET,
                String.format(url, User.loggedUser.id, scavengerHunt.getId()), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        try {
                            String json = jsonObject.getJSONArray("rows").toString();
                            clues = gson.fromJson(json, Clue[].class);
                            json = jsonObject.getJSONArray("session").getJSONObject(0).toString();
                            session = gson.fromJson(json, Session.class);
                            updateMap(session.current_clue_number);
                            dialog.dismiss();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.v(MapActivity.class.getName(), volleyError.toString());
                    }
                });
        dialog = ProgressDialog.show(this, "", "Loading clues...", true);
        NetworkManager.getInstance(this).addRequest(request);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    private void updateMap(int newClueIndex) {
        mMap.clear();
        currentClueIndex = newClueIndex;
        if (clues != null && clues.length > 0 && clues.length > currentClueIndex) {
            Clue currentClue = clues[currentClueIndex];
            LatLng cluePosition = new LatLng(currentClue.getLatitude(), currentClue.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(cluePosition)
                    .title("Enter Augmented Reality..."));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cluePosition, 15));
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("You have finished the Scavenger Hunt!")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).show();
        }
    }

    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    Clue currentClue = clues[currentClueIndex];
                    if (currentClue != null) {
                        startActivityForResult(ARActivity.getIntent(MapActivity.this, currentClue), 0);
                    }
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_OK) {
            if (currentClueIndex != clues.length - 1) {
                showPointsDialog(clues[currentClueIndex].getPoints());
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("You earned " + clues[currentClueIndex].getPoints() + " points! " +
                                "You have finished the Scavenger Hunt!")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
            }
            updateServer(currentClueIndex + 1);
            updateMap(currentClueIndex + 1);
        }
    }

    private void updateServer(int clueNumber) {
        request = new JsonObjectRequest(Request.Method.GET,
                String.format(updateUrl, User.loggedUser.id, scavengerHunt.getId(), clueNumber), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        Log.v("LOG IT", "Updated");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.v(MapActivity.class.getName(), volleyError.toString());
                    }
                });
        NetworkManager.getInstance(this).addRequest(request);
    }

    private void showPointsDialog(int points) {
        new AlertDialog.Builder(this)
                .setTitle("You earned " + points + " points!")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }
}
