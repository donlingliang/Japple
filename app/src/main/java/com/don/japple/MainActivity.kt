package com.don.japple

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

import retrofit2.Callback
import android.widget.Toast
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants.PRECISION_6
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import retrofit2.Call
import retrofit2.Response
import timber.log.Timber



class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var mapboxMap: MapboxMap? = null
    private var dashLineDirectionsFeatureCollection: FeatureCollection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.access_token))
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync {
            this@MainActivity.mapboxMap = it

            val homePoint = Point.fromLngLat(-121.965350, 37.314870)
            it.setStyle(Style.LIGHT) { style ->

                style.addImage(
                    "marker-icon-id",
                    BitmapFactory.decodeResource(this.resources, R.drawable.mapbox_marker_icon_default)
                )

                val geoJsonSource = GeoJsonSource(
                    "source-id",
                    Feature.fromGeometry(homePoint)
                )
                style.addSource(geoJsonSource)

                val symbolLayer = SymbolLayer("layer-id", "source-id")
                symbolLayer.withProperties(PropertyFactory.iconImage("marker-icon-id"))

                style.addLayer(symbolLayer)

                initDottedLineSourceAndLayer(style)

                val point = Point.fromLngLat(-121.894824, 37.329292)
                getRoute(homePoint, point)
            }
        }

    }

    /**
     * Directions API call for route
     * */
    private fun getRoute(origin: Point, destination: Point) {

        val client: MapboxDirections = MapboxDirections.builder()
            .origin(origin)
            .destination(destination)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .accessToken(getString(R.string.access_token))
            .build()

        client.enqueueCall(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                if (response.body() == null) {
                    Timber.d("No routes found, make sure you set the right user and access token.")
                    return
                } else {
                    response.body()?.let {
                        if(it.routes().size < 1) {
                            Timber.d("No routes found")
                            return
                        }
                        drawNavigationPolylineRoute(it.routes()[0])
                    }
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
                Timber.d("Error: %s", throwable.message)
                if (throwable.message != "Coordinate is invalid: 0,0") {
                    Toast.makeText(
                        this@MainActivity,
                        "Error: " + throwable.message, Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    private fun drawNavigationPolylineRoute(route: DirectionsRoute) {
        mapboxMap?.let {
            it.getStyle {
                var directionsRouteFeatureList = ArrayList<Feature>()
                val string = route.geometry()
                if (string != null) {
                    var lineString = LineString.fromPolyline(string, PRECISION_6)

                    var coordinates = lineString.coordinates()

                    coordinates.forEach {
                        directionsRouteFeatureList.add(Feature.fromGeometry(LineString.fromLngLats(coordinates)))
                    }

                    dashLineDirectionsFeatureCollection = FeatureCollection.fromFeatures(directionsRouteFeatureList)
                    val geoJsonSource = it.getSourceAs<GeoJsonSource>("SOURCE_ID")
                    geoJsonSource?.let{
                        it.setGeoJson(dashLineDirectionsFeatureCollection)
                    }
                }
            }
        }
    }

    private fun initDottedLineSourceAndLayer(style: Style) {
        dashLineDirectionsFeatureCollection = FeatureCollection.fromFeatures(arrayOf<Feature>())
        style.addSource(GeoJsonSource("SOURCE_ID", dashLineDirectionsFeatureCollection))

        style.addLayerBelow(LineLayer(
            "DIRECTIONS_LAYER_ID", "SOURCE_ID")
            .withProperties(
                lineWidth(4.5f),
                lineColor(Color.BLACK),
                lineTranslate(arrayOf(0f, 4f)),
                lineDasharray(arrayOf(1.2f, 1.2f))
        ), "road-label-small")
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState?.let {
            mapView?.onSaveInstanceState(it)
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


}
