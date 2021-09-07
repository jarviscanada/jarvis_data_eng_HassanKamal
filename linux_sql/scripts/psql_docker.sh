#! /bin/bash

#setup 3 arguments to variables
cmd=$1
db_username=$2
db_password=$3

#start docker server if not running
sudo systemctl status docker || systemctl start docker

#switch cases for possible options for docker; create, start, stop
case $cmd in

  #create a docker container
  create)
    #check whether container has already been created
    if [ "$(docker container ls -a -f name=jrvs-psql | wc -l)" = "2" ]; then
      echo "Error: \"jrvs-psql\" container has already been created" >&2
      exit 1
    fi

    #check whether the database username and password have been given as arguments
    if [ "$#" -ne 3 ]; then
      echo "Error: db_username or db_password not passed as arguments" >&2
      exit 1
    fi

    #create volume then container using psql image
    docker volume create pgdata
    docker run --name jrvs-psql -e POSTGRES_PASSWORD=${db_password} -e POSTGRES_USER=${db_username} -d -v pgdata:/var/lib/postgresql/data -p 5432:5432 postgres
    exit $?
    ;;

  #start or stop the container
  start | stop)
    #check whether the conatiner has been created before starting and stopping
    if [ "$(docker container ls -a -f name=jrvs-psql | wc -l)" != "2" ]; then
      echo "Error: \"jrvs-psql\" container has not been created" >&2
      exit 1
    fi

    #start or stop the container based of the argument provided
    docker container "$cmd" jrvs-psql
    exit $?
    ;;

  #deal with invalid option
  *)
    #error message for invalid option
    echo "Error: invalid option for docker. either choose create, start or stop"
    exit 1
    ;;
esac

exit 1