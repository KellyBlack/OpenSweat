#!/usr/bin/python

import socket
ip = "192.168.1.135"
port = 3517
message = b"//OpenSweat/fan/0/0"
message = b"//OpenSweat/query"
#message = b"//OpenSweat/status/0/1/"

sock = socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
#sock.sendto(message,(ip,port))
sock = socket.socket(socket.AF_INET,socket.SOCK_DGRAM,socket.IPPROTO_UDP)
sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
sock.sendto(message, ('<broadcast>', port))


receiver = socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
receiver.bind(("192.168.1.5",1+port))
print("Listening")
data, addr = receiver.recvfrom(1024)
print("Reveived: {0}".format(data))
