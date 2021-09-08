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

#parse linux resource usage using bash cmds and set to variables
hostname=$(hostname -f)
memory_free=$(vmstat --unit M -t | sed -n '3p' | awk '{print $4}'| xargs)
cpu_idle=$(vmstat -t | sed -n '3p' | awk '{print $15}'| xargs)
cpu_kernel=$(vmstat -t | sed -n '3p' | awk '{print $14}'| xargs)
disk_io=$(vmstat -d | sed -n '3p' | awk '{print $10}' | xargs)
disk_available=$(df -BM / | sed -n '2p' | awk '{print substr( $3, 1, length($3)-1)}' | xargs)
timestamp=$(vmstat -t  | sed -n '3p' | awk '{print $18,$19}'| xargs)

#create insert statement
insert_stmt="INSERT INTO host_usage(timestamp, host_id, memory_free, cpu_idle, cpu_kernel, disk_io, disk_available)
VALUES ('$timestamp', (SELECT id FROM host_info WHERE hostname = '$hostname'), $memory_free, $cpu_idle, $cpu_kernel, $disk_io, $disk_available)"

#set password for default user and execute insert statement through psql CLI tool
export PGPASSWORD=$psql_password
psql -h "$psql_host" -p "$psql_port" -U "$psql_user" -d "$db_name" -c "$insert_stmt"

exit $?