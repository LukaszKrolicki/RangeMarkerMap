package eu.pl.snk.senseibunny.test3

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import eu.pl.snk.senseibunny.test3.databinding.ActivityMainBinding
import eu.pl.snk.senseibunny.test3.databinding.PopupBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var LocationProvider: FusedLocationProviderClient //Used for user location

    public var mLatitude: Double = 0.0 //updated latitude
    public var mLongitude: Double = 0.0 //updated longitude

    private lateinit var googleMap: GoogleMap //initialized map

    var marker: Marker? = null // user location marker
    var marker2: Marker? = null //user location marker
    var circle: Circle? = null // user location circle

    var counter=1 //test purpose
    var position:LatLng?=null

    var zoom: Float = 14f
    private var shops: ArrayList<Sklep>?=null //all shops list
    private var markers: ArrayList<Marker>? = ArrayList() //all markers list

    private var markers1km: ArrayList<Marker>? =ArrayList() //markers in first range
    private var shops1: ArrayList<Sklep>?=ArrayList()  //shops in first range
    private var markers2km: ArrayList<Marker>? = ArrayList()
    private var shops2: ArrayList<Sklep>?=ArrayList()
    private var markers3km: ArrayList<Marker>? = ArrayList()
    private var shops3: ArrayList<Sklep>?=ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //getting users location
        LocationProvider= LocationServices.getFusedLocationProviderClient(this)
        getUsersLocation()

        //for map animation
        val supportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

        //if buttons clicked set radius and set markers in that range
        binding.OneKm.setOnClickListener{
            circle?.radius=2000.0
            setRangeMarkers(1)
        }

        binding.TwoKm.setOnClickListener{
            circle?.radius=3000.0
            setRangeMarkers(2)
        }

        binding.ThreeKm.setOnClickListener{
            circle?.radius=5000.0
            setRangeMarkers(3)
        }

        //zooming buttons
        binding.Plus.setOnClickListener{
            zoom++
            CameraUpdateFactory.newLatLngZoom(position!!, zoom)
        }

        binding.Minus.setOnClickListener{
            zoom--;
            CameraUpdateFactory.newLatLngZoom(position!!, zoom)
        }



    }

    //permissions
    //////////////////////////////////////////////////////////////////
    private fun getUsersLocation(){
        
        //if location denied
        if(shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)){
            showRationaleDialog(
                "App requires location access",
                "Allow location to be able to do this"
            )

        }
        else{
            //Asking for permissions
            println(isLocationEnabled())
            getPermissions.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION ))
            Toast.makeText(this,"Location enabled?", Toast.LENGTH_SHORT).show()
        }

    }

    //Checking if location is enabled
    private fun isLocationEnabled():Boolean{
        val locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    //Asking for permissions
    private val getPermissions:ActivityResultLauncher<Array<String>> = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()){
        permissions->
        permissions.entries.forEach{
            val permissionName= it.key
            val isGranted = it.value
            if(isGranted){
                Toast.makeText(this,"Permission $permissionName granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRationaleDialog(title: String, message: String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setNegativeButton("Cancel"){dialog, _->dialog.dismiss()}
        builder.setPositiveButton("Settings"){ _, _->goToSettings()}
        builder.create().show()
    }

    fun goToSettings(){
        val Intent= Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(Intent)
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////



    //user position update
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        Toast.makeText(this,"Requesting location data", Toast.LENGTH_SHORT).show()
        val mLocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(1000)
            .setMaxUpdateDelayMillis(3000)
            .build()

        LocationProvider.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.myLooper()
        )
    }

    //function changing user location, getting user city, setting markers
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation = locationResult.lastLocation
            //current location
            mLatitude = mLastLocation!!.latitude
            mLongitude = mLastLocation!!.longitude
            println(mLastLocation)

//            //moving map
//            if(counter==1){
//                position = LatLng(mLatitude, mLongitude)
//                marker = googleMap.addMarker(MarkerOptions().position(position!!).title("h1")) //add a marker to the map
//                circle = googleMap.addCircle(
//                    CircleOptions()
//                        .center(LatLng(mLatitude,mLongitude))
//                        .radius(10000.0)
//                        .strokeColor(Color.RED)
//                        .fillColor(0x220000FF)
//                )
//                val newLatLangZoom = CameraUpdateFactory.newLatLngZoom(position!!, zoom)//zoom in on the map
//                counter=counter+1
//                googleMap.animateCamera(newLatLangZoom)//zooming animation
//            }
//            else{
//                position = LatLng(52.0, 19.0+counter)
//
//                marker?.position = position as LatLng
//                circle?.center = position as LatLng
//
//                val newLatLangZoom = CameraUpdateFactory.newLatLngZoom(position!!, zoom)//zoom in on the map
//
//                //counter=counter+1 //to move
//
//                googleMap.animateCamera(newLatLangZoom)//zooming animation
//            }

            //Test for shop range
            if(counter==1){
                position = LatLng(52.25, 21.0) // here you can set mLat,mLang to get current position

                marker = googleMap.addMarker(MarkerOptions().position(position!!).title("h1")) //add a marker to the map
                circle = googleMap.addCircle(
                    CircleOptions()
                        .center(LatLng(52.25, 21.0))
                        .radius(2000.0)
                        .strokeColor(Color.RED)
                        .fillColor(0x220000FF)
                )
                counter++
            }

            getCityName(49.631944,21.919167)


            val newLatLangZoom = CameraUpdateFactory.newLatLngZoom(position!!, zoom)//zoom in on the map
            googleMap.animateCamera(newLatLangZoom)//zooming animation

        }
    }

    //Do on the start when map is ready
    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        requestNewLocationData()

        //setting map markers OnClickListener and what should appear when clicked
        googleMap.setOnMarkerClickListener {
            marker->
            run {
                if (markers1km?.contains(marker) == true) {
                    shops1?.get(markers1km!!.indexOf(marker))?.let { showPopup(it) }
                }
                else if (markers2km?.contains(marker) == true) {
                    shops2?.get(markers2km!!.indexOf(marker))?.let { showPopup(it) }
                } else if (markers3km?.contains(marker) == true){
                    shops3?.get(markers3km!!.indexOf(marker))?.let { showPopup(it) }
                } else {

                }
            }
            true
        }
        setMarkers()
    }

    //popup whend marker clicked
    public fun showPopup(shop:Sklep){

        val popupBinding: PopupBinding = PopupBinding.inflate(LayoutInflater.from(binding.root.context))
        val popupView: View = popupBinding.root
        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)

        // Set the focusable property to true
        popupWindow.isFocusable = true

        popupBinding.name.text=shop.nazwa
        popupBinding.desc.text=shop.koordynaty.toString()
        popupView.setOnTouchListener { _, _ -> true }

// Show the popup window at a specific location on the screen
        popupWindow.showAtLocation(binding.root, Gravity.CENTER, 0, 0)
    }

    //Markers set
    private fun setMarkers(){
        var i: Double= 0.01
        // Add a marker with a custom icon
        val customMarkerIcon = BitmapFactory.decodeResource(resources, R.drawable.custom_marker)
        val customIcon = BitmapDescriptorFactory.fromBitmap(customMarkerIcon)
        //adding 8 markers for testing
        for( num in 0..7){
            val latlan=LatLng(52.25, 21.0+i)
            val shop = Sklep("Shop$num",latlan)
            val results = FloatArray(1)

            //checking the distance between us and the marker
            Location.distanceBetween(52.25, 21.0, 52.25, 21.0 + i,results)
            println(results[0])

            val marker2=googleMap.addMarker(MarkerOptions().position(shop.koordynaty).title(results[0].toString()).icon(customIcon)) //add a marker to the map

            if (marker2 != null) {
                markers?.add(marker2)
            }
            println(marker2)
            shops?.add( shop )

            println(markers)

            //checking the distance and adding to the list
            if(results[0]<=2000) {
                if (marker2 != null) {
                    markers1km?.add(marker2)
                    shops1?.add(shop)
                }
                marker2?.isVisible=true
            }
            else if(results[0]<=3000){
                if (marker2 != null) {
                    markers2km?.add(marker2)
                    shops2?.add(shop)
                }
                marker2?.isVisible=false
            }
            else if(results[0]<=5000){
                if (marker2 != null) {
                    markers3km?.add(marker2)
                    shops3?.add(shop)
                }
                marker2?.isVisible=false
            }
            else{
                marker2?.isVisible=false
            }

            i += 0.01
        }
    }

    //function for setting markers in range
    private fun setRangeMarkers(options:Int){
        println(markers)
        if(options==1){
            if(markers2km !=null){
                for(marker2 in markers2km!!){

                    marker2.isVisible=false
                }
            }

            if(markers3km !=null){
                for(marker3 in markers3km!!){
                    println(marker2)
                    marker3.isVisible=false
                }
            }
        }
        else if(options==2){
            if(markers2km!=null)
            {
                for(marker2 in markers2km!!){
                    marker2.isVisible=true
                }
            }

            if(markers3km !=null){
                for(marker3 in markers3km!!){
                    println(marker2)
                    marker3.isVisible=false
                }
            }
        }
        else if(options==3){
            if(markers2km!=null)
            {
                for(marker2 in markers2km!!){
                    marker2.isVisible=true
                }
            }
            if(markers3km!=null){
                for(marker3 in markers3km!!){
                    marker3.isVisible=true
                }
            }
        }
    }



    //get City name function
    private fun getCityName(lat: Double,long: Double): String? {
        var cityName: String?
        val geoCoder = Geocoder(this, Locale.getDefault())

        val address = geoCoder.getFromLocation(lat,long,1)
        cityName = address?.get(0)?.adminArea
        if (cityName == null){
            cityName = address?.get(1)?.locality
            if (cityName == null){
                cityName = address?.get(1)?.subAdminArea
            }
        }
        if (address != null) {
            println(address.get(0).getLocality())
        }
        return cityName
    }
}


