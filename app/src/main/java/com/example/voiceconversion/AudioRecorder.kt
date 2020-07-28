package com.example.voiceconversion

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.Single
import java.io.IOException
import java.nio.ShortBuffer


class AudioRecorder {

    private val SAMPLE_RATE = 44100
    private val LOG_TAG = "AudioRecorder"

    private var inputSignal = shortArrayOf()
    private var isRecording = false
    private var isPlaying = false


    private val _audioBuffer = MutableLiveData<ShortArray>()
    val audioBuffer: LiveData<ShortArray>
    get() = _audioBuffer

    fun startRecording(): Single<Pair<ShortArray, Int>>{
        isRecording = true
        return Completable.fromAction{record()}.toSingle{
            Pair(inputSignal, SAMPLE_RATE)
        }
    }

    fun stopRecording(): Boolean{
        return if (isRecording){
            isRecording = false
            true
        }else{
            false
        }
    }

    fun startPlayback(samplesArray: ShortArray): Completable{
        val samplesCount = samplesArray.size
        val samplesBuffer = ShortBuffer.wrap(samplesArray)
        isPlaying = true
        return Completable.fromAction{
            play(samplesBuffer, samplesCount)
        }
    }

    fun stopPlayback(){
        isPlaying = false
    }

    private fun record(){
        try {
            inputSignal = shortArrayOf()
            var bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                bufferSize = SAMPLE_RATE * 2
            }

            _audioBuffer.postValue(ShortArray(bufferSize / 2))
            val buffer = ShortArray(bufferSize / 2)

            val recorder = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(LOG_TAG, "Audio Record can't initialize!")
            }
            recorder.startRecording()
            Log.v(LOG_TAG, "Start recording on ${Thread.currentThread()}")

            var shortsRead: Long = 0
            while (isRecording) {
                val numberOfShort = recorder.read(buffer, 0, buffer.size)
                shortsRead += numberOfShort.toLong()
                inputSignal = inputSignal.plus(buffer)
                _audioBuffer.postValue(buffer)
            }
            recorder.stop()
            recorder.release()
            Log.v(LOG_TAG, String.format("Recording stopped. Samples read: %d", shortsRead))
        } catch (e: IOException) {
            e.printStackTrace()
            Log.v(LOG_TAG, "Recording throw exception $e")
        }
    }

    private fun play(samples: ShortBuffer, samplesCount: Int){
        var bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2
        }
        val player = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        player.play()
        Log.v(LOG_TAG, "Audio streaming started")


        val buffer = ShortArray(bufferSize)
        samples.rewind()
        var totalWritten = 0
        while (samples.position() < samplesCount && isPlaying) {
            val numSamplesLeft: Int = samplesCount - samples.position()
            var samplesToWrite: Int
            if (numSamplesLeft >= buffer.size) {
                samples.get(buffer)
                samplesToWrite = buffer.size
            } else {
                for (i in numSamplesLeft until buffer.size) {
                    buffer[i] = 0
                }
                samples.get(buffer, 0, numSamplesLeft)
                samplesToWrite = numSamplesLeft
            }
            totalWritten += samplesToWrite
            player.write(buffer, 0, samplesToWrite)
        }
        if (!isPlaying) {
            player.release()
        }
        Log.v(
            LOG_TAG,
            "Audio streaming finished. Samples written: $totalWritten"
        )
    }
}