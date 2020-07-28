package com.example.voiceconversion.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.voiceconversion.AudioRecorder
import com.example.voiceconversion.R
import com.xw.repo.BubbleSeekBar
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.main_fragment.*


class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private val LOG_TAG = "MainFragment"
    private lateinit var recordedSignal : ShortArray

    private var sampleRate = 44100

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(this).get(MainViewModel::class.java)
    }

    private val recorder: AudioRecorder by lazy {
        AudioRecorder()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestPermissions()

        record_btn.setOnClickListener {
            mainViewModel.switchRecordState()
        }


        pitch_seek_bar.onProgressChangedListener = object : BubbleSeekBar.OnProgressChangedListener{
            override fun onProgressChanged(
                bubbleSeekBar: BubbleSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {
                // Do nothing
            }

            override fun getProgressOnActionUp(
                bubbleSeekBar: BubbleSeekBar?, progress: Int, progressFloat: Float) {

                // Update Pitch
                mainViewModel.setPitchShift(progress)
                println(progress)
            }

            override fun getProgressOnFinally(
                bubbleSeekBar: BubbleSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {
                // Do nothing
            }

        }

        mainViewModel.isRecording.observe(requireActivity(), Observer { isRecording ->
            // If user wants to stop recording
            if (!isRecording){
                changeRecordButtonColor(R.drawable.record_button_stopped)
                // Stop pulse animation
                pulse_view.stop()
                // Stop recording
                recorder.stopRecording()
            }
            // If user wants to start recording
            else{
                changeRecordButtonColor(R.drawable.record_button_recording)
                // Start pulse animation
                pulse_view.start()
                Toast.makeText(requireContext(),"Recording Started!", Toast.LENGTH_SHORT).show()
                // Start recording
                recorder.startRecording()
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        // store recorded voice and its sample rate
                        recordedSignal = it.first
                        sampleRate = it.second
                        // Trigger pitchShift LiveData
                        mainViewModel.setPitchShift(mainViewModel.pitchShift.value!!)
                        Toast.makeText(requireContext(), "Recording Finished!", Toast.LENGTH_SHORT).show()
                    },{
                        println(it)
                    })
            }
        })

        mainViewModel.pitchShift.observe(requireActivity(), Observer {
            Log.v(LOG_TAG, "Pitch is: $it")
            // Check if recordedSignal has been initialized
            if (this::recordedSignal.isInitialized) {
                if (!Python.isStarted()) {
                    Python.start(AndroidPlatform(context))
                }
                val py = Python.getInstance()
                startPlaybackLoading()
                mainViewModel.changeVoice(recordedSignal, sampleRate, py)
            }
        })

        mainViewModel.shiftedSignal.observe(requireActivity(), Observer { shiftedSignal ->
            stopPlaybackLoading()

            playback_btn.setOnClickListener {
                if (shiftedSignal!!.isNotEmpty()) {
                    Toast.makeText(requireContext(), "Playback Started!", Toast.LENGTH_SHORT).show()
                    // Disable playback button
                    playback_btn.isEnabled = false

                    // Start playback
                    recorder.startPlayback(shiftedSignal)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            // Enable playback button
                            playback_btn.isEnabled = true
                            Toast.makeText(requireContext(), "Playback Finished!", Toast.LENGTH_SHORT).show()
                        },{})
                }
            }
        })

    }

    private fun changeRecordButtonColor(id: Int){
        record_btn.background = resources.getDrawable(id, requireContext().theme)
    }

    private fun startPlaybackLoading() {
        playback_btn.visibility = View.INVISIBLE
        playback_loading_progress_bar.visibility = View.VISIBLE

    }

    private fun stopPlaybackLoading() {
        playback_btn.visibility = View.VISIBLE
        playback_loading_progress_bar.visibility = View.GONE
    }

    private fun requestPermissions(){
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(requireActivity(), permissions,0)
        }
    }
}
