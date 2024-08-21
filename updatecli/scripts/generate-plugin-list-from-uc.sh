#!/usr/bin/env bash
# This script updates the Jenkins plugin information from a specified update center.
# It checks for required dependencies (`jq`, `wget`, or `curl`), downloads the plugin data,
# and removes specified fields from the JSON file using `jq`.

set -e # Exit immediately if a command exits with a non-zero status.

# Create a temporary directory
tempDir=$(mktemp -d)

# Ensure the temporary directory is removed on script exit
trap 'rm -rf -- "$tempDir"' EXIT

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo "jq could not be found. Please install jq to continue."
    exit 1
fi

# Default update center URL
defaultUpdateCenterURL="https://updates.jenkins.io/current/update-center.actual.json"

# Use the first argument as the updateCenterURL if provided, otherwise use the default
updateCenterURL="${1:-$defaultUpdateCenterURL}"

# Use the temporary directory for intermediate files
updateCenterJSON="$tempDir/update-center.json"
resultJSON="$tempDir/result.json"
dependenciesByPluginJSON="$tempDir/dependencies-by-plugin.json"

# Check for wget or curl and download the file
if command -v wget &> /dev/null; then
        wget -q "$updateCenterURL" -O "$updateCenterJSON"
elif command -v curl &> /dev/null; then
        curl -s -o "$updateCenterJSON" "$updateCenterURL"
else
    echo "Neither wget nor curl could be found. Please install one of them to continue."
    exit 1
fi
# Extract the plugin name and version from the JSON file
jq -r '.plugins[] | [.gav, .name] | @tsv' update-center.actual.json |
awk -F '\t' '{
    split($1, parts, ":");
    plugin_name = $2;
    plugin_version = parts[3];
    print plugin_name ":" plugin_version;
}' > plugins.txt

# Clean up the downloaded file
# rm update-center.actual.json

# Prendre exemple sur https://github.com/gounthar/jenkins.io/blob/donors/updatecli/scripts/retrieve-donors.sh pour le transformer en source updatecli
# Il suffirait de remplacer la dernière redirection par la sortie standard.
# Ne pas oublier d'effacer les fichiers temporaires.
