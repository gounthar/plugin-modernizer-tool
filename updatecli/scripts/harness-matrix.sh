#!/bin/bash

# Set the repository
REPO="jenkinsci/jenkins-test-harness"

# Fetch all releases using GitHub CLI
echo "Fetching releases for $REPO..."
RELEASES=$(gh api -X GET "repos/$REPO/releases" --paginate)

# Initialize an empty table
TABLE="| Jenkins Test Harness Version | Jenkins WAR Version |\n|-------------------------------|---------------------|"

# Process each release
echo "$RELEASES" | jq -c '.[]' | while read -r RELEASE; do
  # Extract release tag name (Jenkins Test Harness version)
  TAG_NAME=$(echo "$RELEASE" | jq -r '.tag_name')

  # Extract release body and search for the dependency message
  BODY=$(echo "$RELEASE" | jq -r '.body')
  if echo "$BODY" | grep -q "dependency org.jenkins-ci.main:jenkins-war"; then
    # Extract the Jenkins WAR version from the message
    JENKINS_WAR_VERSION=$(echo "$BODY" | grep -oP "dependency org.jenkins-ci.main:jenkins-war to v\K[0-9\.]+")

    # Add the row to the table
    TABLE+="\n| $TAG_NAME | $JENKINS_WAR_VERSION |"
  fi
done

# Output the table
echo -e "\nCompatibility Table:"
echo -e "$TABLE"
