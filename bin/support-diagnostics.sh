#!/usr/bin/env bash
#
# support-diagnostics.sh
#
#
# TODO:
# - Support for collecting marvel indices for export
# - more system data
# - Collect JVM stats
# - Add facility to automatically post to ZD or S3

timestamp=$(date +"%Y%m%d-%H%M%S")

# set defaults
outputdir="support-diagnostics.$timestamp.$(hostname)"
eshost="localhost:9200"

# pick up command line options
while [ $# -gt 0 ]
do
    case "$1" in	
	-h | --h | --help)  cat <<EOM
support-diagnostics.sh

This script is used to gather diagnotistic information for elasticsearch support. 
In order to gather the elasticsearch config and logs you must run this on a node within your elasticsearch cluster.

  -h  This help message
  -H  Elasticsearch hostname:port (defaults to $eshost)
  -n  On a host with multiple nodes, specify the node name to gather data for. Value should match node.name as defined in elasticsearch.yml
  -o  Script output directory (optional, defaults to ./support-diagnostics.[timestamp].[hostname])
  -nc Disable compression (optional)
EOM
	    exit 1;;
        -H)  eshost=$2;;
        -n)  nodename=$2;;
        -nc) nocompression=true;;
        -o)  outputdir=$2;;

    esac
    shift
done


#Ensure curl exists, or exit as there is nothing more we can do
command -v curl >/dev/null 2>&1 || { printf "ERROR: curl is not installed\n"; exit 1; }


# check dump file
mkdir $outputdir/
if [ ! -e $outputdir ]
then
    echo "Cannot write output file."
    exit 1
fi

# Cribbed from ES startup script.  Only works if we place this script in the elasticsearch/bin
CDPATH=""
SCRIPT="$0"

# SCRIPT may be an arbitrarily deep series of symlinks. Loop until we have the concrete path.
while [ -h "$SCRIPT" ] ; do
    ls=`ls -ld "$SCRIPT"`
    # Drop everything prior to ->
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
	SCRIPT="$link"
    else
	SCRIPT=`dirname "$SCRIPT"`/"$link"
    fi
done

echo "Getting your configuration from the elasticsearch API"

#ensure we can connect to the host, or exit as there is nothing more we can do
connectionTest=`curl -s -S -XGET $eshost 2>&1`
if [ $? -ne 0 ]
then
    echo "Error connecting to $eshost: $connectionTest"
    rmdir $outputdir
    exit 1
fi

#see if nodename was passed. if not just use the _local
if [ -z "$nodename" ]
then
    hostname=$(hostname)
    targetNode='_local'
else
    hostname=$nodename
    targetNode=$nodename

fi

#run a sanity check of the nodename by ensuring we get data back on that node
nodenameStatus=$(curl -s -S -XGET "$eshost/_nodes/$targetNode/settings?pretty" |grep -q '"nodes" : { }'; echo $?)
if [ $nodenameStatus -eq 0 ]
then
    printf "\n\nThe node/host name $hostname does not appear to be connected to your cluster. This script will continue, however without gathering the log files or elasticsearch.yml\n\n"


fi

#get the es version
esVersion=$(curl -s -S -XGET "$eshost/" |grep  '"number" : "'| sed -e 's/"number" : "//' -e 's/"//' )
configType='conf'
pathType='"path" : {'
logType='logs'
homeType='home'

#the api changes between 0.90 and 1.x
if [[ $esVersion =~ 0.90.* ]]; then
    echo "Detected a pre 1.x version of elasticsearch"
    configType='path.conf'
    pathType='"path.home" : "'
    logType='path.logs'
    homeType='path.home'
fi

#Get the desired node's settings and clean it up a bit
#if this is osx, then drop the -m1 from the grep as grep on osx does not work with -A.
if [ "$(uname)" == "Darwin" ]; then
    localNodePaths=$(curl -s -S -XGET "$eshost/_nodes/$targetNode/settings?pretty" |grep -A7  "$pathType" | sed 's/"//g')
else
    localNodePaths=$(curl -s -S -XGET "$eshost/_nodes/$targetNode/settings?pretty" |grep -m1 -A7 "$pathType" | sed 's/"//g')
fi

#Figure out of the above curl succeeded
if [ $(echo $localNodePaths |grep -q 'logs'; echo $?) -eq 0 ]  && [ $nodenameStatus -ne 0 ]
then

    #get the paths from previous the api call, clean it up, then get the desired paths
    localNodePaths=$(echo $localNodePaths |sed -e 's/"//' -e 's/path : {//' | tr ',' '\n' | sed 's/}//g')

    while read lineItem; do
	IFS=' : ' read -a currentItem <<< "$lineItem"
	case "${currentItem[0]}" in
	    "$logType")
		esLogsPath=${currentItem[1]}
		;;
	    "$homeType")
		esHomePath=${currentItem[1]}
		;;
	esac
    done <<<"$localNodePaths"

    #Grab yml location from the API
    localNodeConfig=$(curl -s -S -XGET "$eshost/_nodes/$targetNode/settings?pretty" | grep -m1 -ohE "\"$configType\" : \".*?\"")

    #Ensure the above curl succeeded 
    if [ $(echo $localNodeConfig |grep -q "\"$configType\" : \""; echo $?) -eq 0 ]
    then
	#get just the config including full path
	esConfigPath=$(echo $localNodeConfig | sed -e "s/\"$configType\" : \"//" -e 's/"//')
	
	echo "Getting config"
	cpStatus=-1

	#Check if the config directory we found exists
	if [ -d "$esConfigPath" ]
	then 
	    mkdir $outputdir/config
	    cp -r $esConfigPath/* $outputdir/config/

	    cpStatus=$?

	#Sometimes the api returns a relative path, if so prepend try prepending the home dir
	elif [ -d "$esHomePath/$esConfigPath" ]
	then
	    mkdir $outputdir/config
	    cp -r $esHomePath/$esConfigPath/* $outputdir/config/

	    cpStatus=$?
	fi

	#We couldn't find the config.
	if [ $cpStatus != 0 ]
	then
	    printf "\nERROR: Could not get your elasticsearch config. Please add your elasticsearch config directory to the $outputdir.tar or to your ticket\n\n"
	fi

	echo "Getting logs"
	mkdir $outputdir/logs
	cp $esLogsPath/*.log $outputdir/logs/

    else
	printf "\nERROR: Could not determine config directory location from api. Please add your config directory to the $outputdir.tar or to your ticket\n\n"
    fi

else
    printf "\nERROR: Could not determine elasticsearch paths location from api. Please add your elasticsearch config and log directories to the $outputdir.tar or to your ticket\n\n"
fi


#api calls that work with all versions
echo "Getting version"
curl -XGET "$eshost" >> $outputdir/version.json 2> /dev/null

echo "Getting _mapping"
curl -XGET "$eshost/_mapping?pretty" >> $outputdir/mapping.json 2> /dev/null

echo "Getting _settings"
curl -XGET "$eshost/_settings?pretty" >> $outputdir/settings.json 2> /dev/null

echo "Getting _cluster/settings"
curl -XGET "$eshost/_cluster/settings?pretty" >> $outputdir/cluster_settings.json 2> /dev/null

echo "Getting _cluster/state"
curl -XGET "$eshost/_cluster/state?pretty" >> $outputdir/cluster_state.json 2> /dev/null

echo "Getting _cluster/stats"
curl -XGET "$eshost/_cluster/stats?pretty&human" >> $outputdir/cluster_stats.json 2> /dev/null

echo "Getting nodes info"
curl -XGET "$eshost/_nodes/?all&pretty&human" >> $outputdir/nodes.json 2> /dev/null

echo "Getting _nodes/hot_threads"
curl -XGET "$eshost/_nodes/hot_threads?threads=10" >> $outputdir/nodes_hot_threads.txt 2> /dev/null


#api calls that only work with 0.90
if [[ $esVersion =~ 0.90.* ]]; then
    echo "Getting _nodes/stats"
    curl -XGET "$eshost/_nodes/stats?all&pretty&human" >> $outputdir/nodes_stats.json 2> /dev/null

    echo "Getting indices stats"
    curl -XGET "$eshost/_stats?all&pretty&human" >> $outputdir/indices_stats.json 2> /dev/null

else
    #api calls that only work with >1.0
    echo "Getting _nodes/stats"
    curl -XGET "$eshost/_nodes/stats?pretty&human" >> $outputdir/nodes_stats.json 2> /dev/null

    echo "Getting indices stats"
    curl -XGET "$eshost/_stats?pretty&human" >> $outputdir/indices_stats.json 2> /dev/null

    echo "Getting _cat/allocation"
    curl -XGET "$eshost/_cat/allocation?v" >> $outputdir/allocation.txt 2> /dev/null

    echo "Getting _cat/plugins"
    curl -XGET "$eshost/_cat/plugins?v" >> $outputdir/plugins.txt 2> /dev/null

    echo "Getting _/recovery"
    curl -XGET "$eshost/_cat/recovery?v" >> $outputdir/cat_recovery.json 2> /dev/null

fi


echo "Running netstat"
if [ "$(uname)" == "Darwin" ]; then
    netstat -an >> $outputdir/netstat.txt
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    netstat -anp >> $outputdir/netstat.txt
fi

echo "Running top"
if [ "$(uname)" == "Darwin" ]; then
    top -l 1 >> $outputdir/top.txt
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    top -b -n1 >> $outputdir/top.txt
fi

echo "Running top with threads (Linux only)"
if [ "$(uname)" == "Darwin" ]; then
    echo "This is a Mac.  Not running top -H."
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    top -b -n1 -H >> $outputdir/top_threads.txt
fi

echo "Running ps"
ps -ef | grep elasticsearch >> $outputdir/elasticsearch-process.txt


echo "Output complete.  Creating tarball."
tarfile=$outputdir.tar

command -v tar >/dev/null 2>&1 || { printf "tar is not installed, cannot package the gathered data. Please manually create an archive of $outputdir\n"; exit 1; }
tar cf $tarfile $outputdir/*
if [ ! $nocompression ]; then
    gzip $tarfile
    printf "\nDone. Created $outputdir.tar.gz\n\n"
else
    printf "\nDone. Created $outputdir.tar\n\n"

fi
rm -rf $outputdir


exit 0

