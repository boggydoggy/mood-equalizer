import os
import socket
import time
from src import convert

# Local host
HOST = '192.168.0.6'
PORT = 8888

while True:
    # Address family : IPv4, Socket type: TCP
    serverSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    serverSocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    serverSocket.bind((HOST, PORT))
    serverSocket.listen()

    print('Waiting...')
    # accept 함수에서 대기하다가 클라이언트가 접속하면 새로운 소켓 리턴
    clientSocket, addr = serverSocket.accept()
    print('Connected by', addr)

    # Receiving a music title
    receivedNameData = clientSocket.recv(1024)
    receivedFileName = receivedNameData.decode("utf-8")

    # Receiving a file
    receiveBuffer = clientSocket.recv(1024)
    dataReceived = 0

    nowDir = os.getcwd()

    with open(receivedFileName + ".wav", "wb") as f:
        try:
            while receiveBuffer:
                f.write(receiveBuffer)
                dataReceived += len(receiveBuffer)
                receiveBuffer = clientSocket.recv(1024)
        except Exception as ex:
            print(ex)

    print('%s file transfer complete.' % receivedFileName)
    print('Total bytes: %d' % dataReceived)

    clientSocket.close()
    serverSocket.close()

    print()
    genre = convert.convert(receivedFileName)
    ##플래그 신호를 줘야한다.
    print()

    # Reopen server
    serverSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    serverSocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    serverSocket.bind((HOST, PORT))
    serverSocket.listen()

    print('Waiting...')
    # accept 함수에서 대기하다가 클라이언트가 접속하면 새로운 소켓 리턴
    clientSocket, addr = serverSocket.accept()
    print('Connected by', addr)
    # Sending a music title
    sentFileName = 'new_'+receivedFileName+'.wav'


    # Sending a file
    clientSocket.sendall(sentFileName.encode("utf-8"))
    time.sleep(1)
    clientSocket.sendall(genre.encode("utf-8"))
    time.sleep(2)
    dataSent = 0

    print()

    with open('new_'+receivedFileName+'.wav', 'rb') as f:
        try:
            sendBuffer = f.read(1024)
            while sendBuffer:
                dataSent += clientSocket.send(sendBuffer)
                sendBuffer = f.read(1024)
        except Exception as e:
            print(e)

    print('%s file transfer complete.' % sentFileName)
    print('Total bytes: %d' % dataSent)

    clientSocket.close()
    serverSocket.close()





