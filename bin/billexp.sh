#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";
pushd . >> /dev/null
cd $DIR/..
# pwd
MLHOME=machine-learning/machine-learning
MLJAR=$MLHOME/target/machine-learning-1.0.0-SNAPSHOT.jar

if [ ! -f $MLJAR ]
then
	mvn install -DskipTests
fi

if [ ! -f $MLHOME/cp.txt ]
then
	mvn -pl $MLHOME dependency:build-classpath -Dmdep.outputFile=cp.txt
fi

EXPCP=`cat $MLHOME/cp.txt`:$MLJAR
EXPCLASS=org.openimaj.ml.linear.experiments.BillAustrianExperiments
java -cp $EXPCP $EXPCLASS
popd >> /dev/null