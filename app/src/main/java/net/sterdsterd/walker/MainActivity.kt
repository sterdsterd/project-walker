package net.sterdsterd.walker

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityRecognitionResult

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import android.preference.PreferenceManager
import com.google.android.gms.location.DetectedActivity
import android.app.PendingIntent
import android.content.Intent
import android.view.View
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task


const val DETECTED_ACTIVITY = ".DETECTED_ACTIVITY"

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    var mContext: Context? = null

    var mActivityRecognitionClient: ActivityRecognitionClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
            PreferenceManager.getDefaultSharedPreferences(this).getString(
                DETECTED_ACTIVITY, ""
            )
        )

        mActivityRecognitionClient = ActivityRecognitionClient(this)

        fab.setOnClickListener { view ->
            if(isGooglePlayServiceAvailable(this)) {
                requestUpdatesHandler(centreText)

            } else Snackbar.make(view, "Not Available", Snackbar.LENGTH_LONG).show()
        }

    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)
        updateDetectedActivitiesList()
    }

    override fun onPause() {
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    fun requestUpdatesHandler(view: View) {
        //Set the activity detection interval. I’m using 3 seconds//
        val task = mActivityRecognitionClient?.requestActivityUpdates(
            3000,
            getActivityDetectionPendingIntent()
        )
        task?.addOnSuccessListener { updateDetectedActivitiesList() }
    }

    //Get a PendingIntent//
    private fun getActivityDetectionPendingIntent(): PendingIntent {
        //Send the activity data to our DetectedActivitiesIntentService class//
        val intent = Intent(this, ActivityIntentService::class.java)
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

    }

    //Process the list of activities//
    protected fun updateDetectedActivitiesList() {
        val detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
            PreferenceManager.getDefaultSharedPreferences(this)
                .getString(DETECTED_ACTIVITY, "")
        )
        centreText.setText(detectedActivities.toString())
        /**mAdapter.updateActivities(detectedActivities)*/
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, s: String) {
        if (s == DETECTED_ACTIVITY) {
            updateDetectedActivitiesList()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun isGooglePlayServiceAvailable(activity: Activity): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val status = googleApiAvailability.isGooglePlayServicesAvailable(activity)
        if(status != ConnectionResult.SUCCESS) {
            if(googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(activity, status, 2404).show()
            }
            return false
        }
        return true
    }
}
