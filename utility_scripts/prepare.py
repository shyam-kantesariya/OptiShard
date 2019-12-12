import sys
from random import shuffle

data=[]
mngr_cns=sys.argv[1]
leader=sys.argv[2]
cmt_size=sys.argv[3]

with open("nodes.csv") as f:
  for line in f:
    tokens=line.split(",")
    tokens[3]=tokens[3].rstrip("\n")
    data.append(tokens)
    shuffle(data)

def all_func():
  with open("all.sh","w") as f:
    for tkn in data:
      f.write('scp -i ~/.ssh/blockchain_' + tkn[3] +'.pem ~/blockchain/blockchain.jar ubuntu@' + tkn[1] + ':/home/ubuntu/ &' + "\n")
  return

def run_func():
  with open("run.sh","w") as f:
    mgrcnt=1
    wrkrcnt=1
    for tkn in data:
      if "Master" in tkn[0]:
        f.write('ssh -i ~/.ssh/blockchain_' + tkn[3] + '.pem ubuntu@'+ tkn[1] + ' java -jar blockchain.jar master ' + tkn[1].rstrip("\n") + ' ' + mngr_cns + ' ' + leader + ' ' + cmt_size + ' > logs/master.log 2>&1 &' + "\n")
      elif "Manager" in tkn[0]:
        f.write('ssh -i ~/.ssh/blockchain_' + tkn[3] + '.pem ubuntu@'+ tkn[1] + ' java -jar blockchain.jar manager ' + tkn[1].rstrip("\n") + ' ' + leader + ' ' + cmt_size + ' > logs/manager' + str(mgrcnt) + '.log 2>&1 &' + "\n")
        mgrcnt += 1
      else:
        f.write('ssh -i ~/.ssh/blockchain_' + tkn[3] + '.pem ubuntu@'+ tkn[1] + ' java -jar blockchain.jar worker ' + tkn[1].rstrip("\n") + ' ' + leader + ' ' + cmt_size + ' > logs/worker' + str(wrkrcnt) + '.log 2>&1 &' + "\n")
        wrkrcnt += 1
  return

def logs_func():
  with open("logs.sh","w") as f:
    for tkn in data:
      f.write('ssh -i ~/.ssh/blockchain_' + tkn[3] +'.pem ubuntu@'+ tkn[1] + ' rm -f /home/ubuntu/logs/* &' + "\n")
  return

def kill_func():
  with open("kill.sh","w") as f:
    for tkn in data:
      f.write('ssh -i ~/.ssh/blockchain_' + tkn[3] + '.pem ubuntu@'+ tkn[1] + ' "sudo kill \`sudo lsof -t -i:12345\`"' + " & \n")
      if "Manager" in tkn[0]:
        f.write('ssh -i ~/.ssh/blockchain_' + tkn[3] + '.pem ubuntu@'+ tkn[1] + ' "sudo kill \`sudo lsof -t -i:12347\`"' + " & \n")
  return

def miners_func():
  existing = []
  with open("miner.csv") as f:
    for line in f:
      tkn=line.split(",")
      existing.append(tkn[0])
  with open("miner.csv","w") as f:
    for tkn in data:
      if tkn[1].strip("\n") not in existing:
        print 'ssh -o "StrictHostKeyChecking no" -i ~/.ssh/blockchain_' + tkn[3] +'.pem ubuntu@'+ tkn[1] + ' rm -f /home/ubuntu/logs/*'
      if "Master" in tkn[0]:
        f.write(tkn[1].strip("\n") + ",1000,1,NULL,NULL,12345,0\n")
      elif "Manager" in tkn[0]:
        f.write(tkn[1].strip("\n") + ",100,2,NULL,NULL,12345,0\n")
      else:
        f.write(tkn[1].strip("\n") + ",1,4,NULL,NULL,12345,0\n")
  return

def get_logs_func():
  with open("get.sh","w") as f:
    for tkn in data:
      f.write('scp -i ~/.ssh/blockchain_' + tkn[3] + '.pem ubuntu@' + tkn[1] + ':/home/ubuntu/logs/* ' + "$1/ & \n")
  return

#ssh -i ~/.ssh/blockchain.pem ubuntu@54.188.11.218 "sudo kill \`sudo lsof -t -i:12345\`"

all_func()
run_func()
logs_func()
kill_func()
miners_func()
get_logs_func()
