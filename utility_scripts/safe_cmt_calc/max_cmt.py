import sys
import math
N=999
def calc(i):
  f=int(math.ceil(i*9.99))
  for c in range(1,int(math.floor(N/4)),2):
    cn=int(math.floor(N/c))
    a=int(math.floor((cn-1)/3))+1
    condition = a*((c+1)/2)
    flg=1
#    if condition > 0:
#      flg = -1
#    while condition < 0:
#      c=c+2
#      cn=math.floor(N/c)
#      a=math.floor((cn-1)/3)+1
#      condition = f-a*math.ceil(c/2)
    print(str(f) + "," + str(c) + "," + str(condition) + "," + str(cn))
calc(20)
#for i in range(0,25):
