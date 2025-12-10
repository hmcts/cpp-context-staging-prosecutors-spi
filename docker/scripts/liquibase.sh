echo "Running Liquibase"
dbServerName=$1
dbUserName=$2
dbPassword=$3
dbPort=${5:-5432}
java -jar event-repository-liquibase.jar --url=jdbc:postgresql://${dbServerName}:${dbPort}/stagingprosecutorsspieventstore?sslmode=require --username=${dbUserName} --password=${dbPassword} --logLevel=info update
if [ $? -ne 0 ]
then
    exit 1
else
    echo success!
fi
java -jar aggregate-snapshot-repository-liquibase.jar --url=jdbc:postgresql://${dbServerName}:${dbPort}/stagingprosecutorsspieventstore?sslmode=require --username=${dbUserName} --password=${dbPassword} --logLevel=info update
if [ $? -ne 0 ]
then
    exit 1
else
    echo success!
fi
java -jar event-buffer-liquibase.jar --url=jdbc:postgresql://${dbServerName}:${dbPort}/stagingprosecutorsspiviewstore?sslmode=require --username=${dbUserName} --password=${dbPassword} --logLevel=info update
if [ $? -ne 0 ]
then
    exit 1
else
    echo success!
fi
java -jar event-tracking-liquibase.jar --url=jdbc:postgresql://${dbServerName}:${dbPort}/stagingprosecutorsspiviewstore?sslmode=require --username=${dbUserName} --password=${dbPassword} --logLevel=info update
if [ $? -ne 0 ]
then
    exit 1
else
    echo success!
fi
java -jar stagingprosecutorsspi-viewstore-liquibase.jar --url=jdbc:postgresql://${dbServerName}:${dbPort}/stagingprosecutorsspiviewstore?sslmode=require --username=${dbUserName} --password=${dbPassword} --logLevel=info update
if [ $? -ne 0 ]
then
    exit 1
else
    echo success!
fi
java -jar framework-system-liquibase.jar --url=jdbc:postgresql://${dbServerName}:${dbPort}/stagingprosecutorsspisystem?sslmode=require --username=${dbUserName} --password=${dbPassword} --logLevel=info update
if [ $? -ne 0 ]
then
    exit 1
else
    echo success!
fi
java -jar jobstore-liquibase.jar --url=jdbc:postgresql://${dbServerName}:${dbPort}/stagingprosecutorsspijobstore?sslmode=require --username=${dbUserName} --password=${dbPassword} --logLevel=info update
if [ $? -ne 0 ]
then
    exit 1
else
    echo success!
fi