import wave, struct, math, random
from scipy import signal


def clip16(x):
    # Clipping for 16 bits

    for i in range(0, len(x)):
        if x[i] > 32767:
            x[i] = 32767
        elif x[i] < -32768:
            x[i] = -32768
        else:
            x[i] = x[i]

    return x


wavfile = 'ahpuh.wav'

print("Play the wave file %s." % wavfile)
# Open wave file (should be mono channel)
wf = wave.open(wavfile, 'rb')
# Read the wave file properties
num_channels = wf.getnchannels()  # Number of channels
RATE = wf.getframerate()  # Sampling rate (frames/second)
signal_length = wf.getnframes()  # Signal length
width = wf.getsampwidth()  # Number of bytes per sample
print("The file has %d channel(s)." % num_channels)
print("The frame rate is %d frames/second." % RATE)
print("The file has %d frames." % signal_length)
print("There are %d bytes per sample." % width)

sampleRate = 44100.0  # hertz
duration = 1.0  # seconds
frequency = 440.0  # hertz
obj = wave.open('ahpuh_EQ.wav', 'w')
obj.setnchannels(num_channels)  # mono
obj.setsampwidth(width)
obj.setframerate(RATE)

fc = 500
V = 10  # bass 변경값

K = math.tan(math.pi * fc / RATE)
G = 10 ** (V / 20)
b0 = (1 + math.sqrt(2 * G) * K + G * (K ** 2)) / (1 + math.sqrt(2) * K + K ** 2)
b1 = (2 * (G * (K ** 2) - 1)) / (1 + math.sqrt(2) * K + K ** 2)
b2 = (1 - math.sqrt(2 * G) * K + G * (K ** 2)) / (1 + math.sqrt(2) * K + K ** 2)
# a0 =  1.000000000000000
a1 = (2 * ((K ** 2) - 1)) / (1 + math.sqrt(2) * K + K ** 2)
a2 = (1 - math.sqrt(2) * K + (K ** 2)) / (1 + math.sqrt(2) * K + K ** 2)

########  Difference equation coefficients for high shelving filter   ######
fc_h = 10000  # cut odd frequency for high shelving filter
V_h = 9  # mid 변경값

K_h = math.tan(math.pi * fc_h / RATE)
G_h = 10 ** (V_h / 20)
b3 = (G_h + math.sqrt(2 * G_h) * K_h + (K_h ** 2)) / (1 + math.sqrt(2) * K_h + K_h ** 2)
b4 = (2 * ((K_h ** 2) - G_h)) / (1 + math.sqrt(2) * K_h + K_h ** 2)
b5 = (G_h - math.sqrt(2 * G_h) * K_h + (K_h ** 2)) / (1 + math.sqrt(2) * K_h + K_h ** 2)
a3 = (2 * ((K_h ** 2) - 1)) / (1 + math.sqrt(2) * K_h + K_h ** 2)
a4 = (1 - math.sqrt(2) * K_h + (K_h ** 2)) / (1 + math.sqrt(2) * K_h + K_h ** 2)

################ Difference equation for Peak filter ################
fc_p = 20000
V_p = 30  # treble 변경값
Q = 10

K_p = math.tan(math.pi * fc_p / RATE)
G_p = 10 ** (V_p / 20)

b6 = (1 + (G_p / Q) * K_p + K_p ** 2) / (1 + (1 / Q) * K_p + K_p ** 2)
b7 = (2 * ((K_p ** 2) - 1)) / (1 + (1 / Q) * K_p + K_p ** 2)
b8 = (1 - (G_p / Q) * K_p + K_p ** 2) / (1 + (1 / Q) * K_p + K_p ** 2)
a5 = (2 * ((K_p ** 2) - 1)) / (1 + (1 / Q) * K_p + K_p ** 2)
a6 = (1 - (1 / Q) * K_p + K_p ** 2) / (1 + (1 / Q) * K_p + K_p ** 2)

a0 = 1

b_l = [b0, b1, b2]
a_l = [a0, a1, a2]

b_h = [b3, b4, b5]
a_h = [a0, a3, a4]

b_p = [b6, b7, b8]
a_p = [a0, a5, a6]

BLOCKSIZE = 512

num_blocks = int(math.floor(signal_length / BLOCKSIZE))

for i in range(0,num_blocks):
    input_string = wf.readframes(BLOCKSIZE)
    input_tuple = struct.unpack('h' * BLOCKSIZE, input_string)  # One-element tuple
    x = input_tuple

    b_l = [b0, b1, b2]
    a_l = [a0, a1, a2]

    b_h = [b3, b4, b5]
    a_h = [a0, a3, a4]

    b_p = [b6, b7, b8]
    a_p = [a0, a5, a6]

    y1 = signal.lfilter(b_l, a_l, x)  # LP
    y2 = signal.lfilter(b_h, a_h, y1)  # HP
    y3 = signal.lfilter(b_p, a_p, y2)  # PF

    output_value = y3
    output_value = clip16(output_value)
    output_value = map(int, output_value)
    # value = random.randint(-32767, 32767)
    data = struct.pack('h' * BLOCKSIZE, *output_value)
    obj.writeframesraw(data)
obj.close()