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
eshost="localhost:9200"
targetNode='_local'
repeat=1
interval=60

# pick up command line options
while [ $# -gt 0 ]
do
    case "$1" in
        -h | --h | --help)  cat <<EOM
support-diagnostics.sh

This script is used to gather diagnostics information for elasticsearch support.
In order to gather the elasticsearch config and logs you must run this on a node within your elasticsearch cluster.

  -h  This help message
  -H  Elasticsearch hostname:port (defaults to $eshost)
  -n  On a host with multiple nodes, specify the node name to gather data for. Value should match node.name as defined in elasticsearch.yml
  -o  Script output directory (optional, defaults to ./support-diagnostics.[timestamp].[hostname])
  -nc Disable compression (optional)
  -r  Collect stats r times (optional, in conjunction with -i , defaults to 1)
  -i  Interval in seconds between stats collections (optional, in conjunction with -r , defaults to 60 secs)
  -a  Authentication type. Either 'basic' or 'cookie' (optional)
  -c  Authentication credentials. Either a path to the auth cookie file or the basic auth usename. You will be prompted for the password unless you specify -p.
  -p  Password for authentication. To be used with -c if having this script prompt for a password is undesiarable.

EOM
            exit 1;;
        -H)  eshost=$2;;
        -n)  targetNode=$2;;
        -nc) nocompression=true;;
        -o)  outputdir=$2;;
        -r)  repeat=$2;;
        -i)  interval=$2;;
	-a)  authType=$2;;
	-c)  authCreds=$2;;
	-p)  password=$2;;

    esac
    shift
done

#Ensure curl exists, or exit as there is nothing more we can do
command -v curl >/dev/null 2>&1 || { printf "ERROR: curl is not installed\n"; exit 1; }

# if the output directory was not passed in, then set the default
if [ -z "$outputdir" ]
then
    outputdir="support-diagnostics.$(hostname).$targetNode.$timestamp"
fi

# check dump file
mkdir $outputdir/
if [ ! -e $outputdir ]
then
    printf "Cannot write output file\n\n"
    exit 1
fi

if [ $repeat -le 0 ]
then
	prinf "Repeat option (-r) must be a positive interger greater then 0\n\n"
	exit 1
fi

#Check if user wants auth support
curlCmd=''
if [ $authType ] 
then

    #Ensure valid auth type
    if [ $authType != "basic" ] &&  [ $authType != "cookie" ]
    then
	printf "Authentication type must be either cookie or basic\n\n"
	exit 1
    fi

    #ensure auth creds (user or cookie) are passed
    if [ -z $authCreds ]
    then
	printf "Authentication credentials must be used when -a is passed. See --help\n\n"
	exit 1
    fi

    #if using cookie, make sure cookie file exists
    if  [ $authType == "cookie" ]
    then

	if [ ! -r $authCreds ]
	then
	    printf "Authentication cookie '$authCreds' is not readable or does not exist\n\n"
	    exit 1
	fi

	#cookie based validation, set up cookie auth
	curlCmd="curl -s -S --cookie $authCreds"

    else
	#not using cookie, so setup basic auth
	

	#check if user provided password via flag, if not prompt. This also captures -p with no value
	if [ -z $password ] 
	then
	    printf "Enter authentication password (not displayed): "
	    read -s password
	    printf "\n"
	fi
	#using -k to work around in house/self sign certs
	curlCmd="curl -s -S -k --user $authCreds:$password"

	#test to make sure the auth is right, or exit as things will silently fail
	authStatus=$($curlCmd -XGET "$eshost/")
	authCheck=`echo $authStatus | grep '"status" : 200' > /dev/null; echo $?`

	if [ $authCheck -ne 0 ]
	then
	    printf "Authentication failed: \n$authStatus\n\n"
	    exit 1;
	fi
    fi



else
    #setup curl command without auth support
    curlCmd='curl -s -S'

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
connectionTest=`$curlCmd -s -S -XGET $eshost 2>&1`
if [ $? -ne 0 ]
then
    echo "Error connecting to $eshost: $connectionTest"
#   DON'T DELETE THE OUTPUT DIR!  IT MIGHT BE / or /opt!
#   rmdir $outputdir
    exit 1
fi

#run a sanity check of the nodename by ensuring we get data back on that node
nodenameStatus=$($curlCmd -s -S -XGET "$eshost/_nodes/$targetNode/settings?pretty" |grep -q '"nodes" : { }'; echo $?)
if [ $nodenameStatus -eq 0 ]
then
    printf "\n\nThe host and node name (\"$eshost\" and \"$targetNode\") does not appear to be connected to your cluster. This script will continue, however without gathering the log files or elasticsearch.yml\n\n"
fi

#get the es version
esVersion=$($curlCmd -s -S -XGET "$eshost/" |grep  '"number" : "'| sed -e 's/"number" : "//' -e 's/"//' )
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
    localNodePaths=$($curlCmd -s -S -XGET "$eshost/_nodes/$targetNode/settings?pretty" |grep -A7  "$pathType" | sed 's/"//g')
else
    localNodePaths=$($curlCmd -s -S -XGET "$eshost/_nodes/$targetNode/settings?pretty" |grep -m1 -A7 "$pathType" | sed 's/"//g')
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
    localNodeConfig=$($curlCmd -s -S -XGET "$eshost/_nodes/$targetNode/settings?pretty" | grep -m1 -ohE "\"$configType\" : \".*?\"")

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

        #Sometimes the api returns a relative path, if so try prepending the home dir
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

#grab settings
echo "Collecting version, mappings, settings"
echo "Getting version"
$curlCmd -XGET "$eshost" >> $outputdir/version.json 2> /dev/null

echo "Getting _mapping"
$curlCmd -XGET "$eshost/_mapping?pretty" >> $outputdir/mapping.json 2> /dev/null

echo "Getting _settings"
$curlCmd -XGET "$eshost/_settings?pretty" >> $outputdir/settings.json 2> /dev/null

echo "Getting _cluster/settings"
$curlCmd -XGET "$eshost/_cluster/settings?pretty" >> $outputdir/cluster_settings.json 2> /dev/null

echo "Getting _licenses"
$curlCmd -XGET "$eshost/_licenses?pretty" >> $outputdir/licenses.json 2> /dev/null

echo "Getting _segments"
$curlCmd -XGET "$eshost/_segments?pretty&human" >> $outputdir/segments.json 2> /dev/null

#grab stats
#execute multiple times if $repeat is > 1
i=1
while [ $i -le $repeat ]
    do
        echo "Collecting stats $i/$repeat"

        timestamp=`date  +%Y%m%d-%H%M%S`
        echo "Getting _cluster/state"
        $curlCmd -XGET "$eshost/_cluster/state?pretty" >> $outputdir/cluster_state.$timestamp.json 2> /dev/null

        echo "Getting _cluster/stats"
        $curlCmd -XGET "$eshost/_cluster/stats?pretty&human" >> $outputdir/cluster_stats.$timestamp.json 2> /dev/null

        echo "Getting _cluster/health"
        $curlCmd -XGET "$eshost/_cluster/health?pretty" >> $outputdir/cluster_health.$timestamp.json 2> /dev/null

        echo "Getting _cluster/pending_tasks"
        $curlCmd -XGET "$eshost/_cluster/pending_tasks?pretty&human" >> $outputdir/cluster_pending_tasks.$timestamp.json 2> /dev/null

        echo "Getting _count"
        $curlCmd -XGET "$eshost/_count?pretty" >> $outputdir/count.$timestamp.json 2> /dev/null

        echo "Getting nodes info"
        $curlCmd -XGET "$eshost/_nodes/?all&pretty&human" >> $outputdir/nodes.$timestamp.json 2> /dev/null

        echo "Getting _nodes/hot_threads"
        $curlCmd -XGET "$eshost/_nodes/hot_threads?threads=10000" >> $outputdir/nodes_hot_threads.$timestamp.txt 2> /dev/null


        #api calls that only work with 0.90
        if [[ $esVersion =~ 0.90.* ]]; then
            echo "Getting _nodes/stats"
            $curlCmd -XGET "$eshost/_nodes/stats?all&pretty&human" >> $outputdir/nodes_stats.$timestamp.json 2> /dev/null

            echo "Getting indices stats"
            $curlCmd -XGET "$eshost/_stats?all&pretty&human" >> $outputdir/indices_stats.$timestamp.json 2> /dev/null

        #api calls that only work with 1.0+
        else
            echo "Getting _nodes/stats"
            $curlCmd -XGET "$eshost/_nodes/stats?pretty&human" >> $outputdir/nodes_stats.$timestamp.json 2> /dev/null

            echo "Getting indices stats"
            $curlCmd -XGET "$eshost/_stats?pretty&human" >> $outputdir/indices_stats.$timestamp.json 2> /dev/null

            echo "Getting _cat/allocation"
            $curlCmd -XGET "$eshost/_cat/allocation?v" >> $outputdir/allocation.$timestamp.txt 2> /dev/null

            echo "Getting _cat/plugins"
            $curlCmd -XGET "$eshost/_cat/plugins?v" >> $outputdir/plugins.$timestamp.txt 2> /dev/null

            echo "Getting _cat/shards"
            $curlCmd -XGET "$eshost/_cat/shards?v" >> $outputdir/cat_shards.$timestamp.txt 2> /dev/null

            #api calls that only work with 1.1+
            if [[ ! $esVersion =~ 1.0.* ]]; then
                echo "Getting _recovery"
                $curlCmd -XGET "$eshost/_recovery?detailed&pretty&human" >> $outputdir/recovery.$timestamp.json 2> /dev/null
            #api calls that only work with 1.0
            else
                echo "Getting _cat/recovery"
                $curlCmd -XGET "$eshost/_cat/recovery?v" >> $outputdir/cat_recovery.$timestamp.txt 2> /dev/null
            fi
        fi


        echo "Running netstat"
        if [ "$(uname)" == "Darwin" ]; then
            netstat -an >> $outputdir/netstat.$timestamp.txt
        elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
            netstat -anp >> $outputdir/netstat.$timestamp.txt
        fi

        echo "Running top"
        if [ "$(uname)" == "Darwin" ]; then
            top -l 1 >> $outputdir/top.$timestamp.txt
        elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
            top -b -n1 >> $outputdir/top.$timestamp.txt
        fi

        echo "Running top with threads (Linux only)"
        if [ "$(uname)" == "Darwin" ]; then
            echo "This is a Mac.  Not running top -H."
        elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
            top -b -n1 -H >> $outputdir/top_threads.$timestamp.txt
        fi

        echo "Running ps"
        ps -ef | grep elasticsearch >> $outputdir/elasticsearch-process.$timestamp.txt

        if [ $i -lt $repeat ]
            then
                echo "Sleeping $interval second(s)..."
                sleep $interval
        fi
        i=$[i+1]
done


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
#rm -rf $outputdir


exit 0
