iotester
=============

A standalone Java utility to test the network (TCP) and disk input/output performance of a cluster of machines on a network (ie. a not well understood cloud environment). 

After running for a set period of time, a histogram of the I/O performance (sampled every time period) is written to a file with key metrics such as the transfer rate mean and variance. 

Running the jar prints a usage statement which explains the parameters. 
