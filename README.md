# OptiShard
OptiShard is a Scalable, Secured and Optimized blockchain architecture

Repository is organized as following:

src: contains the scala code to simulate OptiShard
logs: captured during our experimental runs on up to 800 EC2 nodes
utility_scripts: Contains all the scripts (python and linux shell) to execute an experiment on EC2 cluster and capture the runtime by parsing the logs
randomness: contains the python scripts to generate probabilistic experiments


Note:
We used SHA256 with ECDSA implemented in Java BouncyCastle library for the public key encryption.
For probabilistic experiments, we used random library provided in Python 3 distribution