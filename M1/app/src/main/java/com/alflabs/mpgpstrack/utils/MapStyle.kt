package com.alflabs.mpgpstrack.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.alflabs.mpgpstrack.R
import com.alflabs.utils.ILogger
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MapStyleOptions

object MapStyle {
    private val TAG = MapStyle::class.java.simpleName

    fun applyMapStyle(context: Context,
                      map: GoogleMap,
                      logger: ILogger) {
        // Google Maps is stylized using a JSON file. Doc with night style examples:
        // https://developers.google.com/maps/documentation/javascript/examples/style-array

        // Only apply dark theme when in night mode
        val configuration = context.resources.configuration
        val currentNightMode = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (currentNightMode != Configuration.UI_MODE_NIGHT_YES) {
            return
        }

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    context, R.raw.map_style_night
                )
            )
            if (!success) {
                logger.d(TAG, "Map Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            logger.d(TAG, "Map Style: Can't find style. Error: ", e)
        }
    }

}
