grep -n "TIMESTAMP" $1/*/* | sed 's/\// /g' | awk '{print $2" "$NF}' > $1/timings
