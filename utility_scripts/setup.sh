cp /home/s_kante/research/code/research/scala/blockchain/target/scala-2.12/blockchain_research-assembly-1.0.jar blockchain.jar
sh -x $1.sh $2

exit 0

if [ $1 = "all" ]
then
  ./all.sh
fi

if [ $1 = "run" ]
then
  ./run.s
fi

if [ $1 = "logs" ]
then

fi

if [ $1 = "kill" ]
then

fi
