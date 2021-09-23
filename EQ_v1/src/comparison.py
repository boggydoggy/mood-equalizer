import IPython.display as ipd
import librosa

#변경 전.
y , sr = librosa.load('/Users/tomato/Downloads/Data/genres_original/reggae/reggae.00003.wav')
ipd.Audio(y, rate=sr)


#변경 후.
y , sr = librosa.load('new_reggae00003.wav')
ipd.Audio(y, rate=sr)