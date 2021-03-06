package net.sterdsterd.walker

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.ActivityRecognitionClient

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import android.preference.PreferenceManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.hardware.SensorManager
import android.hardware.Sensor.TYPE_ORIENTATION
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import androidx.core.content.ContextCompat.getSystemService
import android.hardware.SensorEvent
import android.hardware.SensorEventListener








const val DETECTED_ACTIVITY = ".DETECTED_ACTIVITY"
const val ACTIVITY_TYPE = ".ACTIVITY_TYPE"

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    var mActivityRecognitionClient: ActivityRecognitionClient? = null
    var mSensorManager: SensorManager? = null
    var sersorrunning: Boolean = false

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
                requestUpdatesHandler(recognisedData)

            } else Snackbar.make(view, "Not Available", Snackbar.LENGTH_LONG).show()
        }


        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val mySensors: List<Sensor>? = mSensorManager?.getSensorList(Sensor.TYPE_ORIENTATION)
        if(mySensors?.size!! > 0){
            mSensorManager?.registerListener(mySensorEventListener, mySensors.get(0), SensorManager.SENSOR_DELAY_NORMAL)
            sersorrunning = true
        } else {
            sersorrunning = false
            finish();
        }

    }

    private val mySensorEventListener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent) {
            rotationX.setText("X-axis: " + event.values[1].toString())

        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

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
        val task = mActivityRecognitionClient?.requestActivityUpdates(
            500,
            getActivityDetectionPendingIntent()
        )
        task?.addOnSuccessListener { updateDetectedActivitiesList() }
    }

    private fun getActivityDetectionPendingIntent(): PendingIntent {
        val intent = Intent(this, ActivityIntentService::class.java)
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

    }

    protected fun updateDetectedActivitiesList() {
        val detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
            PreferenceManager.getDefaultSharedPreferences(this)
                .getString(DETECTED_ACTIVITY, "")
        )
        recognisedData.setText(detectedActivities.toString())

        val activityType = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(ACTIVITY_TYPE, "")
        statusText.setText(activityType)

    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, s: String) {
        if (s == DETECTED_ACTIVITY) {
            updateDetectedActivitiesList()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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
