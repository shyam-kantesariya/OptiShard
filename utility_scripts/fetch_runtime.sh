if [ $# -eq 0 ]
then
  echo "Please pass path as argument"
  exit 1
fi

dir_path=$1
rm /tmp/start_time.csv
rm /tmp/start_time.csv

cnt=1
while [ 1 -eq 1 ] 
do
  curr_dir="$dir_path/$cnt"
  if [ -d $curr_dir ]
  then
    cd $curr_dir
    grep START_TIME NON_CORE_FOLLOWER*.log | awk '{print $1","$NF}' | sed 's/:INFO://g' > /tmp/start_time.csv
    grep END_TIME NON_CORE_FOLLOWER*.log | awk '{print $1","$NF}' | sed 's/:INFO://g' > /tmp/end_time.csv
    python ~/blockchain/fetch_runtime.py
  else
    exit 0
  fi
  cnt=$(($cnt+1))
#  echo $cnt
done

