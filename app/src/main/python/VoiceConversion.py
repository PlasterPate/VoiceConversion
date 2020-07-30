import numpy as np
from numpy.fft import fft, ifft


def time_stretching(input_signal, factor, window_size, T):
    phase = np.zeros(window_size)
    hanning_window = np.hanning(window_size)
    result = np.zeros(int(len(input_signal) / factor) + window_size)

    indexes = np.arange(0, len(input_signal) - (window_size + T), T * factor)
    for i in indexes:
        first_sub = [input_signal[j] for j in range(int(i), int(i) + window_size)]
        second_sub = [input_signal[j] for j in range(int(i) + T, int(i) + T + window_size)]

        first_freqs = fft(hanning_window * first_sub)
        second_freqs = fft(hanning_window * second_sub)

        new_phase = (phase + np.angle(np.divide(second_freqs, first_freqs))) % 2 * np.pi
        second_sub_rephased = ifft(np.abs(second_freqs) * np.exp(1j * new_phase))

        k = int(i / factor)

        result[k: k + window_size] = [result[j] for j in range(int(k), int(k) + window_size)] + second_sub_rephased

    result = (pow(2, 12) * result / np.max(result))

    return result.astype('int16')


def resample(input_signal, factor):
    indexes = np.arange(0, len(input_signal), factor).astype(int)
    if indexes[-1] == len(input_signal):
        np.delete(indexes, len(indexes - 1))

    return input_signal[indexes]


def pitch_shifting(input_signal, n, window_size=pow(2, 13), T=pow(2, 11)):
    factor = pow(2, n / 12)

    stretched_signal = time_stretching(input_signal, 1.0 / factor, window_size, T)

    output_signal = resample(stretched_signal[window_size:], factor)

    return output_signal
