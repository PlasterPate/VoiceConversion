import numpy as np
from scipy.io import wavfile as wav
#import soundfile as sf


def speedx(sound_array, factor):
    """ Multiplies the sound's speed by some `factor` """
    indices = np.round( np.arange(0, len(sound_array), factor) )
    indices = indices[indices < len(sound_array)].astype(int)
    return sound_array[ indices.astype(int) ]


def stretch(sound_array, f, window_size, h):
    """ Stretches the sound by a factor `f` """

    phase = np.zeros(window_size)
    hanning_window = np.hanning(window_size)
    result = np.zeros(int(len(sound_array) /f) + window_size)

    for i in np.arange(0, len(sound_array)-(window_size+h), h*f):

        # print(int(i))
        # print(window_size)

        # two potentially overlapping subarrays
        a1 = [sound_array[j] for j in range(int(i), int(i) + window_size)]
        a2 = [sound_array[j] for j in range(int(i) + h, int(i) + window_size + h)]

        # resynchronize the second array on the first
        s1 =  np.fft.fft(hanning_window * a1)
        s2 =  np.fft.fft(hanning_window * a2)
        phase = (phase + np.angle(s2/s1)) % 2*np.pi
        a2_rephased = np.fft.ifft(np.abs(s2)*np.exp(1j*phase))

        # add to result
        i2 = int(i/f)
        result[i2: i2 + window_size] = [result[j] for j in range(int(i2), int(i2) + window_size)] + hanning_window*a2_rephased

    result = ((2**(16-4)) * result/result.max()) # normalize (16bit)

    return result.astype('int16')

def pitchshift(snd_array, n, window_size=2**13, h=2**11):
    """ Changes the pitch of a sound by ``n`` semitones. """
    #snd_array = snd_array[:, 0]
    factor = 2**(1.0 * n / 12.0)
    stretched = stretch(snd_array, 1.0/factor, window_size, h)
    return speedx(stretched[window_size:], factor)


# sr, sound = wav.read("female_scale.wav")
# sound = sound[:, 0]
# shifted_voice = pitchshift(sound, -6)
# wav.write("out.wav", sr, shifted_voice)

# p = pyaudio.PyAudio()
#
# CHUNK = 30000
# RATE = 44100
#
# stream = p.open(format=pyaudio.paInt16, channels=1, rate=RATE, input=True, frames_per_buffer=CHUNK)
# player = p.open(format=pyaudio.paInt16, channels=1, rate=RATE, output=True, frames_per_buffer=CHUNK)
#
# while True:
#     data = np.fromstring(stream.read(CHUNK), dtype=np.int16)
#     print(data)
#     new_res = pitchshift(data, 24)
#
#     player.write(new_res, CHUNK)

