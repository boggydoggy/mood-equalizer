import pandas as pd
import sklearn as sklearn
from xgboost import XGBClassifier
from sklearn.metrics import accuracy_score
from sklearn.model_selection import train_test_split
import joblib

def learning():
    df = pd.read_csv('/Users/tomato/Downloads/Data/features_3_sec.csv')  # 직접 다운받아서 내 디렉토리에 넣음.

    X = df.drop(columns=['filename', 'length', 'label'])
    y = df['label']  # 장르명

    scaler = sklearn.preprocessing.MinMaxScaler()
    np_scaled = scaler.fit_transform(X)

    X = pd.DataFrame(np_scaled, columns=X.columns)

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=2021)

    xgb = XGBClassifier(n_estimators=1000, learning_rate=0.05)  #테스트를 위해 n_esti를 10으로 설정, 본래 1000
    xgb.fit(X_train, y_train)  # 학습

    y_preds = xgb.predict(X_test)  # 검증
    print('Accuracy: %.2f' % accuracy_score(y_test, y_preds))

    # save model
    joblib.dump(xgb, "model.json")

def main():
    learning()


main()
