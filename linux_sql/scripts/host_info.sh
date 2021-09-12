#! /bin/bash

#setup arguments to variables
psql_host=$1
psql_port=$2
db_name=$3
psql_user=$4
psql_password=$5

#check whether the correct number of arguments passed in the script
if [ "$#" -ne 5 ]; then
    echo "Illegal number of parameters"
    exit 1
fi

#parse hardware specification using bash cmds and set to variables
lscpu_out=`lscpu`
hostname=$(hostname -f)
cpu_number=$(echo "$lscpu_out" | egrep "^CPU\(s\):" | awk '{print $2}' | xargs)
cpu_architecture=$(echo "$lscpu_out" | egrep "Architecture:" | awk '{print $2}' | xargs)
cpu_model=$(echo "$lscpu_out" | egrep "Model name:" | awk '{print $3,$4,$5,$6,$7}' | xargs)
cpu_mhz=$(echo "$lscpu_out" | egrep "CPU MHz" | awk '{print $3}' | xargs)
l2_cache=$(echo "$lscpu_out" | egrep "L2 cache:" | awk '{print substr( $3, 1, length($3)-1)}' | xargs)
total_mem=$(cat /proc/meminfo | egrep "MemTotal:" | awk '{print $2}' | xargs)
timestamp=$(vmstat -t | sed -n '3p' | awk '{print $18,$19}'| xargs)

#create insert statement
insert_stmt="INSERT INTO host_info(hostname, cpu_number, cpu_architecture, cpu_model, cpu_mhz, l2_cache, total_mem, timestamp)
VALUES ('$hostname', $cpu_number, '$cpu_architecture', '$cpu_model', $cpu_mhz, $l2_cache, $total_mem, '$timestamp')"

#set password for default user and execute insert statement through psql CLI tool
export PGPASSWORD=$psql_password
psql -h "$psql_host" -p "$psql_port" -U "$psql_user" -d "$db_name" -c "$insert_stmt"

exit $?