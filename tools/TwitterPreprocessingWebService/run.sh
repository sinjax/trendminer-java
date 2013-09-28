#!/bin/zsh
java -Xmx2G -cp `find target/dependency | grep jar | tr "\n" : | head`target/TwitterPreprocessingWebService-1.0.0-SNAPSHOT.jar org.openimaj.webservice.twitter.TwitterPreprocessingWebService $@
