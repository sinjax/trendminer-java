#!/bin/bash
for i in `cat $1`; do 
	export input=hdfs://seurat$i; 
	export outbase=`echo $i | cut -c 17-`; 
	export output=hdfs://seurat/trendminer/processed$outbase.tokens; 
	export csvoutput=hdfs://seurat/trendminer/processed$outbase.csv; 
	echo "INPUT: $input"
	echo "OUTPUT: $output" 
	hadoop jar target/HadoopTwitterTokenTool.jar -D mapred.child.java.opts="-Xmx2000M" -i $input -o $output -m DFIDF -om CSV -ro $csvoutput -pp "-m PORTER_STEM -om CONDENSED" -j analysis.stemmed ; 
done
