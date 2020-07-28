package com.example.voiceconversion.ui.main

import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.chaquo.python.Python
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class MainViewModel : ViewModel() {

    private val LOG_TAG = "MainViewModel"

    private val _shiftedSignal = MutableLiveData<ShortArray?>()
    val shiftedSignal: LiveData<ShortArray?>
        get() = _shiftedSignal

    private val _pitchShift = MutableLiveData(0)
    val pitchShift: LiveData<Int>
        get() = _pitchShift

    private val _isRecording = MutableLiveData(false)
    val isRecording: LiveData<Boolean>
        get() = _isRecording

    fun changeVoice(signal: ShortArray, sampleRate: Int, py: Python){
        val pyf = py.getModule("VoiceConversion")
        val obj = Single.fromCallable{pyf.callAttr("pitch_shifting", signal, _pitchShift.value)}
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({result ->
                Log.v(LOG_TAG, "Successfully shifted pitch")
                _shiftedSignal.value = result.toJava(ShortArray::class.java)
                println(_shiftedSignal.value)
            }, {
                Log.v(LOG_TAG, it.localizedMessage)
                it.printStackTrace()
            })
    }

    fun switchRecordState(){
        _isRecording.value = _isRecording.value?.not()
    }

    fun setPitchShift(semitone: Int){
        _pitchShift.value = semitone
    }
}
