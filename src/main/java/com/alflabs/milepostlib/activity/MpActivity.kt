package com.alflabs.milepostlib.activity

import android.app.ActivityManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.alflabs.milepostlib.R
import com.alflabs.milepostlib.app.AppSettings
import com.alflabs.milepostlib.app.GlobalState
import com.alflabs.milepostlib.databinding.ActivityMpBinding
import com.alflabs.milepostlib.op.OperatorMode
import com.alflabs.milepostlib.service.ServiceMode
import com.alflabs.utils.ILogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Optional
import javax.inject.Inject

@AndroidEntryPoint
class MpActivity : AppCompatActivity() {

    companion object {
        private val TAG = MpActivity::class.java.simpleName
    }

    @Inject lateinit var globalState: GlobalState
    @Inject lateinit var appSettings: AppSettings
    @Inject lateinit var logger: ILogger
    @Inject lateinit var activityManagerOpt: Optional<ActivityManager>

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger.d(TAG, "onCreate")
        appSettings.initializeDefaults(lifecycleScope)

        binding = ActivityMpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            setupSystemBars(appSettings.hideSystemBars(), force = true)
            setupLockTask(appSettings.useLockTask())
        }

        appSettings.appSettingsDataStore.data.asLiveData().observe(this) { pref ->
            lifecycleScope.launch {
                setupSystemBars(appSettings.hideSystemBars(), force = false)
                setupLockTask(appSettings.useLockTask())
            }
        }

        setSupportActionBar(binding.toolbar)

        // Explanation on how a navigation graph & drawer menu works:
        // - nav items are defined in res/navigation/nav_graph_mp.xml
        //   - this defines fragments as nodes and transitions between these fragments.
        // - the activity layout contains a fragment host named "nav_host_fragment_content_main"
        //   which has an attribute app:navGraph="@navigation/nav_graph_mp".
        //   - alternatively the nav graph can set set programmatically:
        //     navController.setGraph(R.navigation.nav_graph_mp)
        // - this creates a "nav controller" which we can find here via the host id or from
        //   within the fragments.
        // - the activity layout contains a NavigationView which has an attribute
        //   app:menu="@menu/drawer_mp". This defines the visible menus on the side drawer.
        // - Drawer menu items have the same id as the navigation graph items, linking them together.

        navController = findNavController(R.id.nav_host_fragment_content_main)

        // When passing the navController.graph directly into the app bar configuration,
        // only the start destination is considered top level destinations.
        // Instead, we create a configuration where all the destinations are considered top level
        // destinations (with the hamburger icon).
        // val topLevelIds = navController.graph.mapNotNull { destination ->  destination.id }.toSet()
        val topLevelIds = setOf(
            R.id.nav_about,
            R.id.nav_audio_segment_list,
            R.id.nav_docent_track_list,
            R.id.nav_docent_track_play,
            R.id.nav_gps_track_list,
            R.id.nav_gps_track_record,
            R.id.nav_gps_track_play,
            R.id.nav_sensors,
            R.id.nav_permissions,
            R.id.nav_settings,
        )

        appBarConfiguration = AppBarConfiguration(topLevelIds, binding.drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        // This line links the navView (menu/drawer_mp.xml) to the navController for all
        // menu elements which match the same id in the nav_graph_mp.xml
        binding.navView.setupWithNavController(navController)

        // Dynamically update drawer menu items
        binding.navView.menu.findItem(R.id.nav_gps_track_play).isEnabled = false
        binding.drawerLayout.addDrawerListener(drawerListener)

        // Change the start page if the global state is already running
        logger.d(TAG, "Start page: ${globalState.serviceMode} ${globalState.operatorMode}")
        if (globalState.serviceMode == ServiceMode.RUNNING) {
            when (globalState.operatorMode) {
                OperatorMode.SENSORS -> navController.navigate(R.id.nav_sensors)
                OperatorMode.GPS_RECORD -> navController.navigate(R.id.nav_gps_track_record)
                OperatorMode.GPS_PLAYBACK -> navController.navigate(R.id.nav_gps_track_play)
                else -> { /* no-op */ }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration)
    }

    private fun setupLockTask(useLockTask: Boolean) {
        activityManagerOpt.ifPresent { activManager ->
            try {
                val lockTaskMode = activManager.lockTaskModeState

                val currentLockTask = when (lockTaskMode) {
                    ActivityManager.LOCK_TASK_MODE_LOCKED -> true
                    ActivityManager.LOCK_TASK_MODE_PINNED -> true
                    else -> false
                }

                logger.d(TAG, "@@ currentLockTask = $currentLockTask, useLockTask = $useLockTask")

                if (useLockTask != currentLockTask) {
                    if (useLockTask) {
                        startLockTask()
                    } else {
                        stopLockTask()
                    }
                }
            } catch (e: Exception) {
                logger.d(TAG, "@@ setupLockTask failed", e)
            }
        }
    }

    private fun setupSystemBars(hideSystemBars: Boolean, force: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        // Use ViewCompat to get the root insets from the decor view
        val insets = ViewCompat.getRootWindowInsets(window.decorView)

        // Check if the system bars (Status + Navigation) are currently visible
        val areStatusBarsVisible = insets?.isVisible(WindowInsetsCompat.Type.statusBars()) ?: true
        val areNavVarsVisible = insets?.isVisible(WindowInsetsCompat.Type.navigationBars()) ?: true
        val areBarsHidden = !areStatusBarsVisible && !areNavVarsVisible
        logger.d(TAG, "@@ hideSystemBars = $hideSystemBars, areBarsHidden = $areBarsHidden")

        // Only apply changes if the current state differs from the desired state
        if (hideSystemBars != areBarsHidden || force) {
            if (hideSystemBars) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                windowInsetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else if (!hideSystemBars) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    private fun updateNavMenuItems() {
        lifecycleScope.launch {
            val sensorItem = binding.navView.menu.findItem(R.id.nav_sensors)
            val recordItem = binding.navView.menu.findItem(R.id.nav_gps_track_record)
            val gpsPlaybackItem = binding.navView.menu.findItem(R.id.nav_gps_track_play)
            val docentPlaybackItem = binding.navView.menu.findItem(R.id.nav_docent_track_play)
            val gpsPlaybackTrackId = appSettings.gpsPlaybackTrackId()
            val docentPlaybackTrackId = appSettings.docentPlaybackTrackId()

            when (globalState.serviceMode) {
                ServiceMode.STOPPED -> {
                    sensorItem.isEnabled = true
                    recordItem.isEnabled = true
                    gpsPlaybackItem.isEnabled = gpsPlaybackTrackId != -1L
                    docentPlaybackItem.isEnabled = docentPlaybackTrackId != -1L
                }

                ServiceMode.RUNNING -> {
                    when (globalState.operatorMode) {
                        OperatorMode.IDLE, OperatorMode.SENSORS -> {
                            sensorItem.isEnabled = true
                            recordItem.isEnabled = true
                            gpsPlaybackItem.isEnabled = gpsPlaybackTrackId != -1L
                            docentPlaybackItem.isEnabled = docentPlaybackTrackId != -1L
                        }

                        OperatorMode.GPS_RECORD -> {
                            sensorItem.isEnabled = false
                            recordItem.isEnabled = true
                            gpsPlaybackItem.isEnabled = false
                            docentPlaybackItem.isEnabled = false
                        }

                        OperatorMode.GPS_PLAYBACK -> {
                            sensorItem.isEnabled = false
                            recordItem.isEnabled = false
                            gpsPlaybackItem.isEnabled = true
                            docentPlaybackItem.isEnabled = false
                        }

                        OperatorMode.DOCENT_PLAYBACK -> {
                            sensorItem.isEnabled = false
                            recordItem.isEnabled = false
                            gpsPlaybackItem.isEnabled = false
                            docentPlaybackItem.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    val drawerListener = object : DrawerLayout.SimpleDrawerListener() {
        override fun onDrawerStateChanged(newState: Int) {
            logger.d(TAG, "@@ onDrawerStateChanged: $newState")
            if (newState == DrawerLayout.STATE_SETTLING) {
                if (!binding.drawerLayout.isDrawerOpen(binding.navView)) {
                    // The drawer starts opening.
                    updateNavMenuItems()
                }
            }
        }
    }
}
