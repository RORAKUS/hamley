# BEFORE RUNNING SET THE $JAVA19_HOME AND $MVN3_HOME TO THE BIN FOLDERS OF THE PROGRAMS

procid=

build() {
  if [ -z "$MVN3_HOME" ]; then
        mvn clean package
    else
        "$MVN3_HOME"/mvn clean package
    fi
}
run() {
  if [ -z "$JAVA19_HOME" ]; then
        java -jar hamley.jar
        procid=$!
    else
        "$JAVA19_HOME"/java -jar hamley.jar
        procid=$!
    fi
}
checkState() {
    # Keep checking it's state
    while true; do
        ps -q $procid
        if [ $? -eq 1 ]; then
            run
        fi
    done
}

build
run
checkState