import sys
import math


def safe_cmt(f):
  flag=0
  for c in range(2,34):
    cn=math.floor(100/c)
    b=1
    x=1
    a=int(math.floor(cn/3))+1
    b=f-(x*a)
    while b>0:
      x+=1
      b=f-(x*a)
#      print "x val is: " + str(x)
    x-=1
    if (x>math.floor(c/2)) or (c%2==0 and x==math.floor(c/2)):
      print "faulty nodes: {f}, c: {c}, cn: {cn}, #bad cmt: {cmt}, b val: {b}, a val: {a}".format(f=f,c=c,cmt=x,b=b,cn=cn,a=a)
    #  flag=1
    #else:
    #  if flag==1:
    #    print "updating flag:{flg}  as faulty nodes: {f}, c: {c}, cn: {cn}, #bad cmt: {cmt}, b val: {b}, a val: {a}".format(f=f,c=c,cmt=x,b=b,cn=cn,a=a,flg=flag)
    #    flag=0
#      break

for f in range(10,30):
  safe_cmt(f)
#  break
#safe_cmt(49)
