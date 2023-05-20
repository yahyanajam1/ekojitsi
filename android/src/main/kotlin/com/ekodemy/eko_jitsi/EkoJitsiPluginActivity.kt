package com.ekodemy.eko_jitsi

import android.app.KeyguardManager
import android.content.*
import android.content.BroadcastReceiver
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import com.ekodemy.eko_jitsi.EkoJitsiPlugin.Companion.EKO_JITSI_CLOSE
import com.ekodemy.eko_jitsi.EkoJitsiPlugin.Companion.EKO_JITSI_TAG
import com.facebook.react.ReactRootView
import com.facebook.react.views.text.ReactTextView
import com.facebook.react.views.view.ReactViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.jitsi.meet.sdk.*
import java.util.*
import com.google.android.material.bottomsheet.BottomSheetDialog


/**
 * Activity extending JitsiMeetActivity in order to override the conference events
 */
class EkoJitsiPluginActivity : JitsiMeetActivity() {
    companion object {

        var classroomLogo: String? = null;
        var whiteboardUrl: String? = null;
        var classroomLogoId: Int? = null;
        var context: Context? = null;


        @JvmStatic
        fun launchActivity(
            context: Context?,
            options: JitsiMeetConferenceOptions
        ) {
            var intent = Intent(context, EkoJitsiPluginActivity::class.java).apply {
                action = "org.jitsi.meet.CONFERENCE"
                putExtra("JitsiMeetConferenceOptions", options)
            }
            context?.startActivity(intent)
            this.context = context;
        }

        @JvmStatic
        fun setData(classroomLogo: String?, whiteboardUrl: String?): Unit {
            this.classroomLogo = classroomLogo;
            this.whiteboardUrl = whiteboardUrl;
            if (this.classroomLogo != null) {
                this.classroomLogoId = this.context!!.resources.getIdentifier(
                    this.classroomLogo,
                    "drawable",
                    context!!.packageName
                );
            }
            Log.i(
                EKO_JITSI_TAG,
                "classroomLogo [" + classroomLogo + "] whiteboardUrl [" + whiteboardUrl + "]"
            );
        }
    }

    var onStopCalled: Boolean = false;
    var ekoLayout: LinearLayout? = null;
    //var jitsiView: JitsiMeetView ? = null

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            EkoJitsiEventStreamHandler.instance.onPictureInPictureWillEnter()
            this.ekoLayout!!.setVisibility(LinearLayout.GONE);
        } else {
            EkoJitsiEventStreamHandler.instance.onPictureInPictureTerminated()
            this.ekoLayout!!.setVisibility(LinearLayout.VISIBLE);
        }
        if (isInPictureInPictureMode == false && onStopCalled) {
            // Picture-in-Picture mode has been closed, we can (should !) end the call
                //getJitsiView().leave()
        }
    }

    private val myReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                EKO_JITSI_CLOSE -> finish()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        onStopCalled = true;
        unregisterReceiver(myReceiver)
    }

    override fun onResume() {
        super.onResume()
        onStopCalled = false
        registerReceiver(myReceiver, IntentFilter(EKO_JITSI_CLOSE))
    }

    override fun onConferenceWillJoin(data: HashMap<String, Any>?) {
        Log.d(EKO_JITSI_TAG, String.format("EkoJitsiPluginActivity.onConferenceWillJoin: %s", data))
        EkoJitsiEventStreamHandler.instance.onConferenceWillJoin(data)
        super.onConferenceWillJoin(data)
    }

    override fun onConferenceJoined(data: HashMap<String, Any>?) {
        Log.d(EKO_JITSI_TAG, String.format("EkoJitsiPluginActivity.onConferenceJoined: %s", data))
        EkoJitsiEventStreamHandler.instance.onConferenceJoined(data)
        super.onConferenceJoined(data)
        this.test();
    }

    override fun onConferenceTerminated(data: HashMap<String, Any>?) {

        Log.d(
            EKO_JITSI_TAG,
            String.format("EkoJitsiPluginActivity.onConferenceTerminated: %s", data)
        )
        EkoJitsiEventStreamHandler.instance.onConferenceTerminated(data)
        super.onConferenceTerminated(data)
    }

    override fun onParticipantLeft(data: HashMap<String, Any>?) {
        Log.d(EKO_JITSI_TAG, String.format("EkoJitsiPluginActivity.onParticipantLeft: %s", data))
        EkoJitsiEventStreamHandler.instance.onParticipantLeft(data)
        super.onConferenceTerminated(data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndKeyguardOff();
    }

    //START
    override fun onPostCreate(savedInstanceState: Bundle?) {
        Log.i(EKO_JITSI_TAG, "ABC Post Create");
        super.onPostCreate(savedInstanceState);
        logContentView(getWindow().getDecorView(), "");

        val view = window.decorView as ViewGroup;

        //val view = window.decorView.findViewById<ViewGroup>(android.R.id.content)

        Log.d(EKO_JITSI_TAG, "ABC " + view.javaClass.canonicalName);


        //for (i in 0 until view.childCount) {}

        val layout: LinearLayout = view.getChildAt(0) as LinearLayout;
        prepareWhiteboardLayout(layout);

        /*

        val jitsiMeetView = JitsiMeetView(this)

        val button = Button(this)
        val gradientDrawable = GradientDrawable()
        gradientDrawable.shape = GradientDrawable.RECTANGLE
        gradientDrawable.cornerRadius = 12f

        gradientDrawable.setColor(Color.WHITE)

        button.text = " ☰ "
        button.setBackgroundColor(Color.WHITE);
        // Set the button's position and size
        button.setTextColor(Color.BLACK);

        button.background = gradientDrawable

        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.BOTTOM or Gravity.START
        layoutParams.setMargins(106, 0, 0, 24)

        button.layoutParams = layoutParams
        button.layoutParams.width = 60
        button.layoutParams.height = 72

// Add the button to the Jitsi view
        (jitsiView as FrameLayout).addView(button)
        */

    }
    //END

    fun test() {
        if(true){
            return;
        }
        try {
            val jitsiView: JitsiMeetView = jitsiView;
            Log.d(EKO_JITSI_TAG, "ABC " + jitsiView.javaClass.canonicalName);
            val ab = getRootReactView(jitsiView);
            if (ab != null) {
                Log.d(EKO_JITSI_TAG, "ABC " + ab.javaClass.canonicalName)
            };
            val rootReactView: ReactRootView = ab as ReactRootView;
            Log.d(EKO_JITSI_TAG, "ABC " + rootReactView.javaClass.canonicalName);
            logContentView(rootReactView.rootViewGroup, "");
        } catch (ex: Exception) {
            Log.e(EKO_JITSI_TAG, "ABC Error", ex);
        }
//        var jitsiFragment: Fragment? = getSupportFragmentManager().findFragmentById(R.id.jitsiFragment);
    }

    //START
    fun prepareWhiteboardLayout(layout: LinearLayout) {
        this.ekoLayout = LinearLayout(this);
        this.ekoLayout!!.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        this.ekoLayout!!.setPadding(20, 20, 20, 20)

        this.ekoLayout!!.gravity = Gravity.LEFT;
        var logoParentlayout: LinearLayout = LinearLayout(this);
        logoParentlayout.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        logoParentlayout.gravity = Gravity.LEFT;
        val logoImage = ImageView(this);
        //logoImage.setImageURI(Uri.parse("https://www.ekodemy.in/wp-content/uploads/2021/02/vidyartham@2x_1.png"));
        if (EkoJitsiPluginActivity.classroomLogoId != null) {
            logoImage.setImageResource(EkoJitsiPluginActivity.classroomLogoId!!);
        }
        logoImage.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            100
        );

        logoImage.id = View.generateViewId();
        logoImage.scaleType = ImageView.ScaleType.FIT_START
        logoImage.adjustViewBounds = true;

        var btnParentlayout: LinearLayout = LinearLayout(this);
        btnParentlayout.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );

        btnParentlayout.gravity = Gravity.RIGHT;

        val btnTag = Button(this)
        btnTag.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            100
        );
        btnTag.text = "Sidebar Menu";
        btnTag.id = View.generateViewId();
        btnTag.setBackgroundColor(Color.DKGRAY);

        if (EkoJitsiPluginActivity.whiteboardUrl != null) {
            btnTag.setTextColor(Color.WHITE);
            btnTag.setOnClickListener {
                EkoJitsiEventStreamHandler.instance.onWhiteboardClicked();
                  //Toast.makeText(this, "Whiteboard", Toast.LENGTH_SHORT).show()

                //val alert: AlertDialog.Builder = AlertDialog.Builder(this)
                //alert.setTitle("Sidebar Menu")

                val wv = WebView(this)

                val progressBar = ProgressBar(this)

                val bottomSheetDialog = BottomSheetDialog(this)

                //val linearLayout = RelativeLayout(this)

                progressBar.indeterminateTintList = ColorStateList.valueOf(Color.WHITE)
                wv.webViewClient = object : WebViewClient() {

                    @Deprecated("Deprecated in Java")
                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                        view.loadUrl(url)
                        return true
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        // Show the progress bar when the page starts loading
                        progressBar.visibility = View.VISIBLE
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        // Hide the progress bar when the page finishes loading
                        progressBar.visibility = View.GONE
                    }

                }
                wv.loadUrl(whiteboardUrl!!)
                wv.settings.javaScriptEnabled = true;
                wv.settings.javaScriptCanOpenWindowsAutomatically = true;
                wv.settings.domStorageEnabled = true;

                /*
                alert.setNegativeButton("Close",
                    DialogInterface.OnClickListener { dialog, id -> dialog.dismiss() });

                alert.setView(wv)
                alert.show()
                */

                /*
                val d: Dialog = alert.setView(View(this)).create()
                val lp: WindowManager.LayoutParams = WindowManager.LayoutParams()
                lp.copyFrom(d.window!!.attributes)
                lp.width = WindowManager.LayoutParams.MATCH_PARENT
                lp.height = WindowManager.LayoutParams.MATCH_PARENT

                val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
                val height = (resources.displayMetrics.heightPixels * 0.90).toInt()

                d.window!!.setLayout(width, height);
                d.window!!.attributes = lp

                d.show()
                */

                /*
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(whiteboardUrl!!))
                ContextCompat.startActivity(context!!, browserIntent, null)
                */

                val layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )

                layoutParams.gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
                progressBar.layoutParams = layoutParams
                (jitsiView as FrameLayout).addView(progressBar)

                /*
                linearLayout.layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
                )
                // Set the background color
                linearLayout.setBackgroundColor(Color.RED) // Replace with your desired color

                // Add other views to the LinearLayout
                linearLayout.addView(wv)
                */
                bottomSheetDialog.setContentView(wv)

                // Inflate the layout for the bottom sheet
               // val sheetView = layoutInflater.inflate(linearLayout, null)

                // Customize the bottom sheet view as needed
                // For example, you can find and configure views within the sheetView

                // Set the view for the bottom sheet dialog
                //bottomSheetDialog.setContentView(wv as View)

                val behavior = BottomSheetBehavior.from(wv.parent as View)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED

                // Show the bottom sheet dialog
                bottomSheetDialog.show()

            }

        } else {
            btnTag.setTextColor(Color.BLACK);
        }

        layout.setBackgroundColor(Color.BLACK);
        logoParentlayout.addView(logoImage);
        btnParentlayout.addView(btnTag);
        this.ekoLayout!!.addView(logoParentlayout);
        this.ekoLayout!!.addView(btnParentlayout);
        layout.addView(ekoLayout, 0);
    }
    //END

    fun logContentView(parent: View, indent: String) {
        if (parent is ReactViewGroup) {
            var abc = parent as ReactViewGroup;
            Log.i("ABC test", indent + parent.javaClass.name + " - Tag "+ abc.tag)
        } else if (parent is ReactTextView) {
            var abc = parent as ReactTextView;
            Log.i("ABC test", indent + parent.javaClass.name + " - Text " + abc.text)
        } else {
            Log.i("ABC test", indent + parent.javaClass.name)
        }
        if (parent is ViewGroup) {
            val group = parent
            for (i in 0 until group.childCount) logContentView(group.getChildAt(i), "$indent ")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        turnScreenOffAndKeyguardOn()
    }

    private fun turnScreenOnAndKeyguardOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // For newer than Android Oreo: call setShowWhenLocked, setTurnScreenOn
            setShowWhenLocked(true)
            setTurnScreenOn(true)

            // If you want to display the keyguard to prompt the user to unlock the phone:
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            // For older versions, do it as you did before.
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_FULLSCREEN
                        or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
        }
    }

    private fun turnScreenOffAndKeyguardOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false)
            setTurnScreenOn(false)
        } else {
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_FULLSCREEN
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
        }
    }
}

fun getRootReactView(view: JitsiMeetView): Any? {

    return ReactRootView::class.java.getDeclaredField("reactRootView").let {
        it.isAccessible = true;
        val value = it.get(view)
        //todo
        return@let value;
    }


//return this.reactRootView;


}
