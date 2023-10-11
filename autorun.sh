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
        java --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/sun.net.www.protocol.https=ALL-UNNAMED -jar hamley.jar debug
        procid=$!
    else
        "$JAVA19_HOME"/java --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/sun.net.www.protocol.https=ALL-UNNAMED -jar hamley.jar debug
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