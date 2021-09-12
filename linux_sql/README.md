# Linux Cluster Monitoring Agent

# Introduction
The Linux Cluster Monitoring Agent is used to document the hardware specifications of each node/server 
in a cluster connected through a switch. It also tracks the resource usage of each node in realtime using tool called 
crontab. This data is stored on an RDBMS database, PostgreSQL, for analysis and further study of the cluster by the Jarvis 
Linux Cluster Administration (LCA) team. The tools/technologies used:
- Linux Command Line, 
- Bash Scripts, 
- Docker, 
- Github, 
- IntelliJ 
- PostgreSQL.

# Quick Start
###1. Start PostgreSQL instance
The first step to getting the Linux Monitoring Agent up and running is to provision a psql instance 
using docker. By running `psql_docker.sh` script, the psql container can be created and started.
```bash
#create the docker container using PostgreSQL image 
./scripts/psql_docker.sh create psql_username psql_password

# Start the docker psql container
./scripts/psql_docker.sh start
```
###2. Create database tables
In order for the hard specifications and resource usage data to be stored for later analysis, 2 tables 
must be created in the host_agent database.
```bash
psql -h localhost -U postgres -d host_agent -f sql/ddl.sql
```
###3. Inserting hardware specification data into Database
Using the `host_info.sh` script will insert all the hardware specifications obtained 
from the node/computer into `host_info` table.
```bash
./scripts/host_info.sh psql_host psql_port db_name psql_user psql_password
```
###4. Inserting hardware usage data into Database
Using the `host_usage.sh` script will insert the usage data from the node into 
the `host_usage` table. This script will be automated to run every minute for realtime 
data.
```bash
./scripts/host_usage.sh psql_host psql_port db_name psql_user psql_password
```
###5. Setting up Crontab for real-time data
Crontab will create a job used to run the `data_usage.sh` script repeatedly every minute for
continuous data.
```bash
#create the job for the `data_usage.sh`
crontab -e

#set up the job by adding this line to the file
* * * * * bash /home/centos/dev/jrvs/bootcamp/linux_sql/host_agent/scripts/host_usage.sh localhost 5432 
host_agent postgres password > /tmp/host_usage.log

#check if the job was created
crontab -l

#verifiy if the crontab is running by checking log file
cat /tmp/host_usage.log
```

# Implemenation
- First, we need to provision a psql instance that contains a database, which is done using 
Docker. The `psql_docker.sh` script creates the psql docker container and allow for the start and 
stopping of the container.
- With the implementation of the `ddl.sql` script, the two tables for can be created in the `host_agent` 
database.
- Using the `host_info.sh` and `host_usage.sh` scripts allows for the collection of hardware data and resource usage data 
from each node on the cluster. The data is also inserted into the tables in the database.
- With crontab the `host_usage` script can collect real-time data.
- Solve business questions in `queries.sql` from the cluster.
Discuss how you implement the project.
## Architecture
![my image](./assets/architecture.jpg)

## Scripts
- `psql_docker.sh`: Provision a psql instance using docker
```bash
#create a docker container using postgresql image with the chosen username and password.
./scripts/psql_docker.sh create psql_user psql_password

#start the stopped psql docker container that was created.
./scripts/psql_docker.sh start

#stop the started psql docker container
./scripts/psql_docker.sh stop
```
- `host_info.sh`: Collects the hardware specifications data from the host/node it is run on and inserts 
it into the `host_info` table in `host_agent` database found on the psql instance.
```bash
#run the `host_info.sh` script
#required parameters:
  #psql_host: host name the psql instance is running on
  #psql_port: port used to communicate with psql instance
  #db_name: database the tables are created in
  #psql_user: psql instance user name
  #psql_password: psql instance's password
./scripts/host_info.sh psql_host psql_port db_name psql_user psql_password
```
- `host_usage.sh`: Collects the resource usage data from the host/node and inserts it into `host_usage` table 
in the `host_agent` database.
```bash
#run the `host_usage.sh` scripts
#required parameters:
  #psql_host: host name the psql instance is running on
  #psql_port: port used to communicate with psql instance
  #db_name: database the tables are created in
  #psql_user: psql instance user name
  #psql_password: psql instance's password
./scripts/host_usage.sh psql_host psql_port db_name psql_user psql_password

#example using the parameters
./scripts/host_usage.sh localhost 5432 host_agent postgres password
```
- crontab: Used to automate the `host_usage.sh` script to run every minute to obtain realtime data.
```bash
#edit crontab jobs
crontab -e

#add this to the opened file in crontab
* * * * * bash /home/centos/dev/jrvs/bootcamp/linux_sql/host_agent/scripts/host_usage.sh localhost 5432 host_agent postgres password > /tmp/host_usage.log
```
- `queries.sql`: Queries that are created to help manage the cluster of nodes better and 
aid in future analysis.
```bash
# Business Questions

# 1. Group the hosts/nodes based of their hardware specifications, (specifically number of cpu)

# 2. Calculate the average memory usage in 5 minute intervals.

# 3. Find 
```
- `ddl.sql`: Automatically creates two tables called `host_info` and `host_usage` that will store the data in the `host_agent` 
database.
```bash
#run the sql script to create the tables in the `host_agent` database within the psql docker container.
#required parameters:
  #psql_host: host name the psql instance is running on
  #psql_user: psql instance user name
  #db_name: database the tables are created in
  #sql/ddl.sql: file used to create the two tables
psql -h psql_host -U psql_user -d db_name -f sql/ddl.sql

#example using parameters
psql -h localhost -U postgres -d host_agent -f sql/ddl.sql
```

## Database Modeling
The database used `host_agent` consists of 2 tables called `host_info` and `host_usage`.
`host_info`:

Attribute | Description | Data Type | Constraints 
------------ | ------------- | -------- | ------
`id` | Auto incremented id that is unique for each linux node, along with being the primary key | `SERIAL` | `PRIMARY KEY NOT NULL`
`hostname` | Unique fully qualified name of each linux host/node | `VARCHAR` | `UNIQUE NOT NULL`
`cpu_number` | Number of CPU's on the linux node | `INT` | `NOT NULL`
`cpu_architecture` | The architecture of the CPU | `VARCHAR` | `NOT NULL`
`cpu_model` | The model name of the CPU on node | `VARCHAR` | `NOT NULL`
`cpu_mhz` | Clock speed of the CPU on the computer in MHz| `FLOAT` | `NOT NULL`
`L2_cache` | Level 2 cache in kB | `INT` | `NOT NULL`
`total_mem` | Total memory on the node in kB | `INT` | `NOT NULL`
`"timestamp"` | Time when data is retrieved | `TIMESTAMP` | `NOT NULL`

`host_info`:

Attribute | Description | Data Type | Constraints
------------ | ------------- | -------- | ------
`"timestamp"` | Time when data is retrieved | `TIMESTAMP` | `NOT NULL`
`host_id` | Id of the host linked with id from `host_info` table | `SERIAL` | `FOREIGN KEY NOT NULL`
`memory_free` | Amount of free memory in MB | `INT` | `NOT NULL`
`cpu_idle` | Percentage of CPU processor idle time | `INT` | `NOT NULL`
`cpu_kernel` | Percentage of CPU system time | `INT` | `NOT NULL`
`disk_io` | Number of disk I/O | `INT` | `NOT NULL`
`disk_available` | Available disk space in root directory in MB | `INT` | `NOT NULL`

# Test
- Tested the bash scripts by running them with many test cases. For example, with the `psql_docker` script, all parameter 
options were checked, along with user and password.
- Tested SQL queries in `queries.sql` with DBeaver program and used possible test data to check whether 
the queries gave the correct results.

# Deployment
- Used Github for software configuration management using GitFlow for workflow of branches.
- Used Docker to create a container that provisions a psql instance.
- Used Intellij to implement the scripts and sql files.

# Improvements
- Gather more data for each node through more columns in the tables or having additional tables, allowing for 
deeper analysis and reports being created by the Jarvis Linux Cluster Administration team
- Backup the `host_agent` database to be prepared for possible problems that can arise such as upgrades that 
go bad, data table corruption and other system problems.
- Create a GUI for easier access and understand of the Monitoring Agent as visual representation of data is recognized 
faster than text, with non-programmers understanding it easier.
- Possible combine the quick start steps into one bash script that completes the setup.