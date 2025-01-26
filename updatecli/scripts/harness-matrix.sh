#!/bin/bash

# Script: harness-matrix.sh
# Purpose: Generates a compatibility matrix between Jenkins Test Harness and Jenkins WAR versions
# Requirements: GitHub CLI (gh), jq

# Check for required commands
for cmd in gh jq; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
        echo "Error: $cmd is required but not installed." >&2
        exit 1
    fi
done

# Allow repository override via command line
REPO=${1:-"jenkinsci/jenkins-test-harness"}

# Check GitHub API rate limit
RATE_LIMIT=$(gh api rate_limit | jq '.rate.remaining')
if [ "$RATE_LIMIT" -lt 10 ]; then
    echo "Error: GitHub API rate limit is too low ($RATE_LIMIT remaining)" >&2
    exit 1
fi

# Fetch all releases using GitHub CLI with error handling
echo "Fetching releases for $REPO..."
if ! RELEASES=$(gh api -X GET "repos/$REPO/releases" --paginate); then
    echo "Error: Failed to fetch releases from GitHub API" >&2
    exit 1
fi

# Check if any releases were found
if [ "$(echo "$RELEASES" | jq '. | length')" -eq 0 ]; then
    echo "Warning: No releases found for $REPO" >&2
    exit 0
fi

# Initialize an empty table
TABLE="| Jenkins Test Harness Version | Jenkins WAR Version |\n|-------------------------------|---------------------|"

# Process each release
declare -A VERSION_MAP  # For sorting versions later
echo "$RELEASES" | jq -c '.[]' | while read -r RELEASE; do
  # Extract release tag name (Jenkins Test Harness version)
  TAG_NAME=$(echo "$RELEASE" | jq -r '.tag_name' | tr -d '[:space:]')
  
  # Validate tag name format
  if ! [[ $TAG_NAME =~ ^[0-9]+\.[0-9]+(\.[0-9]+)?(-[0-9a-zA-Z.-]+)?$ ]]; then
    echo "Warning: Skipping invalid tag format: $TAG_NAME" >&2
    continue
  fi

  # Extract release body and search for the dependency message
  BODY=$(echo "$RELEASE" | jq -r '.body // empty')
  [[ -z "$BODY" ]] && continue

  if echo "$BODY" | grep -q "dependency org.jenkins-ci.main:jenkins-war"; then
    # Extract the Jenkins WAR version from the message
    if ! JENKINS_WAR_VERSION=$(echo "$BODY" | grep -oP "dependency org.jenkins-ci.main:jenkins-war to v\K[0-9]+\.[0-9]+(\.[0-9]+)?" || true); then
      echo "Warning: Could not extract WAR version from release $TAG_NAME" >&2
      continue
    fi

    # Validate WAR version format
    if ! [[ $JENKINS_WAR_VERSION =~ ^[0-9]+\.[0-9]+(\.[0-9]+)?$ ]]; then
      echo "Warning: Invalid WAR version format in release $TAG_NAME: $JENKINS_WAR_VERSION" >&2
      continue
    fi

    # Store in associative array for sorting
    VERSION_MAP["$TAG_NAME"]="$JENKINS_WAR_VERSION"
  fi
done

# Sort versions and build table
for TAG_NAME in $(echo "${!VERSION_MAP[@]}" | tr ' ' '\n' | sort -V); do
  TABLE+="\n| $TAG_NAME | ${VERSION_MAP[$TAG_NAME]} |"
done

# Output the table
echo -e "\nCompatibility Table:"
echo -e "$TABLE"
