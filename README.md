# odoxSync - An optimistic, remote directory synchronizer

--- 

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



## Building

Clone the project:
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

Enter the project dir:
```
$ cd odoxSync
```

Build the project runnable distributions
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

The distribution files will be under the server and client distribution builds:
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
 
