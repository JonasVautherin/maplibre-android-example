package com.example.maplibre

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.maplibre.ui.theme.MapLibreExampleTheme
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MapLibreExampleTheme {
                val map = rememberMapViewWithLifecycle()
                val coroutineScope = rememberCoroutineScope()

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { map },
                    update = { mapView ->
                        coroutineScope.launch {
                            val maplibre = mapView.awaitMap()
                            val key = getMapLibreKey()
                            maplibre.setStyle("https://api.maptiler.com/maps/satellite/style.json?key=${key}")
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun rememberMapViewWithLifecycle(): MapView {
        val context = LocalContext.current
        Mapbox.getInstance(context)
        val mapView = remember { MapView(context) }

        // Makes MapView follow the lifecycle of this composable
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle, mapView) {
            val lifecycleObserver = getMapLifecycleObserver(mapView)
            lifecycle.addObserver(lifecycleObserver)
            onDispose {
                lifecycle.removeObserver(lifecycleObserver)
            }
        }

        return mapView
    }

    private fun getMapLifecycleObserver(mapView: MapView): LifecycleEventObserver =
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> throw IllegalStateException()
            }
        }

    private suspend inline fun MapView.awaitMap(): MapboxMap =
        suspendCoroutine { continuation ->
            getMapAsync {
                continuation.resume(it)
            }
        }

    private fun getMapLibreKey(): String {
        val key = resources.getString(R.string.maplibreKey)

        if (key.isEmpty()) {
            throw RuntimeException(
                "Failed to find maplibreKey. Did you define 'MAPLIBRE_KEY' in keystore.properties?"
            )
        }

        return key
    }
}
