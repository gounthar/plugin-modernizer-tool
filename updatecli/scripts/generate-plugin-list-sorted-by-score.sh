#!/bin/bash

# Source required scripts
source log-utils.sh
source check-env.sh

set -e # Exit immediately if a command exits with a non-zero status.

# Create a temporary directory
tempDir=$(mktemp -d)

# Ensure the temporary directory is removed on script exit
trap 'rm -rf -- "$tempDir"' EXIT

# Validate the sort order parameter
if [ "$#" -ne 1 ]; then
    error "Usage: $0 <asc|desc>"
    exit 1
fi

sort_order=$1
if [ "$sort_order" != "asc" ] && [ "$sort_order" != "desc" ]; then
    error "Invalid sort order: $sort_order. Use 'asc' or 'desc'."
    exit 1
fi

# Define the URLs for the JSON files
URL_TO_UPDATE_CENTER_JSON=https://updates.jenkins.io/current/update-center.actual.json
URL_TO_SCORES_JSON=https://reports.jenkins.io/plugin-health-scoring/scores.json
updateCenterJSON="$tempDir/update-center.json"
scoresJSON="$tempDir/scores.json"

# Download update-center.actual.json
debug "Downloading update-center.actual.json..."

# Check for wget or curl and download the file
if command -v wget &> /dev/null; then
  wget -q "$URL_TO_UPDATE_CENTER_JSON" -O "$updateCenterJSON"
elif command -v curl &> /dev/null; then
  curl -s -o "$updateCenterJSON" "$URL_TO_UPDATE_CENTER_JSON"
else
    error "Neither wget nor curl could be found. Please install one of them to continue."
    exit 1
fi
if [ $? -ne 0 ]; then
  error "Failed to download update-center.actual.json"
  exit 1
fi
debug "Downloaded update-center.actual.json:"

# Download scores.json
debug "Downloading scores.json..."
if command -v wget &> /dev/null; then
  wget -q "$URL_TO_SCORES_JSON" -O "$scoresJSON"
elif command -v curl &> /dev/null; then
  curl -s -o "$scoresJSON" "$URL_TO_SCORES_JSON"
else
    error "Neither wget nor curl could be found. Please install one of them to continue."
    exit 1
fi
if [ $? -ne 0 ]; then
  error "Failed to download scores.json"
  exit 1
fi
debug "Downloaded scores.json:"

# Format the JSON files with jq
jq . "$updateCenterJSON" > "$tempDir/update-center.actual.formatted.json"
jq . "$scoresJSON" > "$tempDir/scores.formatted.json"

# Parse update-center.actual.formatted.json to get plugin names and versions
debug "Parsing update-center.actual.formatted.json..."
jq -r '.plugins | to_entries[] | "\(.key):\(.value.version)"' "$tempDir/update-center.actual.formatted.json" > "$tempDir/plugins_with_versions.txt"
if [ $? -ne 0 ]; then
  error "Failed to parse update-center.actual.formatted.json"
  exit 1
fi
debug "Number of plugins found in update-center.actual.formatted.json: $(wc -l < $tempDir/plugins_with_versions.txt)"
debug "Plugins with versions (10 first lines):"
debug_head -n 10 "$tempDir/plugins_with_versions.txt"

# Parse scores.formatted.json to get plugin names and scores
debug "Parsing scores.formatted.json..."
jq -r '.plugins | to_entries[] | "\(.key):\(.value.value)"' "$tempDir/scores.formatted.json" > "$tempDir/plugins_with_scores.txt"
if [ $? -ne 0 ]; then
  error "Failed to parse scores.formatted.json"
  exit 1
fi
debug "Number of plugins found in scores.formatted.json: $(wc -l < $tempDir/plugins_with_scores.txt)"
debug "Plugins with scores (10 first lines):"
debug_head -n 10 "$tempDir/plugins_with_scores.txt"

# Sort plugins_with_scores.txt by score in the specified order
debug "Sorting plugins_with_scores.txt by score in $sort_order order..."
if [ "$sort_order" = "asc" ]; then
  sort -t ':' -k2 -n "$tempDir/plugins_with_scores.txt" > "$tempDir/sorted_plugins_with_scores.txt"
else
  sort -t ':' -k2 -nr "$tempDir/plugins_with_scores.txt" > "$tempDir/sorted_plugins_with_scores.txt"
fi

# Re-order plugins_with_versions.txt based on sorted_plugins_with_scores.txt
debug "Re-ordering plugins_with_versions.txt based on sorted_plugins_with_scores.txt..."
# Join the two files based on the plugin name (first field)
join -t ':' -a 1 -o 1.1,1.2,2.2 <(sort -t ':' -k1,1 "$tempDir/plugins_with_versions.txt") <(sort -t ':' -k1,1 "$tempDir/plugins_with_scores.txt") | awk -F':' '($2 == "" ? $2 = ":0" : 1) && ($3 == "" ? $3 = ":0" : 1)' | sort -t ':' -k3 -g > "$tempDir/plugins.txt"
tr ' ' ':' < "$tempDir/plugins.txt" > "$tempDir/plugins-no-space.txt"
tr -s ':' < "$tempDir/plugins-no-space.txt" | cut -d':' -f1,2 > "$tempDir/plugins.txt"

# Print the contents of plugins.txt
debug "Contents of plugins.txt:"
cat "$tempDir/plugins.txt"
