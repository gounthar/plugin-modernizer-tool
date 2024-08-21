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

# Combine plugin names with scores using GNU Parallel
echo "Combining plugin names and scores in parallel..."
combined_data=$(printf "%s\n" "$plugins_with_versions" | parallel --no-notice --keep-order -j "$(nproc)" --colsep ':' '
  score=$(jq -r ".plugins."{2}?.value scores.formatted.json)
  if [ -n "$score" ]; then
    echo "{1}:$score"
  else
    echo "No score found for plugin: {1}" >&2
  fi
' | grep -v '^$')

if [ -z "$combined_data" ]; then
  echo "No plugin names and scores were combined"
  exit 1
fi

# Write combined data to temp-plugins.txt
echo "Writing combined data to temp-plugins.txt..."
printf "%s\n" "$combined_data" > temp-plugins.txt
if [ $? -ne 0 ]; then
  echo "Failed to write combined data to temp-plugins.txt"
  exit 1
fi
echo "Contents of temp-plugins.txt:"
cat temp-plugins.txt


