cp /home/s_kante/research/code/research/scala/blockchain/target/scala-2.12/blockchain_research-assembly-1.0.jar blockchain.jar

if [ "$1" = 'master' ]
then
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@54.188.168.62:/home/ubuntu/
fi

if [ $1 = "manager" ]
then
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@34.217.40.100:/home/ubuntu/
fi

if [ $1 = "worker" ]
then
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@34.222.28.17:/home/ubuntu/
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@54.244.155.85:/home/ubuntu/
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@54.202.23.84:/home/ubuntu/
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@34.219.167.243:/home/ubuntu/
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@52.26.195.114:/home/ubuntu/
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@34.222.138.146:/home/ubuntu/ 
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@52.11.75.74:/home/ubuntu/
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@34.219.166.205:/home/ubuntu/
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@34.217.72.128:/home/ubuntu/ 
fi

if [ $1 = "all" ]
then
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@54.188.11.218:/home/ubuntu/
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@54.245.58.202:/home/ubuntu/
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@52.12.173.128:/home/ubuntu/
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@52.10.111.117:/home/ubuntu/
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@54.186.60.58:/home/ubuntu/
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@54.244.24.94:/home/ubuntu/
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@34.219.79.235:/home/ubuntu/
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@52.43.203.70:/home/ubuntu/
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@54.202.78.82:/home/ubuntu/
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@18.236.170.210:/home/ubuntu/
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@54.190.31.144:/home/ubuntu/
  scp -i ~/.ssh/blockchain.pem ~/blockchain/blockchain.jar ubuntu@52.41.213.67:/home/ubuntu/
fi

if [ $1 = "run" ]
then
  ssh -i ~/.ssh/blockchain.pem ubuntu@54.188.11.218 java -jar blockchain.jar master ip-172-31-17-224.us-west-2.compute.internal > logs/master 2>&1 &
  ssh -i ~/.ssh/blockchain.pem ubuntu@54.245.58.202 java -jar blockchain.jar manager ip-172-31-47-76.us-west-2.compute.internal > logs/manager1 2>&1 &
  ssh -i ~/.ssh/blockchain.pem ubuntu@52.12.173.128 java -jar blockchain.jar manager ip-172-31-19-147.us-west-2.compute.internal > logs/manager2 2>&1 &
  ssh -i ~/.ssh/blockchain.pem ubuntu@52.10.111.117 java -jar blockchain.jar worker ip-172-31-41-55.us-west-2.compute.internal > logs/worker1 2>&1 &
  ssh -i ~/.ssh/blockchain.pem ubuntu@54.186.60.58 java -jar blockchain.jar worker ip-172-31-45-223.us-west-2.compute.internal > logs/worker2 2>&1 &
  ssh -i ~/.ssh/blockchain.pem ubuntu@54.244.24.94 java -jar blockchain.jar worker ip-172-31-39-61.us-west-2.compute.internal > logs/worker3 2>&1 &
  ssh -i ~/.ssh/blockchain.pem ubuntu@34.219.79.235 java -jar blockchain.jar worker ip-172-31-25-93.us-west-2.compute.internal > logs/worker4 2>&1 &
  ssh -i ~/.ssh/blockchain.pem ubuntu@52.43.203.70 java -jar blockchain.jar worker ip-172-31-31-247.us-west-2.compute.internal > logs/worker5 2>&1 &
  ssh -i ~/.ssh/blockchain.pem ubuntu@54.202.78.82 java -jar blockchain.jar worker ip-172-31-21-139.us-west-2.compute.internal > logs/worker6 2>&1 &
  ssh -i ~/.ssh/blockchain.pem ubuntu@18.236.170.210 java -jar blockchain.jar worker ip-172-31-21-66.us-west-2.compute.internal > logs/worker7 2>&1 &
  ssh -i ~/.ssh/blockchain.pem ubuntu@54.190.31.144 java -jar blockchain.jar worker ip-172-31-18-179.us-west-2.compute.internal > logs/worker8 2>&1 &
  ssh -i ~/.ssh/blockchain.pem ubuntu@52.41.213.67 java -jar blockchain.jar worker ip-172-31-29-49.us-west-2.compute.internal > logs/worker9 2>&1 &
fi

if [ $1 = "logs" ]
then
  ssh -i ~/.ssh/blockchain.pem ubuntu@54.188.11.218 rm -f /home/ubuntu/logs/*
  ssh -i ~/.ssh/blockchain.pem ubuntu@54.245.58.202 rm -f /home/ubuntu/logs/*
  ssh -i ~/.ssh/blockchain.pem ubuntu@52.12.173.128 rm -f /home/ubuntu/logs/*
  ssh -i ~/.ssh/blockchain.pem ubuntu@52.10.111.117 rm -f /home/ubuntu/logs/*
  ssh -i ~/.ssh/blockchain.pem ubuntu@54.186.60.58 rm -f /home/ubuntu/logs/*
  ssh -i ~/.ssh/blockchain.pem ubuntu@54.244.24.94 rm -f /home/ubuntu/logs/*
  ssh -i ~/.ssh/blockchain.pem ubuntu@34.219.79.235 rm -f /home/ubuntu/logs/*
  ssh -i ~/.ssh/blockchain.pem ubuntu@52.43.203.70 rm -f /home/ubuntu/logs/*
  ssh -i ~/.ssh/blockchain.pem ubuntu@54.202.78.82 rm -f /home/ubuntu/logs/*
  ssh -i ~/.ssh/blockchain.pem ubuntu@18.236.170.210 rm -f /home/ubuntu/logs/*
  ssh -i ~/.ssh/blockchain.pem ubuntu@54.190.31.144 rm -f /home/ubuntu/logs/*
  ssh -i ~/.ssh/blockchain.pem ubuntu@52.41.213.67 rm -f /home/ubuntu/logs/*
fi

if [ $1 = "kill" ]
then
  ssh -i ~/.ssh/blockchain.pem ubuntu@54.188.11.218 "sudo kill \`sudo lsof -t -i:12345\`"
  ssh -i ~/.ssh/blockchain.pem ubuntu@54.245.58.202 "sudo kill \`sudo lsof -t -i:12345\`"
  ssh -i ~/.ssh/blockchain.pem ubuntu@54.245.58.202 "sudo kill \`sudo lsof -t -i:12347\`"
  ssh -i ~/.ssh/blockchain.pem ubuntu@52.12.173.128 "sudo kill \`sudo lsof -t -i:12345\`"
  ssh -i ~/.ssh/blockchain.pem ubuntu@52.12.173.128 "sudo kill \`sudo lsof -t -i:12347\`"
  ssh -i ~/.ssh/blockchain.pem ubuntu@52.10.111.117 "sudo kill \`sudo lsof -t -i:12345\`"
  ssh -i ~/.ssh/blockchain.pem ubuntu@54.186.60.58 "sudo kill \`sudo lsof -t -i:12345\`"
  ssh -i ~/.ssh/blockchain.pem ubuntu@54.244.24.94 "sudo kill \`sudo lsof -t -i:12345\`"
  ssh -i ~/.ssh/blockchain.pem ubuntu@34.219.79.235 "sudo kill \`sudo lsof -t -i:12345\`"
  ssh -i ~/.ssh/blockchain.pem ubuntu@52.43.203.70 "sudo kill \`sudo lsof -t -i:12345\`"
  ssh -i ~/.ssh/blockchain.pem ubuntu@54.202.78.82 "sudo kill \`sudo lsof -t -i:12345\`"
  ssh -i ~/.ssh/blockchain.pem ubuntu@18.236.170.210 "sudo kill \`sudo lsof -t -i:12345\`"
  ssh -i ~/.ssh/blockchain.pem ubuntu@54.190.31.144 "sudo kill \`sudo lsof -t -i:12345\`"
  ssh -i ~/.ssh/blockchain.pem ubuntu@52.41.213.67 "sudo kill \`sudo lsof -t -i:12345\`"
fi
