Simple Bitcoin Miner
========================

## What is it?
A Naive bitcoin miner written in Scala using Akka Actors

Bitcoins (see http://en.wikipedia.org/wiki/Bitcoin) are the most popular crypto-currency in common use. At their heart, bitcoins use the hardness of cryptographic hashing (for a reference see http://en.wikipedia.org/wiki/Cryptographic hash function) to ensure a limited “supply” of coins. In particular, the key component in a bitcoin is an input that, when “hashed” produces an output smaller than a target value. In practice, the comparison values have leading 0’s, thus the bitcoin is required to have a given number of leading 0’s (to ensure 3 leading 0’s, you look for hashes smaller than 0x001000... or smaller or equal to 0x000ff....

The goal of this project is to use Scala and the actor model to build a good solution to this problem that runs well on multi-core machines. This project was also done to learn Scala, Akka Actor model, SBT and, the basics of bitcoin mining. 

The project follows a simple master-workers architecture. Master distributes work to the worker nodes as they connect, worker nodes report back to master with mined coins. Master consolidates all the results. 

---

## How to use?

1. Run in sbt, build.sbt file in included

2. Use the following command to start the master: 
  * `sbt run <numLeadingZeros>` 
  * numLeadingZeros is the required number of leading zeroes in the mined bitcoin. 

3. To Start the worker: 
  * `sbt run <masterIPAddress>`

4. If no IP address is provided then it starts in the Master mode otherwise it starts in the worker mode.

5. To change any configuration settings for remoting, edit server_application.conf for Master and client_application.conf for Worker.
