Instructions:
Java main class is present in ChainReplication.java

Main Files:
1) com.stonybrook.bank.main.ChainReplication.java
This initiates the whole system.
Creates processes for bank servers.
Creates process for master.
Creates process for client. (Client process spawns threads for each client)
Communicates the server list to Master.

2) com.stonybrook.bank.client.ChainReplicationClient.java
Client process which generates all the client threads for all banks.
Initiates itemized requests from each client based on configuration of client.
Initiates randomized requests form each client based on configuration of client.

3) com.stonybrook.bank.server.ChainReplicationServer.java
Server process for each bank. 
May act as head or tail or an intermediate server depending upon logical positioning of server.
Maintains own instance of all the accounts.
Listens to both TCP and UDP requests.
Responds to clients only if the instance is of tail.
Processes the client requests only if the instance is of head.

4) com.stonybrook.bank.utils.ChainReplicationUtil.java
Utility to do the following:
a) Create custom loggers
b) TCP listening utility
c) TCP sending utility
d) UDP listening utility
e) UDP sending utility

5) com.stonybrook.bank.master.ChainReplicationMaster.java
Detects the failure of Servers.
Handles the failure of Head Server.
Handles the failure of Tail Server.
Handles the failure of Internal Server.
Handles the extension of chain if a new server gets alive.
Informs all the servers about the list of all other servers present in chain.
Informs all the clients about the head and tail of each chain.
Informs all the servers about the head and tail of each chain.

Bugs and Limitations:
Java: No known bugs

Language Comparison:
Java took more time to code, for setting up TCP and UDP connections.
Number of lines of code is relatively huge in Java when compared to DistAlgo. 

Contributions:
1) Phase 1, both the team members contributed equally.
2) Complete implementation in Java: (Phase2-Phase3-Phase4) Vishal Nayak (vishal.nayak@stonybrook.edu).
3) Complete implementation in DistAlgo: (Phase2-Phase3) Supritha Mundaragi (supritha.mundaragi@stonybrook.edu).

Other Comments:
The division of work between team members was based on implementation in each language.

Note on experience:
Vishal Nayak: 
I took up the Java part of the implementation.
The implementation has become intricate and huge.
The overall experience is that the time taken will be more in Java.
In my opinion, every change in the feature will bring in more code changes.
Taking care of TCP and UDP message handling has to be manually done in Java, unlike what I hear in DistAlgo.
Given another opportunity OR time (in other words), I would like to do this whole thing again in DistAlgo.