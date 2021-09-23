import joblib
import librosa
import pandas as pd

def extraction(filename):
    test_y, test_sr = librosa.load(filename+'.wav')
    ##여기에 불러올 음악 넣어야함.

    data = {}

    data['filename'] = filename+'.wav'
    ##파일명을 자동으로 만들어주는 것도 생각해야함.
    data['length'] = len(test_y)

    chromagram = librosa.feature.chroma_stft(test_y, sr=test_sr, hop_length=512)
    data['chroma_stft_mean'] = chromagram.mean()
    data['chroma_stft_var'] = chromagram.var()

    rms = librosa.feature.rms(y=test_y)
    data['rms_mean'] = rms.mean()
    data['rms_var'] = rms.var()

    spectral_centroids = librosa.feature.spectral_centroid(test_y, sr=test_sr)
    data['spectral_centroid_mean'] = spectral_centroids.mean()
    data['spectral_centroid_var'] = spectral_centroids.var()

    spectral_bandwidth = librosa.feature.spectral_bandwidth(y=test_y, sr=test_sr)
    data['spectral_bandwidth_mean'] = spectral_bandwidth.mean()
    data['spectral_bandwidth_var'] = spectral_bandwidth.var()

    spectral_rolloff = librosa.feature.spectral_rolloff(test_y, sr=test_sr)
    data['rolloff_mean'] = spectral_rolloff.mean()
    data['rolloff_var'] = spectral_rolloff.var()

    zero_crossings = librosa.zero_crossings(test_y, pad=False)
    data['zero_crossing_rate_mean'] = zero_crossings.mean()
    data['zero_crossing_rate_var'] = zero_crossings.var()

    y_harm, y_perc = librosa.effects.hpss(test_y)
    data['harmony_mean'] = y_harm.mean()
    data['harmony_var'] = y_harm.var()
    data['perceptr_mean'] = y_perc.mean()
    data['perceptr_var'] = y_perc.var()

    tempo, _ = librosa.beat.beat_track(test_y, sr=test_sr)
    data['tempo'] = tempo

    mfccs = librosa.feature.mfcc(test_y, sr=test_sr)
    for i in range(0, len(mfccs)):
        a = 'mfcc'
        a = a + str(i + 1)
        keys = {a + '_mean': mfccs[i].mean(), a + '_var': mfccs[i].var()}
        data.update(keys)

    data['label'] = 'none'
    print(data)

    return data

def predict(data):
    test_data = pd.DataFrame([data])
    test_data_X = test_data.drop(columns=['filename', 'length', 'label'])
    test_data_y = test_data['label']  # 장르명

    # load saved model
    xgb = joblib.load("model.json")

    pred = xgb.predict(test_data_X)
    print(pred)
    return pred

# def main():
#     data = extraction()
#     pred = predict(data)
#
#     return pred
