package com.subratsss.agorasdkvoicecall

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.subratsss.agorasdkvoicecall.databinding.ActivityMainBinding
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.audio.IAudioSpectrumObserver


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Fill the App ID of your project generated on Agora Console.
    private val appId = "bdbd981e811747f8869d362bef6b4294"

    private var channelName = "AgoraVoice"

    // Fill the temp token generated on Agora Console.
    private val token =
        "007eJxTYJD/ajml5rO255OjLs8q5y1sDdv5o+WspeUtN9P7KhU7WTQVGJJSklIsLQxTLQwNzU3M0ywszCxTjM2MklLTzJJMjCxNco82pDQEMjJcfL6YgREKQXwuBsf0/KLEsPzM5FQGBgCBLSND"

    // An integer that identifies the local user.
    var uid = 0

    // Track the status of your connection
    private var isJoined = false
    private var isMuted = false

    // Agora engine instance
    private var agoraEngine: RtcEngine? = null

    private val REQUESTED_PERMISSION = arrayOf<String>(Manifest.permission.RECORD_AUDIO)


    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                setUpVoiceSDKEngine()
            } else {
                showMessage("Permission required for voice call")
            }

        }

    private fun checkSelfPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                REQUESTED_PERMISSION[0]
            ) == PackageManager.PERMISSION_GRANTED -> {

                setUpVoiceSDKEngine()
            }

            shouldShowRequestPermissionRationale(REQUESTED_PERMISSION[0]) -> {
                showMessage("Permission required for voice call")
            }

            else -> {
                requestPermissionLauncher.launch(REQUESTED_PERMISSION[0])
            }
        }
    }

    fun showMessage(message: String?) {
        runOnUiThread {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setUpVoiceSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            agoraEngine?.enableAudioVolumeIndication(500, 3, true)
        } catch (e: Exception) {
            showMessage(e.toString())
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkSelfPermission()

        binding.tvChannelName.text = "Channel Name: $channelName"

    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()

        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }

    fun joinChannel() {
        val options = ChannelMediaOptions()
        options.autoSubscribeAudio = true
        // Set both clients as the BROADCASTER.
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        // Set the channel profile as BROADCASTING.
        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING

        // Join the channel with a temp token.
        // You need to specify the user ID yourself, and ensure that it is unique in the channel.
        agoraEngine?.joinChannel(token, channelName, uid, options)

    }

    fun onEndCallClicked(view: View) {
        finish()
    }


    fun joinLeaveChannel(view: View) {
        if (isJoined) {
            agoraEngine?.leaveChannel()
            binding.joinLeaveButton.text = getString(R.string.join)
            binding.muteButton.visibility = View.GONE
            binding.endButton.visibility = View.GONE

        } else {
            joinChannel()

            binding.joinLeaveButton.text = getString(R.string.leave)
            binding.muteButton.visibility = View.VISIBLE
            binding.endButton.visibility = View.VISIBLE


        }
    }

    fun onAudioMuteClicked(view: View) {
        isMuted = !isMuted
        //stops/resumes sending the local audio stream
        agoraEngine?.muteLocalAudioStream(isMuted)
        if (isMuted) {
            binding.muteButton.setSelected(true)
            binding.muteButton.setColorFilter(
                resources.getColor(R.color.colorPrimary),
                PorterDuff.Mode.MULTIPLY
            )

        } else {

            binding.muteButton.setSelected(false)
            binding.muteButton.clearColorFilter()

        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote user joining the channel.

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                binding.infoText.text = "Remote user joined: $uid"
                binding.muteButton.visibility = View.VISIBLE
                binding.endButton.visibility = View.VISIBLE
            }
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            // Successfully joined a channel
            Log.v("Check ", "current uid " + uid)
            this@MainActivity.uid = uid
            isJoined = true
            showMessage("Joined Channel $channel")
            runOnUiThread { binding.infoText.text = getString(R.string.waiting_instruction) }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            // Listen for remote users leaving the channel
            showMessage("Remote user offline $uid $reason")
            runOnUiThread {
                binding.muteButton.visibility = View.GONE
                binding.endButton.visibility = View.GONE
            }

            if (isJoined) {
                runOnUiThread { binding.infoText.text = getString(R.string.waiting_instruction) }
            }
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            // Listen for the local user leaving the channel
            runOnUiThread {
                binding.infoText.text = getString(R.string.join_instruction)
                binding.muteButton.visibility = View.GONE
                binding.endButton.visibility = View.GONE
            }
            isJoined = false
        }

        override fun onAudioVolumeIndication(
            speakers: Array<out AudioVolumeInfo>?,
            totalVolume: Int
        ) {
            super.onAudioVolumeIndication(speakers, totalVolume)
            for (speaker in speakers!!) {

                if (speaker.volume > 0 && speaker.uid == 0 || speaker.uid == uid) {

                    Log.v("Check", "U Speaking")
                } else {
                    Log.v("Check", "you are Not Speaking")
                }
            }


        }


    }
}