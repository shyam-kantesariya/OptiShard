 # Probability that a committee becomes faulty due to majority of the nodes turning faulty
 # Cn committee size, cn-1/3 + 1 members decide to become corrupted right from the same transaction
 # Tx number of transactions 

import random
import math
import platform
import sys
import getopt


if len(sys.argv) <4:
    print("-t or --tx <Total Transactions> -cn <Committee size> -i or --iteration <iterations>")
    exit(0)

totalTx=0
cn=0
majority = 0 
i=0

optlist, args = getopt.getopt(sys.argv[1:], 't:i:h', ["tx=", "iteration=", "cn=", "help"])
#print(optlist)

for opt, arg in optlist:
    if opt in ('-t', '--tx'):
        totalTx=int(arg)
    if opt == '--cn':
        cn=int(arg)
        majority = math.floor((cn-1)/3)+1
    if opt in ('-i','--iteration'):
        i=int(arg)
    if opt in ('-h','--help'):
        print("-t or --tx <Total Transactions> -cn <Committee size> -i or --iteration <iterations>")
        exit(0)

params="cn_{cn}_tx_{tx}_iterations_{i}".format(cn=cn, tx=totalTx,i=i)
filepath=""
if platform.system() == "Windows":
    filepath="C:\\Users\\s_kante\\Documents\\gitrepo\\research\\warehouse\\probability\\random_tx\\output_"+params+".txt"
else:
    filepath="/mnt/c/Users/s_kante/Documents/gitrepo/research/warehouse/probability/random_tx/output_"+params+".txt"

opfile = open(filepath,"w")
opfile.write(params + "\n")

tx = range(0,totalTx)

temp = [1,2,3]

for x in range(0,i):
    faulty_at = dict()
    for node in range(0,cn):
        f_tx = random.choice(tx)
        #f_tx = random.choice(temp)
        #opfile.write("node: " + str(node) + " tx: " + str(f_tx))
        f_tx_cnt = faulty_at.get(f_tx,0)+1
        faulty_at[f_tx] = f_tx_cnt
    for key in faulty_at.keys():
        if faulty_at.get(key) >= majority:
            opfile.write("Iter {i}: Total {n} nodes transited at the same tx number {tx_no} \n".format(n=faulty_at.get(key), tx_no=key, i=x))
opfile.close()