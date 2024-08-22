#!/bin/bash

URL_TO_UPDATE_CENTER_JSON=https://updates.jenkins.io/current/update-center.actual.json
URL_TO_SCORES_JSON=https://reports.jenkins.io/plugin-health-scoring/scores.json

# Download update-center.actual.json
echo "Downloading update-center.actual.json..."
curl -s -o update-center.actual.json "$URL_TO_UPDATE_CENTER_JSON"
if [ $? -ne 0 ]; then
  echo "Failed to download update-center.actual.json"
  exit 1
fi
echo "Downloaded update-center.actual.json:"

# Download scores.json
echo "Downloading scores.json..."
curl -s -o scores.json "$URL_TO_SCORES_JSON"
if [ $? -ne 0 ]; then
  echo "Failed to download scores.json"
  exit 1
fi
echo "Downloaded scores.json:"

# Format the JSON files with jq
jq . update-center.actual.json > update-center.actual.formatted.json
jq . scores.json > scores.formatted.json

# Parse update-center.actual.formatted.json
echo "Parsing update-center.actual.formatted.json..."
plugins_with_versions=$(jq -r '.plugins | to_entries[] | .key' update-center.actual.formatted.json)
if [ $? -ne 0 ]; then
  echo "Failed to parse update-center.actual.formatted.json"
  exit 1
fi
echo "Number of plugins found in update-center.actual.formatted.json: $(echo "$plugins_with_versions" | wc -l)"
echo "Plugins with versions (10 first lines):"
echo "$plugins_with_versions" | head -n 10

# Create two text files: one for plugin names and versions, another for plugin names and scores
echo "Creating text files for plugin names and versions, and plugin names and scores..."
printf "%s\n" "$plugins_with_versions" | parallel --no-notice --keep-order -j "$(nproc)" --colsep ':' '
  plugin_name="{1}"
  escaped_plugin_name="${plugin_name//\"/\\\"}"
  version=$(jq -r ".plugins.\"$escaped_plugin_name\".version" update-center.actual.formatted.json)
  score=$(jq -r ".plugins.\"$escaped_plugin_name\"?.value" scores.formatted.json)
  echo "$plugin_name:$version" >> plugins_with_versions.txt
  echo "$plugin_name:$score" >> plugins_with_scores.txt
'

# Debug: Print the contents of plugins_with_versions.txt
echo "Contents of plugins_with_versions.txt (10 first lines):"
head -n 10 plugins_with_versions.txt

# Sort both files by plugin names
echo "Sorting text files by plugin names..."
sort -t ':' -k1,1 plugins_with_versions.txt > sorted_plugins_with_versions.txt
sort -t ':' -k1,1 plugins_with_scores.txt > sorted_plugins_with_scores.txt

# Combine the sorted files and sort by score
echo "Combining sorted files and sorting by score..."
paste -d ':' sorted_plugins_with_versions.txt sorted_plugins_with_scores.txt | sort -t ':' -k4 -nr | cut -d ':' -f 1,2 > plugins.txt

# Print the contents of plugins.txt
echo "Contents of plugins.txt:"
cat plugins.txt
