import json
import os
from pprint import pprint

os.system('rm -f aws.json')
os.system('rm -f nodes.csv')
regions=dict()
with open("regions.csv") as f:
  for line in f:
    tpl=line.split(",")
    regions[tpl[0]] = tpl[1]

#regions=["ap-south-1","us-east-2","us-west-1","ca-central-1","us-west-2","ap-southeast-1","ap-south-1"]
for region in regions.keys():
  cmd="aws ec2 --region " + region + " describe-instances > aws.json"
  os.system(cmd)
  #origin_cd=regions.get(region).rstrip('\n')
  with open("aws.json") as f:
    data=json.load(f)
  groups=data.get("Reservations")
  with open("nodes.csv", "a") as f:
    for grp in groups:
      for inst in grp.get("Instances"):
        state=inst.get("State").get("Name")
        if state != "running":
          continue
        ip=inst.get("PublicIpAddress")
#        dns=inst.get("PrivateDnsName")
        dns=inst.get("PrivateIpAddress")
        try:
          name=inst.get("Tags")[0].get("Value")
        except:
          name="Worker"
        f.write(name + "," + ip + "," + dns + "," + regions.get(region))
