package eu.pl.snk.senseibunny.test3

import android.content.Context
import android.location.Geocoder
import java.util.Locale

class getUserCountry(private val context: Context, private val latitude:Double, private val longitude:Double) {

    private  val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    //private lateinit var adressListener: AddressListener
}