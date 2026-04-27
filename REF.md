# for running backend 
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
mvn clean package -DskipTests
set -a && source .env && set +a
java -jar target/MPJ-1.0.jar


# for running frontend
voting_index.html
