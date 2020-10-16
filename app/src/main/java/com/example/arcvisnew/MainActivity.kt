package com.example.arcvisnew

import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.data.Feature
import com.esri.arcgisruntime.data.ServiceFeatureTable
import com.esri.arcgisruntime.layers.FeatureLayer
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener
import com.esri.arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.symbology.ClassBreaksRenderer
import com.esri.arcgisruntime.symbology.ClassBreaksRenderer.ClassBreak
import com.esri.arcgisruntime.symbology.SimpleFillSymbol
import com.esri.arcgisruntime.symbology.SimpleLineSymbol
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private var mNavigationDrawerItemTitles: Array<String> = arrayOf(
        "Info",
        "Legende",
        "Disclaimer",
        "Datenschutzerklärung"
    )

    lateinit var mAlertDialog: AlertDialog.Builder

    private val mRefreshButton: ImageButton by lazy { findViewById(R.id.imageButton) }

    private val mStatusText: TextView by lazy{ findViewById(R.id.textView) }

    private val mDrawerToggle: ActionBarDrawerToggle by lazy { setupDrawer() }

    private val mClassBreakRen: ClassBreaksRenderer by lazy { createRenderer() }

    private val mMap: ArcGISMap by lazy {  ArcGISMap(
        Basemap.Type.DARK_GRAY_CANVAS_VECTOR,
        51.1657,
        10.4515,
        6
    )  }

    private val mFeatureTable: ServiceFeatureTable by lazy { ServiceFeatureTable(getString(R.string.url)) }

    private val mFeatureLayer: FeatureLayer by lazy { createFeatureLayer() }

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName
    }

    private fun createFeatureLayer(): FeatureLayer {
        val it = FeatureLayer(mFeatureTable)
        it.refreshInterval = 3600000L
        it.renderer = mClassBreakRen
        return it
    }

    private fun createRenderer(): ClassBreaksRenderer {
        val it = ClassBreaksRenderer()
        it.fieldName = "cases7_per_100k"

        it.classBreaks.add(createBreak(0.0, 0.0, Color.rgb(255, 255, 255), "0"))
        it.classBreaks.add(
            createBreak(
                0.00000001,
                4.999999,
                Color.rgb(255, 251, 206),
                "1"
            )
        )
        it.classBreaks.add(createBreak(5.0, 24.999999, Color.rgb(255, 249, 157), "2"))
        it.classBreaks.add(createBreak(25.0, 49.999999, Color.rgb(249, 178, 0), "3"))
        it.classBreaks.add(createBreak(50.0, 99.999999, Color.rgb(211, 40, 55), "4"))
        it.classBreaks.add(createBreak(100.0, 500.0, Color.rgb(168, 0, 24), "5"))
        return it
    }

    private fun createBreak(min: Double, max: Double, color: Int, label: String): ClassBreak {
        var symbol = SimpleFillSymbol(
            SimpleFillSymbol.Style.SOLID, color,
            SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLACK, 1.0f)
        )
        val classBreak = ClassBreak()
        classBreak.label = label
        classBreak.symbol = symbol
        classBreak.minValue = min
        classBreak.maxValue = max

        return classBreak
    }

    /**
     * Add navigation drawer items
     */
    private fun addDrawerItems() {
        ArrayAdapter(this, android.R.layout.simple_list_item_1, mNavigationDrawerItemTitles).apply {
            drawerList.adapter = this
            drawerList.onItemClickListener =
                AdapterView.OnItemClickListener { _, _, position, _ -> onMenuClick(
                    position
                ) }
        }
    }

    private fun onMenuClick(position: Int) {

        val textArray = arrayOf(
            getString(R.string.info),
            getString(R.string.caption),
            getString(R.string.disclaimer),
            getString(R.string.policy),
        )
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle(mNavigationDrawerItemTitles[position])
        dialog.setMessage(textArray[position])
        dialog.setPositiveButton(android.R.string.yes) { _, _ -> }
        drawerLayout.closeDrawer(Gravity.LEFT)
        dialog.show()
    }

    /**
     * Set up the navigation drawer
     */
    private fun setupDrawer(): ActionBarDrawerToggle {
        return object :
            ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {

            override fun isDrawerIndicatorEnabled() = true

            /** Called when a drawer has settled in a completely open state.  */
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state.  */
            override fun onDrawerClosed(view: View) {
                super.onDrawerClosed(view)
                invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
            }
        }
    }

    private fun formatFeature(feature: Feature):String {
        val attributes = feature.attributes
        var txt =
            "Einwohnerzahl: " + attributes.get("EWZ").toString() + "\n"
        txt += "Bundesland: " + attributes.get("BL").toString() + "\n"
        txt += "Fälle: " + attributes.get("cases").toString() + "\n"
        txt += "Todesfälle: " + attributes.get("deaths").toString() + "\n"
        txt += "7 Tage/100k EW: " + attributes.get("cases7_per_100k")
            .toString().toFloat().toInt().toString()
        return txt;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getActionBar()?.setTitle(R.string.app_name)
        getSupportActionBar()?.setTitle(R.string.app_name)
        setContentView(R.layout.activity_main)

        addDrawerItems()
        drawerLayout.addDrawerListener(mDrawerToggle)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

        mAlertDialog = AlertDialog.Builder(this)
        mapView.visibility = MapView.INVISIBLE

        ArcGISRuntimeEnvironment.setLicense(getString(R.string.license))
        mMap.addDoneLoadingListener() {
                mStatusText.text = getString(R.string.loading)
                if (mMap.loadStatus == LoadStatus.LOADED) {
                    mRefreshButton.visibility = ImageButton.INVISIBLE
                    mapView.visibility = MapView.VISIBLE

                    mFeatureLayer.addDoneLoadingListener() {
                        if (mFeatureLayer.loadStatus ==  LoadStatus.LOADED) {
                            showMessage("RKI Daten geladen")
                            mMap.operationalLayers.add(mFeatureLayer)

                            mapView.let {
                                // set the map to be displayed in the layout's map view
                                mapView.map = mMap
                                // give any item selected on the map view a red selection halo
                                it.selectionProperties.color = Color.RED
                                // set an on touch listener on the map view
                                it.onTouchListener = object : DefaultMapViewOnTouchListener(
                                    this,
                                    it
                                ) {
                                    override fun onSingleTapConfirmed(motionEvent: MotionEvent): Boolean {
                                        // clear the previous selection
                                        mFeatureLayer.clearSelection()
                                        // get the point that was tapped and convert it to a point in map coordinates
                                        val tappedPoint =
                                            android.graphics.Point(
                                                motionEvent.x.roundToInt(),
                                                motionEvent.y.roundToInt()
                                            )
                                        // set a tolerance for accuracy of returned selections from point tapped
                                        val tolerance = 1.0

                                        val identifyLayerResultFuture =
                                            mapView.identifyLayerAsync(
                                                mFeatureLayer,
                                                tappedPoint,
                                                tolerance,
                                                false,
                                                -1
                                            )
                                        identifyLayerResultFuture.addDoneListener {
                                            try {
                                                val identifyLayerResult =
                                                    identifyLayerResultFuture.get()
                                                // get the elements in the selection that are features
                                                val features =
                                                    identifyLayerResult.elements.filterIsInstance<Feature>()
                                                if (features.isNotEmpty()) {
                                                    val feature = features[0]
                                                    mFeatureLayer.selectFeature(feature)

                                                    val txt = formatFeature(feature)
                                                    mAlertDialog.setTitle(
                                                        feature.attributes.get("county").toString()
                                                    )
                                                    mAlertDialog.setMessage(txt)
                                                    mAlertDialog.setPositiveButton(android.R.string.yes) { _, _ ->
                                                    }
                                                    mAlertDialog.show()
                                                }
                                            } catch (e: Exception) {
                                                val errorMessage = "Select feature failed: " + e.message
                                                Log.e(TAG, errorMessage)
                                                Toast.makeText(
                                                    applicationContext,
                                                    errorMessage,
                                                    Toast.LENGTH_LONG
                                                )
                                                    .show()
                                            }
                                        }
                                        return super.onSingleTapConfirmed(motionEvent)
                                    }
                                }
                            }
                        } else {
                            showMessage("failed loading features")
                        }
                    }
                    mFeatureLayer.loadAsync()
                } else {
                    showMessage(getString(R.string.failure))
                    mStatusText.text = getString(R.string.failed)
                    mRefreshButton.visibility = ImageButton.VISIBLE
                    mapView.visibility = MapView.INVISIBLE
                }
            }
        mMap.loadAsync()
    }

    fun onRetryButtonClick(v:View) {
        try {
            mMap.retryLoadAsync()
        } catch (e: java.lang.Exception) {
          Log.e(TAG, e.message.toString())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // R.menu.mymenu is a reference to an xml file named mymenu.xml which should be inside your res/menu directory.
        // If you don't have res/menu, just create a directory named "menu" inside res
        // menuInflater.inflate(R.menu.mymenu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.detail_refresh_btn) {
            // do something here
            showMessage("you clicked")
        }
        // Activate the navigation drawer toggle
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mDrawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mDrawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onPause() {
        mapView.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapView.resume()
    }

    override fun onDestroy() {
        mapView.dispose()
        super.onDestroy()
    }

    private fun showMessage(text: String) {
        val toast = Toast.makeText(applicationContext, text, Toast.LENGTH_LONG)
        toast.show()
    }

}