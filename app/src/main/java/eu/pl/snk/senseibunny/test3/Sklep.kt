package eu.pl.snk.senseibunny.test3

import com.google.android.gms.maps.model.LatLng

class Sklep(nazwa: String, koordynaty: LatLng) {

    public var koordynaty: LatLng
    public var nazwa: String

    init {
        this.nazwa = nazwa
        this.koordynaty = koordynaty
    }

}