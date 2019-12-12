import os
import sys
cmd = "sh parseLogs.sh "+ sys.argv[1]
os.system(cmd)
ts_max=dict()
ts_min=dict()
with open(sys.argv[1]+'/timings') as f:
    for line in f:
        tokens = line.split(" ")
        test = int(tokens[0])
        result = long(tokens[1])
        if ts_max.has_key(test):
            ts_max[test] = max(ts_max.get(test), result)
        else:
            ts_max[test] = result
        if ts_min.has_key(test):
            ts_min[test] = min(ts_min.get(test), result)
        else:
            ts_min[test] = result
    for key in ts_max.keys():
        diff = ts_max.get(key) - ts_min.get(key)
        print "Max:" + str(ts_max.get(key)) + " Min:" + str(ts_min.get(key))
        print str(key) + "," + str(diff)
