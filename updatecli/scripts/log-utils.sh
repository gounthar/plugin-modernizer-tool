#!/usr/bin/env bash

# utils.sh

# Function to print information messages in blue
info() {
  echo -e "\033[0;34m[INFO] $1\033[0m"
}

# Function to print warning messages in yellow
warning() {
  echo -e "\033[0;33m[WARNING] $1\033[0m"
}

# Function to print success messages in green
success() {
  echo -e "\033[0;32m[SUCCESS] $1\033[0m"
}

# Function to print error messages in red
error() {
  echo -e "\033[0;31m[ERROR] $1\033[0m" >&2
}

# Function to print debug messages in purple
# Only prints messages if the DEBUG_MODE environment variable is set to true
debug() {
  if [ "$DEBUG_MODE" = "true" ]; then
    echo -e "\033[0;35m[DEBUG] $1\033[0m"
  fi
}

# Replace cat with a loop that reads the file line by line
debug_cat() {
  local file=$1
  while IFS= read -r line; do
    debug "$line"
  done < "$file"
}

# Replace head with a loop that reads the first N lines of the file
debug_head() {
  local n=10  # Default value for n
  while getopts ":n:" opt; do
    case $opt in
      n) n=$OPTARG ;;
      \?) error "Invalid option: -$OPTARG" ;;
      :) error "Option -$OPTARG requires an argument." ;;
    esac
  done
  shift $((OPTIND -1))
  local file=$1
  local count=0
  while IFS= read -r line && [ $count -lt "$n" ]; do
    debug "$line"
    count=$((count + 1))
  done < "$file"
}

export -f info warning success error debug debug_cat debug_head
