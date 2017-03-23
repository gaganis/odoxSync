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
device is heavily resourse constrained and both on it's
network speed and it's cpu power. Also the need was to get
as many of the changes made to the files, yet absolute
persission was not important. It was acceptable for some
parts of the files that changed to not be discovered and
transferred if a higher rate of discovery could be achieved.

## Main idea - Optimism

The main idea of this project is the adoption of an
optimistic approach to look at changing files along with a
pessimistic precise approach. The files are conseptually
divided in regions, currently 1MB in size. For each file at
both the server two digests are calculated

 - The first one uses a very rudimentary hash algorithm
   calculated on a small sample of the region - 4KB
 - The second one uses a SHA256 hash of the entire region

The above calculations happen in parallel(using multiple
threads), also increasing the multicore utilization of the
rpi. 

## Other optimizations

### Ochillating transfer candidate queue

One thing that I found about the rpi is that it's network
transfer rate drops dramaticaly when cpu utilization is
high. This is not related to the jvm. Even a simple static
http server drops it's transfer rate when other processes
are heavily using the CPU.

Because of this a high data transfer rate could not be
achieved when other threads were using the CPU to calculate
digests. For this an ochillating queue implementation is
used to stop digest calculation when there are many regions
discoved to be transferred.

### File modification time skipping

The server also tracks  the file modification timestamp for
each calculation and associates it to fast/slow digests so
they are not recalculated when the file was not reported as
changed from the OS.

## Quick Start

TBD

## Building

TBD

## Previous approaches - Other tools

Before writing this project I attempted to use rsync over
ssh. Rsync is a general purpose tool that I really like and
I have used it extensivelly. I have found that in this case
it's use of encryption and it's change discovery
algorithm(rolling checksum) that assumes that it must be
100% precise made very slow(about 2.5 MB/s when transfering
changes). 
 
