import random
import math
import sys, getopt
import platform

class Node:
    def __init__(self, id, isFaulty=False):
        self.id=id
        self.isFaulty=isFaulty

N=0
f=0
c=0
i=0
cn=0
optlist, args = getopt.getopt(sys.argv[1:], 'N:f:i:h', ["iteration=","cn=", "help"])
#print(optlist)

for opt, arg in optlist:
    if opt == '-N':
        N=int(arg)
    if opt == '-f':
        f=int(arg)
    if opt == '--cn':
        cn=int(arg)
    if opt in ('-i','--iteration'):
        i=int(arg)
    if opt in ('-h','--help'):
        print("-N <Total Nodes> -f <Faulty Nodes> -c <Total Committees, optional> -cn <Committee size> -i <iterations, default 1000000>")
        exit(0)

c=math.floor(N/cn)
params="N_{N}_f_{f}_cn_{cn}_c_{c}_i_{i}".format(N=N, f=f, cn=cn, c=c, i=i)
filepath=''
if platform.system() == "Windows":
    filepath="C:\\Users\\s_kante\\Documents\\gitrepo\\research\\warehouse\\probability\\output_"
else:
    filepath="/mnt/c/Users/s_kante/Documents/gitrepo/research/warehouse/probability/output_"

opfile=open(filepath + params+".txt","w")
opfile.write(params + "\n")
# opfile.close()
# exit(0)
majority=math.floor((cn-1)/3) + 1

for x in range(1,i):
    nodes=[]
    for x in range(1, N+1):
        nodes.append(Node(x))

    faultyNodes=random.sample(nodes,f)

    for faultyNode in faultyNodes:
        faultyNode.isFaulty=True

    memberCnt=0
    faultyCnt=0
    totalFaultyCmt=0
    cnt=0

    # for node in nodes:
    #    if cnt<100:
    #        node.isFaulty = True
    #    cnt += 1

    for node in nodes:
        if memberCnt >= cn:
            memberCnt = 0
            if faultyCnt >= majority:
                totalFaultyCmt += 1
            faultyCnt = 0
        memberCnt += 1
        if node.isFaulty == True:
            faultyCnt += 1
        cnt += 1

    if totalFaultyCmt>0:
        opfile.write("Total faulty Committees: {a}".format(a=totalFaultyCmt) + "\n")
opfile.close()