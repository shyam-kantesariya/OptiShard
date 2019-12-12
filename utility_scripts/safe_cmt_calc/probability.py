import sys
from math import floor, factorial

n=int(sys.argv[1])
nf=int(floor(n/4))
#cmt_size=int(sys.argv[2])

print "nf: " + str(nf)

def nPr(n,r):
  return float(factorial(n))/float(factorial(n-r)*factorial(r))

def probability(m):
  deno=1
  nume=1
  for i in range(0,m):
    deno *= (n-i)
  #print "deno: " + str(deno)

  for i in range(0,int(floor(m/3))):
    nume *= (nf-i)
  #print "nf multiplication: " + str(nume)
  for i in range(0,m-int(floor(m/3))):
    nume *= (n-nf-i)
  #print "nume: " + str(nume)

  return float(float(nume)/float(deno)) * nPr(m,int(floor(m*2/3)))

for i in range(3,n,2):
  print "cmt size: " + str(i) + " \t\tprobability:" + str(probability(i))
