# odoxSync - An optimistic, remote directory synchronizer

This project provides a server and client that can be used
to synchronize a directory of continuously changing files on
a server to clients over the network. It has been
specifically designed to be very fast on slow hardware(ie
Raspberry Pi) using an optimistic approach.

## The problem

This project came out of the need to sync files that reside
on my rpi and keep them in sync while they change. This
device is heavily resource constrained and both on it's
network speed and it's cpu power. Also the need was to get
as many of the changes made to the files, yet absolute
precision was not important. It was acceptable for some
parts of the files that changed to not be discovered and
transferred if a higher rate of discovery could be achieved.

## Main idea - Optimism

The main idea of this project is the adoption of an
optimistic approach to look at changing files along with a
pessimistic precise approach. The files are conceptually
divided in regions, currently 1MB in size. For each file at
both the server two digests are calculated

 - The first one uses a very rudimentary hash algorithm
   calculated on a small sample of the region - 4KB
 - The second one uses a SHA256 hash of the entire region

The above calculations happen in parallel(using multiple
threads), also increasing the multicore utilization of the
rpi. 

## Other optimizations

### Oscillating transfer candidate queue

One thing that I found about the rpi is that it's network
transfer rate drops dramatically when cpu utilization is
high. This is not related to the jvm. Even a simple static
http server drops it's transfer rate when other processes
are heavily using the CPU.

Because of this a high data transfer rate could not be
achieved when other threads were using the CPU to calculate
digests. For this an oscillating queue implementation is
used to stop digest calculation when there are many regions
discovered to be transferred.

### File modification time skipping

The server also tracks  the file modification timestamp for
each calculation and associates it to fast/slow digests so
they are not recalculated when the file was not reported as
changed from the OS.

## Quick Start

:exclamation: You need to have java available to run both the 
server and the client.

### Running server for directory `~/test`

__Grab__ _server.zip_ from the latest [release](https://github.com/gaganis/odoxSync/releases)
and upload it to the device you want to server files from.

__unzip the archive__
```
pi@raspbmc:~$ unzip  server.zip
Archive:  server.zip
   creating: server/
   creating: server/lib/
  inflating: server/lib/server.jar
  inflating: server/lib/odoxSync-buildjob-0.1.jar
  inflating: server/lib/commons-io-2.5.jar
  inflating: server/lib/guava-21.0.jar
  inflating: server/lib/jackson-core-2.8.4.jar
  inflating: server/lib/jackson-module-jaxb-annotations-2.8.4.jar
  inflating: server/lib/commons-lang3-3.5.jar
  inflating: server/lib/jersey-container-grizzly2-http-2.25.1.jar
  inflating: server/lib/jackson-annotations-2.8.0.jar
  inflating: server/lib/jackson-databind-2.8.4.jar
  inflating: server/lib/javax.inject-2.5.0-b32.jar
  inflating: server/lib/grizzly-http-server-2.3.28.jar
  inflating: server/lib/jersey-common-2.25.1.jar
  inflating: server/lib/jersey-server-2.25.1.jar
  inflating: server/lib/javax.ws.rs-api-2.0.1.jar
  inflating: server/lib/grizzly-http-2.3.28.jar
  inflating: server/lib/javax.annotation-api-1.2.jar
  inflating: server/lib/jersey-guava-2.25.1.jar
  inflating: server/lib/hk2-api-2.5.0-b32.jar
  inflating: server/lib/hk2-locator-2.5.0-b32.jar
  inflating: server/lib/osgi-resource-locator-1.0.1.jar
  inflating: server/lib/jersey-client-2.25.1.jar
  inflating: server/lib/jersey-media-jaxb-2.25.1.jar
  inflating: server/lib/validation-api-1.1.0.Final.jar
  inflating: server/lib/grizzly-framework-2.3.28.jar
  inflating: server/lib/hk2-utils-2.5.0-b32.jar
  inflating: server/lib/aopalliance-repackaged-2.5.0-b32.jar
  inflating: server/lib/javassist-3.20.0-GA.jar
  inflating: server/lib/javax.inject-1.jar
   creating: server/bin/
  inflating: server/bin/server.bat
  inflating: server/bin/server
```

__Enter the bin directory of the extracted distribution zip file__
```
pi@raspbmc:~$ cd server/bin
pi@raspbmc:~/server/bin$
```

__Start the server to serve directory `~/test`__ 

Replace parameter `~/test` with the directory you want to serve

```
pi@raspbmc:~/server/bin$ ./server ~/programming/
```

### Running the client

__Grab__ _client.zip_ from the latest [release](https://github.com/gaganis/odoxSync/releases)
and upload it to the device you want to transfer the files to.

__Unzip client.zip__
```
unzip client.zip 
```
__Enter the bin directory of the extracted distribution zip file__
```
cd client/bin
```
__Start the client__ 

Replace parameter `~/test` with the directory you want the files to be transferred
to. And replace `192.169.1.190` with the ip address of your server.
```
$ ./client 192.168.1.190:8081 ~/test 
Apr 10, 2017 12:22:18 PM com.giorgosgaganis.odoxsync.client.SyncClient start
INFO: Starting sync client at [/home/gaganis/test]
Apr 10, 2017 12:22:18 PM com.giorgosgaganis.odoxsync.client.net.RestClient getClientId
INFO: Retrieved clientId [207270713]
```


__
## Building

__Clone the project:__
```
$ git clone https://github.com/gaganis/odoxSync.git
Cloning into 'odoxSync'...
remote: Counting objects: 1577, done.
remote: Compressing objects: 100% (348/348), done.
remote: Total 1577 (delta 895), reused 1575 (delta 893), pack-reused 0
Receiving objects: 100% (1577/1577), 188.41 KiB | 73.00 KiB/s, done.
Resolving deltas: 100% (895/895), done.
Checking connectivity... done.
```

__Enter the project dir:__
```
$ cd odoxSync
```

__Build the project runnable distributions__
```
$ ./gradlew distZip
:compileJava
Note: /home/gaganis/IdeaProjects/odoxSync/src/com/giorgosgaganis/odoxsync/client/FileOperations.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
:processResources NO-SOURCE
:classes
:jar
:client:compileJava NO-SOURCE
:client:processResources NO-SOURCE
:client:classes UP-TO-DATE
:client:jar
:client:startScripts
:client:distZip
:server:compileJava NO-SOURCE
:server:processResources NO-SOURCE
:server:classes UP-TO-DATE
:server:jar
:server:startScripts
:server:distZip

BUILD SUCCESSFUL

Total time: 2.291 secs
```

__The distribution files will be under the server and client distribution builds:__
```
$ find . |grep zip
./server/build/distributions/server.zip
./client/build/distributions/client.zip
```

## Previous approaches - Other tools

Before writing this project I attempted to use rsync over
ssh. Rsync is a general purpose tool that I really like and
I have used it extensively. I have found that in this case
it's use of encryption and it's change discovery
algorithm(rolling checksum) that assumes that it must be
100% precise made very slow(about 2.5 MB/s when transfering
changes). 
 
