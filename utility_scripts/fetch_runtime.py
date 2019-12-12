import os,sys
import math

start_time=dict()
end_time=dict()
duration=[]
with open("/tmp/start_time.csv") as f:
  for line in f:
    tpl = line.split(",")
    start_time[tpl[0]] = long(tpl[1])

with open("/tmp/end_time.csv") as f:
  for line in f:
    tpl = line.split(",")
    end_time[tpl[0]] = long(tpl[1])

for key in end_time.keys():
  duration.append(end_time.get(key) - start_time.get(key))

duration.sort()
#duration=[1,2,3,4]
total_ele = len(duration)
mid_idx=int(math.ceil(total_ele/2))
mid=float(duration[mid_idx])
if total_ele%2 == 0:
  mid=float((duration[mid_idx]+duration[mid_idx-1]))/2

print "Runtime: " + str(mid)
