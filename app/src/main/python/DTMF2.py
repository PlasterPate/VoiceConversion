import numpy as np
from itertools import groupby
import DTMF1


def DTMF(signal, rate):

    if len(signal) < 10:
        return ""

    # Length of the signal in seconds
    length = len(signal) / rate
    # print("\nFile is " + str(length) + " seconds long")

    # print(length)
    chunk_count = int(length / 0.2) + 1

    signal.reshape((signal.shape[0], 1))

    # Divide the signal into smaller pieces
    chunks = np.array_split(signal, chunk_count)
    # print("chunk len:   " + str(len(chunks)))

    res = []
    # Process every chunks to detect key beeps
    for chunk in chunks:
        res.append(DTMF1.DTMF(chunk, rate))

    # Group similar values which are useless
    grouped_res = [x[0] for x in groupby(res)]

    # Concatenate values together
    result = ''.join(grouped_res)

    return result
