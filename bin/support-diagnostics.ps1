<#
.SYNOPSIS
    Collects diagnostic information from your elasticsearch cluster.
.DESCRIPTION
    This script is used to gather diagnotistic information for elasticsearch support.  In order to gather the elasticsearch config and logs you must run this on a node within your elasticsearch cluster.
.PARAMETER H
    Elasticsearch protocol://hostname:port (defaults to http://localhost:9200)
.PARAMETER n
    On a host with multiple nodes, specify the node name to gather data for. Value should match node.name as defined in elasticsearch.yml
.PARAMETER o
    Script output directory (optional, defaults to ./support-diagnostics.[timestamp].[hostname])
.PARAMETER nc
    Disable compression (optional)
.PARAMETER r
    Collect stats r times (optional, in conjunction with -i , defaults to 1)
.PARAMETER i
    Interval in seconds between stats collections (optional, in conjunction with -r , defaults to 60 secs)
.PARAMETER a
    Authentication type. Either 'basic' or 'cookie'. Cookie is not yet implemented. (optional)
.PARAMETER c
    Authentication credentials. Either a path to the auth cookie file or the basic auth usename. You will be prompted for the password unless you specify -p.
.PARAMETER p
    Password for authentication. To be used with -A if having this script prompt for a password is undesiarable.
#>

Param(
	[string]$H,
	[string]$n,
	[string]$o,
	[switch]$nc,
	[int]$r,
	[int]$i,
	[string]$A,
	[string]$c,
	[string]$p
)

# Set defaults
$esHostPort = 'http://localhost:9200'
$timestamp = Get-Date -format yyyyMMdd-HHmmss
$outputDir = $o
$targetNode = '_local'
$repeat = 1
$interval = 60

If ($H) {
    $esHostPort = $H
}

If ($n) {
    $targetNode = $n
}

# Cannot be defaulted without the target node being set
If (! $o) {
    $hostName = [System.Net.Dns]::GetHostName()
    $outputDir = 'support-diagnostics.'+$hostName+'.'+$targetNode+'.'+$timestamp
}

If ($r) {
	$repeat = $r
}

If ($i) {
	$interval = $i
}

$esHost = $esHostPort

#cookie isn't implemented yet, but putting the basis of it here to finish it
If ($a) {

	#ensure valid auth type
	If ($a -ne 'cookie' -and $a -ne 'basic') {
		Write-Host Authentication type must be either cookie or basic
		Exit
	}

	#ensure auth creds (user or cookie) are passed
	If (! $c) {
		Write-Host Authentication credentials must be used when -a is passed.
		Exit
	}


	#if using cookie, make sure cookie file exists
	If ($a -eq 'cookie') {

		Write-Host Sorry, cookie authentication has not been implemented yet. If you require this option please use the bash script.
		Exit

	}
	#not using cookie, so setup basic auth
	Else
	{
		#check if user provided password via flag, if not prompt.
		If ($p) {
			$password = $p
		}
		Else {
			$secPassword = Read-Host 'Enter authentication password' -AsSecureString
			#convert to a string
			$Ptr = [System.Runtime.InteropServices.Marshal]::SecureStringToCoTaskMemUnicode($secPassword)
			$password = [System.Runtime.InteropServices.Marshal]::PtrToStringUni($Ptr)
			[System.Runtime.InteropServices.Marshal]::ZeroFreeCoTaskMemUnicode($Ptr)

		}

		#ignore self sign certs
		add-type @"
using System.Net;
using System.Security.Cryptography.X509Certificates;
public class TrustAllCertsPolicy : ICertificatePolicy {
	public bool CheckValidationResult(
		ServicePoint srvPoint, X509Certificate certificate,
		WebRequest request, int certificateProblem) {$authHeader
							     return true;
							     }
}
"@

		#generate a cred string that can be used in the webrequest
		$base64AuthInfo = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes(("{0}:{1}" -f $c,$password)))
		$authHeader = @{Authorization=('Basic {0}' -f $base64AuthInfo)}

		[System.Net.ServicePointManager]::CertificatePolicy = New-Object TrustAllCertsPolicy

		#test connection
		$connectionTest = Invoke-WebRequest -Headers $authHeader -Uri $esHostPort
		If ($connectionTest.StatusCode -ne 200) {
			Write-Host Error connecting to $esHost
			Exit
		}

		Write-Host 'Getting your configuration from the elasticsearch API'

		$nodenameStatus = (Invoke-WebRequest -Headers $authHeader -Uri $esHost/_nodes/$targetNode/settings?pretty).RawContent | Select-String '"nodes" : { }'
		If ($nodenameStatus) {
			Write-Host `n`nThe host and node name (\"$esHostPort\" and \"$targetNode\") does not appear to be connected to your cluster.  This script will continue, however without gathering the log files or elasticsearch.yml`n`n
		}

		# Get the ES version
		$esVersion= (Invoke-RestMethod  -Headers $authHeader -Uri $esHost).version.number
		$nodes = (Invoke-RestMethod  -Headers $authHeader -Uri $esHost/_nodes/$targetNode/settings?pretty).nodes

}

}
Else {
	#not using authentication
	$connectionTest = Invoke-WebRequest -Uri $esHostPort
	If ($connectionTest.StatusCode -ne 200) {
		Write-Host Error connecting to $esHost
		Exit
	}

	Write-Host 'Getting your configuration from the elasticsearch API'

	$nodenameStatus = (Invoke-WebRequest -Uri $esHost/_nodes/$targetNode/settings?pretty).RawContent | Select-String '"nodes" : { }'
	If ($nodenameStatus) {
		Write-Host `n`nThe host and node name (\"$esHostPort\" and \"$targetNode\") does not appear to be connected to your cluster.  This script will continue, however without gathering the log files or elasticsearch.yml`n`n
	}

	# Get the ES version
	$esVersion= (Invoke-RestMethod $esHost).version.number
	$nodes = (Invoke-RestMethod -Uri $esHost/_nodes/$targetNode/settings?pretty).nodes

}



#function to preform requests
Function preformRequest ($uri, $dstFile)
{

	#if any sort of auth is used
	If ($a) {

		If ($a -eq 'cookie') {
			Invoke-WebRequest -Uri $uri -OutFile $dstFile
		}
		#not using cookie, so setup basic auth
		Else
		{
			Invoke-WebRequest -Headers $authHeader -Uri $uri -OutFile $dstFile
		}
	}

	#not using authentication, preform normal request
	Else {
		Invoke-WebRequest -Uri $uri -OutFile $dstFile
	}

}


New-Item $outputDir -Type directory | Out-Null
If (!(Test-Path $outputDir)) {
    Write-Host Cannot write output file.
    Exit
}



$nodesPropertyName = $nodes.psobject.properties.name
$nodeSettings = $nodes.$nodesPropertyName.settings

If ($esVersion.StartsWith("0.9")) {
    $esHomePath = $nodeSettings."path.home"
    $esLogsPath = $nodeSettings."path.logs"
} Else {
    $esHomePath = $nodeSettings.path.home
    $esLogsPath = $nodeSettings.path.logs
}

Write-Host 'Getting config'
$esConfigPath = $esHomePath+'\config'
# Check if the config directory we found exists
If (!(Test-Path $esConfigPath)) {
    # Sometimes the API returns a relative path.  If so, try prepending the home directory
    $esConfigPath = $esHomePath+'\'+$esConfigPath
    If (!(Test-Path $esConfigPath)) {
        Write-Host `nCould not get your elasticsearch config.  Please add your elasticsearch config directory to the $outputDir'.zip' or to your ticket.`n`n
        $esConfigPath = ''
    }
}

If ($esConfigPath) {
    cp -r $esConfigPath $outputDir\config
}

Write-Host 'Getting logs'
If (!(Test-Path $esLogsPath)) {
    $esLogsPath = $esHomePath+'\'+$esLogsPath
    If (!(Test-Path $esLogsPath)) {
        Write-Host `nCould not get your elasticsearch logs.  Please add your logs directory to the $outputDir+'.zip' or to your ticket.`n`n
        $esLogsPath = ''
    }
}

If ($esLogsPath) {
    mkdir $outputDir\logs | Out-Null
    cp $esLogsPath\*.log $outputDir\logs\
}

# API calls that work with all versions
Write-Host 'Getting version'
preformRequest "$esHost" "$outputDir/version.json"

Write-Host "Getting _mapping"
preformRequest "$esHost/_mapping?pretty" "$outputDir/mapping.json"

Write-Host 'Getting _settings'
preformRequest "$esHost/_settings?pretty" "$outputDir/settings.json"

Write-Host 'Getting _cluster/settings'
preformRequest "$esHost/_cluster/settings?pretty" "$outputDir/cluster_settings.json"

Write-Host 'Getting _alias'
preformRequest "$esHost/_alias?pretty" "$outputDir/alias.json"

Write-Host 'Getting _licenses'
preformRequest "$esHost/_licenses?pretty" "$outputDir/licenses.json"

Write-Host 'Getting _licenses'
preformRequest "$esHost/_segments?pretty&human" "$outputDir/segments.json"

#grab stats
#execute multiple times if $repeat is > 1
[int]$n=1

while($n -le $repeat) {
        $timestamp = Get-Date -format yyyyMMdd-HHmmss
        Write-Host "Collecting stats $n/$repeat"
        Write-Host 'Getting _cluster/state'

	preformRequest "$esHost/_cluster/state?pretty" "$outputDir/cluster_state.$timestamp.json"

        Write-Host 'Getting _cluster/stats'
       preformRequest "$esHost/_cluster/stats?pretty&human"  "$outputDir/cluster_stats.$timestamp.json"

        Write-Host 'Getting _cluster/health'
        preformRequest "$esHost/_cluster/health?pretty" "$outputDir/cluster_health.$timestamp.json"

        Write-Host 'Getting _cluster/pending_tasks'
        preformRequest "$esHost/_cluster/pending_tasks?pretty&human" "$outputDir/cluster_pending_tasks.$timestamp.json"

        Write-Host 'Getting _count'
        preformRequest "$esHost/_count?pretty" "$outputDir/count.$timestamp.json"

        Write-Host 'Getting nodes info'
        preformRequest "$esHost/_nodes/?all&pretty&human" "$outputDir/nodes.$timestamp.json"

        Write-Host 'Getting _nodes/hot_threads'
        preformRequest "$esHost/_nodes/hot_threads?threads=10000" "$outputDir/nodes_hot_threads.$timestamp.txt"

        # API calls that only work with 0.90
        If ($esVersion.StartsWith("0.9")) {
            Write-Host 'Getting _nodes/stats'
            preformRequest "$esHost/_nodes/stats?all&pretty&human" "$outputDir/nodes_stats.$timestamp.json"

            Write-Host 'Getting indices stats'
            preformRequest "$esHost/_stats?all&pretty&human" "$outputDir/indices_stats.$timestamp.json"
        # API calls that only work with 1.0+
        } Else {
            Write-Host 'Getting _nodes/stats'
            preformRequest "$esHost/_nodes/stats?pretty&human" "$outputDir/nodes_stats.$timestamp.json"

            Write-Host 'Getting indices stats'
            preformRequest "$esHost/_stats?pretty&human" "$outputDir/indices_stats.$timestamp.json"

            Write-Host 'Getting _cat/allocation'
            preformRequest "$esHost/_cat/allocation?v" "$outputDir/allocation.$timestamp.txt"

            Write-Host 'Getting _cat/plugins'
            preformRequest "$esHost/_cat/plugins?v" "$outputDir/plugins.$timestamp.txt"

            Write-Host 'Getting _cat/shards'
            preformRequest "$esHost/_cat/shards?v" "$outputDir/cat_shards.$timestamp.txt"

            # API calls that only work with 1.1+
            If (-Not $esVersion.StartsWith("1.0")) {
                Write-Host 'Getting _recovery'
                preformRequest "$esHost/_recovery?detailed&pretty&human" "$outputDir/recovery.$timestamp.json"
            # API calls that only work with 1.0
            } Else {
                Write-Host 'Getting _cat/recovery'
                preformRequest "$esHost/_cat/recovery?v" "$outputDir/cat_recovery.$timestamp.txt"
            }
        }

        Write-Host 'Running netstat'
        netstat -an | Out-File $outputDir/netstat.$timestamp.txt

        Write-Host 'Running top'
        ps | sort -desc cpu | select -first 30 | Out-File $outputDir/top.$timestamp.txt

        Write-Host 'Running ps'
        Get-Process -Name *elasticsearch* | Out-File $outputDir/elasticsearch-process.$timestamp.txt

    if ($n -lt $repeat) {
            Write-Output "Sleeping $interval second(s)..."
            Start-Sleep -s $interval
    }
    $n=$n+1
}


Write-Host 'Running fsutil'
fsutil fsinfo drives | Out-File $outputDir/fsinfo.txt

Write-Host 'Output complete.  Creating zip.'
Add-Type -Assembly System.IO.Compression.FileSystem
$compressionLevel = [System.IO.Compression.CompressionLevel]::Optimal
If ($nc) {
    $compressionLevel = [System.IO.Compression.CompressionLevel]::NoCompression
}
$zipFile = $outputDir+'.zip'
$source = $pwd.Path+'\'+$outputDir
$destination = $pwd.Path+'\'+$zipFile
If ($o) {
    $source = $outputDir
    $destination = $zipFile
}
[System.IO.Compression.ZipFile]::CreateFromDirectory($source, $destination, $compressionLevel, $false)

Write-Host `nDone.  Created $zipFile`n`n
