import sys
from random import *

total_acc = int(sys.argv[1]) + 1
#total_tx = sys.argv[2]
total_tx = 1000001

with open("account.csv","w") as f:
  for x in range(1,total_acc):
    f.write(str(x) + "," + str(randint(1,500)) + "\n")

with open("transaction.csv","w") as f:
  for x in range(1,total_tx):
    debit=randint(1,total_acc-1)
    credit = (debit + randint(1,total_acc-debit))
    f.write(str(x) + "," + str(credit) + "," + str(debit) + "," + str(randint(1, 250)) + ",0,0\n")

