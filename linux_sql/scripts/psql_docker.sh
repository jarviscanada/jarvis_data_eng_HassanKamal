#! /bin/bash

cmd=$1
db_username=$2
db_password=$3

sudo systemctl status docker || systemctl start docker

case $cmd in

  create)
    if [ "$(docker container ls -a -f name=jrvs-psql | wc -l)" = "2" ]; then
      echo "jrvs-psql container has already been created" >&2
      exit 1
    fi

    if [ "$#" -ne 3 ]; then
      echo "db_username or db_password not passed as arguments" >&2
      exit 1
    fi

    docker volume create pgdata
    docker run --name jrvs-psql -e POSTGRES_PASSWORD=${db_password} -e POSTGRES_USER=${db_username} -d -v pgdata:/var/lib/postgresql/data -p 5432:5432 postgres
    exit $?
    ;;
esac

if [ "$(docker container ls -a -f name=jrvs-psql | wc -l)" != "2" ]; then
  echo "container failed to create" >&2
  exit 1
fi

if [ $1 = "start" ]; then
  docker container start jrvs-psql
  exit $?
fi

if [ $1 = "stop" ]; then
  docker container stop jrvs-psql
  exit $?
fi

if [[ $1 != "create" && $1 != "start" && $1 != "stop" ]]; then
  echo "Invalid option for docker. Either choose create, start or stop" >&2
  exit 1
fi

exit 0