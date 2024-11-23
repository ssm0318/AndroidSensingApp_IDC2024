package com.example.drg2023

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.work.*
import com.example.drg2023.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.security.Permission
import java.util.*


class FloatingButtonLifecycleListener : Application.ActivityLifecycleCallbacks {
    private var floatingButtonService: FloatingButtonService? = null

    fun setFloatingButtonService(service: FloatingButtonService?) {
        floatingButtonService = service
    }

    override fun onActivityResumed(activity: Activity) {
        floatingButtonService?.resetButtonSize()
    }

    // Other methods from the interface
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}

data class FormResponse(
    val parentEmotion: String,
    val question1: List<String> = listOf(),
    val question1Other: String = "",
//    val question2: String = "",
    val question_feature: String = "",
    val question_feature_other: String = "",
    val question_engagement: String = "",
    val question_engagement_other: String = "",
    val question4: String = "",
    val question5: String = "",
    val question6: String = ""
)

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener{
    companion object {
        const val REQUEST_PERMISSION_ALL = 100
        const val REQUEST_PERMISSION_USAGE_STATS = 101
        const val REQUEST_PERMISSION_NOTIFICATION = 102
        const val REQUEST_CODE_OVERLAY_PERMISSION = 1
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var headerView: View
    private val lifecycleListener = FloatingButtonLifecycleListener()

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var database: DatabaseReference
    private lateinit var question0: RadioGroup
//    private lateinit var question2: EditText
    private lateinit var question4: EditText
    private lateinit var question5: RadioGroup
    private lateinit var question6: RadioGroup
    private lateinit var question_feature: RadioGroup
    private lateinit var question_engagement: RadioGroup
    private lateinit var submitButton: Button
    private lateinit var parentEmotion: String

    private val emotionReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val emotion = intent.getStringExtra("emotion")
            val emotionImageView = findViewById<ImageView>(R.id.emotion_image_view)

            val imageResId = when (emotion) {
                "Excited" -> R.drawable.excited
                "Calm" -> R.drawable.calm
                "Sad" -> R.drawable.sad
                "Frustrated" -> R.drawable.frustrated
                else -> return
            }

            emotionImageView.setImageResource(imageResId)
            val emotionMap = mapOf<String, String>(
                "Excited" to "high energy, high pleasantness",
                "Calm" to "low energy, high pleasantness",
                "Sad" to "low energy, low pleasantness",
                "Frustrated" to "high energy, low pleasantness"
            )
            parentEmotion = emotionMap.get(emotion).toString()

            val radioButtonId = when (emotion) {
                "Excited" -> R.id.excited_button
                "Calm" -> R.id.calm_button
                "Sad" -> R.id.sad_button
                "Frustrated" -> R.id.frustrated_button
                else -> return
            }

            val radioButton = findViewById<RadioButton>(radioButtonId)
            radioButton.isChecked = true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        application.registerActivityLifecycleCallbacks(lifecycleListener)
        registerReceiver(emotionReceiver, IntentFilter("com.example.drg2023.EMOTION_SELECTED"))

        database = FirebaseDatabase.getInstance().reference

        checkPermission()

        //start daily notification service
//        val notificationServiceIntent = Intent(this, NotificationService::class.java)
//        startService(notificationServiceIntent)

        if (checkOverlayPermission()) {
            startFloatingButtonService()
        } else {
            requestOverlayPermission()
        }

        // update selected emotion text
        val sharedPref1 = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val chosenAppPackageName = sharedPref1.getString("chosenApp", "") ?: "<app>"
        val appName = when(chosenAppPackageName) {
            "com.google.android.apps.maps" -> "Instagram"
            "com.android.chrome" -> "Instagram"
            "com.instagram.android" -> "Instagram"
            "com.zhiliaoapp.musically" -> "TikTok"
            "com.snapchat.android" -> "Snapchat"
            else -> ""
        }
        val chosenAppString = getString(R.string.selected_emotion, appName)
        val textView = findViewById<TextView>(R.id.selected_emotion)
        textView.text = chosenAppString

//        val whatView = findViewById<TextView>(R.id.question_what)
//        val whatString = getString(R.string.question_what, appName)
//        whatView.text = whatString

        val choiceView = findViewById<TextView>(R.id.question_choice)
        val choiceString = getString(R.string.question_choice, appName)
        choiceView.text = choiceString

        val featureString = getString(R.string.what_were_you_interacting_with, appName)
        val featureView = findViewById<TextView>(R.id.question_feature)
        featureView.text = featureString

        val engagementString = getString(R.string.how_were_you_interacting, appName)
        val engagementView = findViewById<TextView>(R.id.question_engagement)
        engagementView.text = engagementString

        // update uuid
        val sharedPref2 = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        var userId = sharedPref2.getString(getString(R.string.user_id), null)
        if (userId==null) {
            userId = UUID.randomUUID().toString()
            with(sharedPref2.edit()) {
                putString(getString(R.string.user_id) ,userId)
                commit()
            }
        }

        drawerLayout = binding.drawerLayout
        navigationView = binding.navView
        navigationView.setNavigationItemSelectedListener(this)

        // Add a hamburger icon to the app bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        // Set up the ActionBarDrawerToggle
        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        headerView = binding.navView.getHeaderView(0)

        val tvUuid = headerView.findViewById<TextView>(R.id.tv_uuid)
        tvUuid.text = "ID: $userId"

        updateAppChoice()
        updateTrackerStatus()

//        question2 = findViewById(R.id.question_what_input)
        question4 = findViewById(R.id.question_why_input)
        question5 = findViewById(R.id.question_5)
        question6 = findViewById(R.id.question_6)
        question_feature = findViewById(R.id.select_feature)
        question_engagement = findViewById(R.id.select_engagement)

        submitButton = findViewById(R.id.submit_button)

        submitButton.setOnClickListener {
            submitForm(userId)
        }
        setLayoutBackgroundColor()

        if (Build.VERSION.SDK_INT <= 32) {
            val intent = Intent(this, AppUsageMonitorService::class.java)
            startService(intent)
        } else {
            if (!checkPostNotificationPermission()) {
                requestPostNotificationPermission()
            } else {
                val intent = Intent(this, AppUsageMonitorService::class.java)
                startService(intent)
            }
        }

        val formScrollView = findViewById<ScrollView>(R.id.form_scroll_view)

        formScrollView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {

                val view = currentFocus
                if (view is EditText) {
                    val rect = Rect()
                    view.getGlobalVisibleRect(rect)
                    val x = event.rawX.toInt()
                    val y = event.rawY.toInt()
                    if (!rect.contains(x, y)) {
                        hideKeyboard(view)
                        view.clearFocus()
                    }
                }
            }
            false
        }
    }

    private fun updateAppChoice() {
        var chosenApp = headerView.findViewById<TextView>(R.id.chosenApp)
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val chosenAppPackageName = sharedPref.getString("chosenApp", "") ?: ""
        val appName = when(chosenAppPackageName) {
            "com.google.android.apps.maps" -> "Instagram"
            "com.android.chrome" -> "Instagram"
            "com.instagram.android" -> "Instagram"
            "com.zhiliaoapp.musically" -> "TikTok"
            "com.snapchat.android" -> "Snapchat"
            else -> ""
        }
        chosenApp.text = appName

        var chosenPackage = headerView.findViewById<TextView>(R.id.chosenPackage)
        chosenPackage.text = chosenAppPackageName

        val chosenAppString = getString(R.string.selected_emotion, appName)
        val textView = findViewById<TextView>(R.id.selected_emotion)
        textView.text = chosenAppString

//        val whatString = getString(R.string.question_what, appName)
//        val whatView = findViewById<TextView>(R.id.question_what)
//        whatView.text = whatString

        val featureString = getString(R.string.what_were_you_interacting_with, appName)
        val featureView = findViewById<TextView>(R.id.question_feature)
        featureView.text = featureString

        val engagementString = getString(R.string.how_were_you_interacting, appName)
        val engagementView = findViewById<TextView>(R.id.question_engagement)
        engagementView.text = engagementString

        val choiceString = getString(R.string.question_choice, appName)
        val choiceView = findViewById<TextView>(R.id.question_choice)
        choiceView.text = choiceString
    }

    private fun updateTrackerStatus() {
        var trackerStatusText = headerView.findViewById<TextView>(R.id.trackerStatus)
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val trackerStatus = sharedPref.getString("trackerStatus", "") ?: ""
        trackerStatusText.text = trackerStatus
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun submitForm(userId: String) {
        // get the emotion button that they pressed
        val radioGroup = findViewById<RadioGroup>(R.id.question_0)
        val checkedRadioButtonId = radioGroup.checkedRadioButtonId
        val checkedRadioButton = findViewById<RadioButton>(checkedRadioButtonId)

        val selectedEmotions = mutableListOf<String>()
        val checkBoxIds = listOf(
            R.id.amused, R.id.fun_loving, R.id.silly,
            R.id.proud, R.id.confident, R.id.self_assured,
            R.id.inspired, R.id.uplifted, R.id.elevated,
            R.id.interested, R.id.alert, R.id.curious,
            R.id.joyful, R.id.glad, R.id.happy,

            R.id.hopeful, R.id.optimistic, R.id.encouraged,
            R.id.serene, R.id.content, R.id.peaceful,
            R.id.grateful, R.id.appreciative, R.id.thankful,
            R.id.embarrassed, R.id.self_conscious, R.id.blushing,
            R.id.love, R.id.closeness, R.id.trust,

            R.id.bored, R.id.tired, R.id.addicted,
            R.id.ashamed, R.id.humiliated, R.id.disgraced,
            R.id.guilty, R.id.repentant, R.id.blameworthy,
            R.id.jealous, R.id.inferior, R.id.insecure,
            R.id.sad, R.id.downhearted, R.id.unhappy,

            R.id.stressed, R.id.nervous, R.id.overwhelmed,
            R.id.scared, R.id.fearful, R.id.afraid,
            R.id.contemptuous, R.id.scornful, R.id.disdainful,
            R.id.angry, R.id.irritated, R.id.annoyed,
            R.id.disgust, R.id.distaste, R.id.revulsion,
            R.id.hate, R.id.distrust, R.id.suspicion
        )

        for (id in checkBoxIds) {
            val checkBox = findViewById<CheckBox>(id)
            if (checkBox.isChecked) {
                selectedEmotions.add(checkBox.text.toString())
            }
        }

        val otherEmotion = findViewById<EditText>(R.id.other_emotion).text.toString()

        val selectedRadioButtonIdFeature = question_feature.checkedRadioButtonId
        val selectedRadioButtonFeature = findViewById<RadioButton>(selectedRadioButtonIdFeature)

        val selectedRadioButtonIdEngagement = question_engagement.checkedRadioButtonId
        val selectedRadioButtonEngagement = findViewById<RadioButton>(selectedRadioButtonIdEngagement)
        val answer4 = question4.text.toString()

        val selectedRadioButtonId5 = question5.checkedRadioButtonId
        val selectedRadioButton5 = findViewById<RadioButton>(selectedRadioButtonId5)

        val selectedRadioButtonId6 = question6.checkedRadioButtonId
        val selectedRadioButton6 = findViewById<RadioButton>(selectedRadioButtonId6)

        val timestamp: Date = Date()

        // Store the answers in Firebase under the associated userId
        val userId = userId // Replace this with the actual userId

        val isFormIncomplete = (selectedEmotions.isEmpty() && TextUtils.isEmpty(otherEmotion)) ||
                selectedRadioButtonIdFeature == -1 ||
                selectedRadioButtonIdEngagement == -1 || TextUtils.isEmpty(answer4) ||
                selectedRadioButtonId5 == -1 || selectedRadioButtonId6 == -1 || checkedRadioButtonId == -1
        // Check if the questions are answered

        if (isFormIncomplete) {
            Toast.makeText(this, "Please answer all questions", Toast.LENGTH_SHORT).show()
            return
        }

        val feature = selectedRadioButtonFeature.text.toString()
        val otherFeature = findViewById<EditText>(R.id.other_feature).text.toString()
        val engagement = selectedRadioButtonEngagement.text.toString()
        val otherEngagement = findViewById<EditText>(R.id.other_engagement).text.toString()
        val answer5 = selectedRadioButton5.text.toString()
        val answer6 = selectedRadioButton6.text.toString()
        val checkedRadioButtonText = checkedRadioButton.text.toString()

        val emotionMap = mapOf<String, String>(
            "\uD83E\uDD29" to "high energy, high pleasantness",
            "\uD83D\uDE0C" to "low energy, high pleasantness",
            "\uD83D\uDE14" to "low energy, low pleasantness",
            "\uD83D\uDE21" to "high energy, low pleasantness"
        )
        parentEmotion = emotionMap.get(checkedRadioButtonText).toString()

        // Store the FormResponse object in Firebase under the associated userId
        val formResponse = FormResponse(parentEmotion, selectedEmotions, otherEmotion,
            feature, otherFeature, engagement, otherEngagement, answer4, answer5, answer6)

        val formResponseRef = database.child("responses").child(userId).child(timestamp.toString())
        formResponseRef.setValue(formResponse)
            .addOnSuccessListener {
                // Show a success message
                Toast.makeText(this, "Answers submitted successfully!", Toast.LENGTH_SHORT).show()
                resetForm()
            }
            .addOnFailureListener { e ->
                // Show an error message
                Toast.makeText(this, "Failed to submit form: ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }

    private fun resetForm() {
        val formScrollView = findViewById<ScrollView>(R.id.form_scroll_view)

        formScrollView.postDelayed({ formScrollView.scrollTo(0, 0) }, 100)
        // question 0
        val question0Layout = findViewById<RadioGroup>(R.id.question_0)
        question0Layout.clearCheck()

        val question1Layout = findViewById<LinearLayout>(R.id.question_1)
        for (i in 0 until question1Layout.childCount) {
            val child = question1Layout.getChildAt(i)
            if (child is CheckBox) {
                child.isChecked = false
            } else if (child is LinearLayout) {
                for (j in 0 until child.childCount) {
                    val subChild = child.getChildAt(j)
                    if (subChild is CheckBox) {
                        subChild.isChecked = false
                    }
                }
            }
        }

        val otherEmotion = findViewById<EditText>(R.id.other_emotion)
        otherEmotion.setText("")

//        val question2 = findViewById<EditText>(R.id.question_what_input)
//        question2.setText("")

        val questionFeatureRadioGroup = findViewById<RadioGroup>(R.id.select_feature)
        questionFeatureRadioGroup.clearCheck()
        val otherFeature = findViewById<EditText>(R.id.other_feature)
        otherFeature.setText("")

        val questionEngagementRadioGroup = findViewById<RadioGroup>(R.id.select_engagement)
        questionEngagementRadioGroup.clearCheck()
        val otherEngagement = findViewById<EditText>(R.id.other_engagement)
        otherEngagement.setText("")

        val question4 = findViewById<EditText>(R.id.question_why_input)
        question4.setText("")

        val question5RadioGroup = findViewById<RadioGroup>(R.id.question_5)
        question5RadioGroup.clearCheck()

        val question6RadioGroup = findViewById<RadioGroup>(R.id.question_6)
        question6RadioGroup.clearCheck()
    }

    private fun requestDisableBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            startActivity(intent)
        }
    }

    private val shouldAskForBatteryOptimizationDisable: Boolean
        get() {
            var shouldAsk = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    shouldAsk = true
                }
            }
            return shouldAsk
        }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        }
    }

    private fun checkPostNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT > 32) {
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestPostNotificationPermission() {
        try {
            if (Build.VERSION.SDK_INT > 32) {
                ActivityCompat.requestPermissions(
                    this, arrayOf<String>(permission.POST_NOTIFICATIONS),
                    112
                )
            }
        } catch (e: Exception) {
        }
    }

    fun startFloatingButtonService() {
        val intent = Intent(this, FloatingButtonService::class.java)
        startService(intent)
    }

    fun startService() {
        Intent(this, TrackerService::class.java).also { intent ->
            startForegroundService(intent)
        }
    }

    private fun setLayoutBackgroundColor() {
        with (checkAllPermission()) {
            if (this.isNotEmpty()) {
                binding.layoutMain.setBackgroundColor(Color.parseColor("#FF6347"))
                val tvPermissions = headerView.findViewById<TextView>(R.id.tv_permissions)
                tvPermissions.text = this.joinToString()
            } else {
                binding.layoutMain.setBackgroundColor(Color.parseColor("#FFFFFF"))
                val tvPermissions = headerView.findViewById<TextView>(R.id.tv_permissions)
                tvPermissions.text = ""
            }
        }
    }

//    check if all requested permission is granted
    private fun checkAllPermission(): Array<String> {
        var res = arrayListOf<String>()
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        val appOpsManager: AppOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName)
        } else {
            appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName)
        }

        if (mode!=AppOpsManager.MODE_ALLOWED)
            res.add("Usage Stats")

        val sets = NotificationManagerCompat.getEnabledListenerPackages(this)
        if (!sets.contains(packageName))
            res.add("Noti Listener")

        return res.toTypedArray()
    }

    private fun checkPermission() {
        checkUsageStatsPermission()
        checkNotificationPermission()
    }

    private fun checkUsageStatsPermission() {
        //        Permission for App Usage: USAGE_STATS
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        val appOpsManager: AppOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName)
        } else {
            appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName)
        }

        if (mode!=AppOpsManager.MODE_ALLOWED) {
            Toast.makeText(this, "설정에서 권한을 허용하십시오", Toast.LENGTH_LONG).show()
            startActivityForResult(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS), REQUEST_PERMISSION_USAGE_STATS)
        }
    }

    private fun checkNotificationPermission() {
//        Permission for Notification: NOTIFICATION_LISTENER_SERVICE
        val sets = NotificationManagerCompat.getEnabledListenerPackages(this)
        if (!sets.contains(packageName)) {
            startActivityForResult(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS), REQUEST_PERMISSION_NOTIFICATION)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestPermissionAllVersionQ() {
        val permissions = listOf(permission.READ_PHONE_STATE, permission.READ_CALL_LOG,
            permission.RECEIVE_SMS, permission.READ_SMS, permission.ACTIVITY_RECOGNITION)
        val permissionAccessCoarseLocationApproved = ActivityCompat
            .checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val backgroundLocationPermissionApproved = ActivityCompat
            .checkSelfPermission(this, permission.ACCESS_BACKGROUND_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        if (checkSelfPermissions(permissions)) {
            if (!permissionAccessCoarseLocationApproved) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        permission.ACCESS_FINE_LOCATION)) {
                    ActivityCompat.requestPermissions(this,
                        arrayOf(permission.ACCESS_FINE_LOCATION, permission.ACCESS_BACKGROUND_LOCATION),
                        REQUEST_PERMISSION_ALL
                    )
                } else {
                    ActivityCompat.requestPermissions(this,
                        arrayOf(permission.ACCESS_FINE_LOCATION, permission.ACCESS_BACKGROUND_LOCATION),
                        REQUEST_PERMISSION_ALL
                    )
                }
            } else {
                if (!backgroundLocationPermissionApproved) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(permission.ACCESS_BACKGROUND_LOCATION),
                        REQUEST_PERMISSION_ALL
                    )
                } else {
                    startService()
                }
            }
        } else {
            var requestPermission = getPermissionNotApproved(permissions)
            if (permissionAccessCoarseLocationApproved) {
                if (!backgroundLocationPermissionApproved) {
                    requestPermission.add(permission.ACCESS_BACKGROUND_LOCATION)
                }
            } else {
                requestPermission.add(permission.ACCESS_FINE_LOCATION)
                requestPermission.add(permission.ACCESS_BACKGROUND_LOCATION)
            }
            if (checkPermissionRationale(requestPermission)) {
                ActivityCompat.requestPermissions(this, requestPermission.toTypedArray(), REQUEST_PERMISSION_ALL)
            } else {
                ActivityCompat.requestPermissions(this, requestPermission.toTypedArray(), REQUEST_PERMISSION_ALL)
            }
        }
    }

    fun requestPermissionAll() {
        val permissions = listOf<String>(permission.READ_PHONE_STATE, permission.READ_CALL_LOG,
            permission.RECEIVE_SMS, permission.READ_SMS, permission.ACCESS_FINE_LOCATION)
        if (!checkSelfPermissions(permissions)) {
            var requestPermission = getPermissionNotApproved(permissions)
            if (checkPermissionRationale(permissions)) {
                ActivityCompat.requestPermissions(this, requestPermission.toTypedArray(), REQUEST_PERMISSION_ALL)
            } else {
                ActivityCompat.requestPermissions(this, requestPermission.toTypedArray(), REQUEST_PERMISSION_ALL)
            }
        } else {
            startService()
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSION_ALL -> {
                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q) {
                    if (containsPermission(permissions, permission.ACCESS_FINE_LOCATION)
                        && grantResults.isNotEmpty() && !checkPermissionGranted(grantResults)) {
                        requestPermissionAllVersionQ()
                    } else if (grantResults.isNotEmpty() && !checkPermissionGranted(grantResults)) {
                        var requestPermission = ArrayList<String>()
                        getPermissionNotApproved(grantResults).forEach{
                            requestPermission.add(permissions[it])
                        }
                        ActivityCompat.requestPermissions(this, requestPermission.toTypedArray(), REQUEST_PERMISSION_ALL)
                    } else {
                        startService()
                    }
                } else {
                    if (grantResults.isNotEmpty() && !checkPermissionGranted(grantResults)) {
                        var requestPermission = ArrayList<String>()
                        getPermissionNotApproved(grantResults).forEach{
                            requestPermission.add(permissions[it])
                        }
                        ActivityCompat.requestPermissions(this, requestPermission.toTypedArray(), REQUEST_PERMISSION_ALL)
                    } else {
                        startService()
                    }
                }
            }
            112 -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    val intent = Intent(this, AppUsageMonitorService::class.java)
                    startService(intent)
                } else {
                    requestPostNotificationPermission()
                }
            }
            else -> {

            }
        }
        setLayoutBackgroundColor()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_PERMISSION_USAGE_STATS -> {
                checkUsageStatsPermission()
            }
            REQUEST_PERMISSION_NOTIFICATION -> {
                checkNotificationPermission()
            }
            REQUEST_CODE_OVERLAY_PERMISSION -> {
                if (checkOverlayPermission()) {
                    startFloatingButtonService()
                }
            }
            else -> {

            }
        }
        setLayoutBackgroundColor()
    }

    private fun checkSelfPermissions(permissions: List<String>): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission)!=PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun checkPermissionRationale(permissions: List<String>): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission))
                return false
        }
        return true
    }

    private fun getPermissionNotApproved(permissions: List<String>): ArrayList<String> {
        var res = arrayListOf<String>()
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission)!=PackageManager.PERMISSION_GRANTED)
                res.add(permission)
        }
        return res
    }

    private fun getPermissionNotApproved(grantResults: IntArray): ArrayList<Int> {
        var res = ArrayList<Int>()
        grantResults.forEachIndexed{i, element ->
            if (element!=PackageManager.PERMISSION_GRANTED)
                res.add(i)
        }
        return res
    }

    private fun checkPermissionGranted(grantResults: IntArray): Boolean {
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED)
                return false
        }
        return true
    }

    private fun containsPermission(permissions: Array<String>, permission: String): Boolean {
        for (p in permissions) {
            if (p==permission)
                return true
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Pass the event to ActionBarDrawerToggle
        // If it returns true, then it has handled the app icon touch event
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // Handling navigation item clicks
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_tracker -> {
                Intent(this, TrackerService::class.java).also { intent ->
                    startForegroundService(intent)
                }
                val sharedPrefApp = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                with(sharedPrefApp.edit()) {
                    putString("trackerStatus", "Tracker Status: Running!")
                    apply()
                }
            }
            R.id.menu_stop_tracker -> {
                Intent(this, TrackerService::class.java).also { intent ->
                    stopService(intent)
                }
                val sharedPrefApp = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                with(sharedPrefApp.edit()) {
                    putString("trackerStatus", "Tracker Status: Stopped")
                    apply()
                }
            }
            R.id.menu_instagram -> {
//                val packageName = "com.google.android.apps.maps"
//                val packageName = "com.android.chrome"
                val packageName = "com.instagram.android"
                saveToSharedPreferences(packageName)
                Toast.makeText(this, "Instagram selected", Toast.LENGTH_SHORT).show()
            }
            R.id.menu_tiktok -> {
                val packageName = "com.zhiliaoapp.musically"
                saveToSharedPreferences(packageName)
                Toast.makeText(this, "TikTok selected", Toast.LENGTH_SHORT).show()
            }
            R.id.menu_snapchat -> {
                val packageName = "com.snapchat.android"
                saveToSharedPreferences(packageName)
                Toast.makeText(this, "Snapchat selected", Toast.LENGTH_SHORT).show()
            }
        }

        updateTrackerStatus()
        updateAppChoice()

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun saveToSharedPreferences(packageName: String) {
        val sharedPrefApp = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPrefApp.edit()) {
            putString("chosenApp", packageName)
            apply()
        }

        val sharedPrefUser = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val userId = sharedPrefUser.getString(getString(R.string.user_id), null)

        val appName = when(packageName) {
            "com.google.android.apps.maps" -> "Instagram"
            "com.android.chrome" -> "Instagram"
            "com.instagram.android" -> "Instagram"
            "com.zhiliaoapp.musically" -> "TikTok"
            "com.snapchat.android" -> "Snapchat"
            else -> "Unknown App"
        }

        if (userId != null) {
            val firebaseRef1 = FirebaseDatabase.getInstance().reference.child("responses").child(userId).child("app_name")
            firebaseRef1.setValue(appName)
            val firebaseRef2 = FirebaseDatabase.getInstance().reference.child("responses").child(userId).child("package_name")
            firebaseRef2.setValue(packageName)
        }
    }

    // Handle back button press when the drawer is open
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(emotionReceiver)
    }
}
