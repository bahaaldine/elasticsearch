#!/bin/bash

# Variables
ES_VERSION="9.0.0-SNAPSHOT"
TARBALL_DIR="distribution/archives/darwin-tar/build/distributions"
TARBALL="elasticsearch-${ES_VERSION}-darwin-x86_64.tar.gz"
ES_DIR="elasticsearch-${ES_VERSION}"
NOTEBOOK_NAME="./x-pack/plugin/plesql/src/test/script/retro-arcade-data.ipynb"

# Function to kill any running Elasticsearch processes
kill_elasticsearch() {
  echo "Stopping any running Elasticsearch processes..."
  pkill -f "elasticsearch"
}

# Function to clean up the extracted directory
cleanup() {
  echo "Cleaning up extracted Elasticsearch directory..."
  if [ -d "${ES_DIR}" ]; then
    rm -rf "${ES_DIR}"
    echo "Removed ${ES_DIR}."
  else
    echo "No extracted directory found to clean up."
  fi
}

# Step 1: Install Python dependencies
install_dependencies() {
  echo "Checking and installing Python dependencies..."

  # Install pip if not already installed
  if ! command -v pip &> /dev/null; then
    echo "pip not found, installing pip..."
    curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py
    python3 get-pip.py --user
    rm get-pip.py
  fi

  # Install required packages if not already installed
  for package in jupyter elasticsearch pandas papermill; do
    if ! python3 -c "import ${package}" &> /dev/null; then
      echo "Installing ${package}..."
      pip install --user ${package}
    else
      echo "${package} already installed."
    fi
  done
}

# Step 2: Build Elasticsearch tarball using Gradle
echo "Building Elasticsearch tarball..."
./gradlew -p distribution/archives/darwin-tar assemble

if [ $? -ne 0 ]; then
  echo "Error: Failed to assemble the Elasticsearch tarball."
  exit 1
fi

# Kill any existing Elasticsearch processes
kill_elasticsearch

# Step 3: Check if the tarball exists
if [ ! -f "${TARBALL_DIR}/${TARBALL}" ]; then
  echo "Error: Elasticsearch tarball not found in ${TARBALL_DIR}."
  exit 1
fi

# Step 4: Extract the tarball
echo "Extracting Elasticsearch tarball..."
tar -xzf "${TARBALL_DIR}/${TARBALL}"

# Check if extraction was successful
if [ ! -d "${ES_DIR}" ]; then
  echo "Error: Failed to extract Elasticsearch."
  exit 1
fi

# Step 5: Disable SSL and security in the elasticsearch.yml file
echo "Disabling SSL and security in elasticsearch.yml and enabling debug logging..."
ES_CONFIG_FILE="./${ES_DIR}/config/elasticsearch.yml"
LOG_CONFIG_FILE="./${ES_DIR}/config/log4j2.properties"

if [ -f "${ES_CONFIG_FILE}" ]; then
  echo "xpack.security.transport.ssl.enabled: false" >> "${ES_CONFIG_FILE}"
  echo "xpack.security.http.ssl.enabled: false" >> "${ES_CONFIG_FILE}"
  echo "xpack.security.enabled: false" >> "${ES_CONFIG_FILE}"
  echo "xpack.ml.enabled: false" >> "${ES_CONFIG_FILE}"
else
  echo "Error: elasticsearch.yml file not found!"
  cleanup
  exit 1
fi

# Step 6: Enable DEBUG logging in log4j2.properties
if [ -f "${LOG_CONFIG_FILE}" ]; then
  echo "Setting Elasticsearch log level to DEBUG..."
  sed -i '' 's/^rootLogger.level = .*/rootLogger.level = info/' "${LOG_CONFIG_FILE}"
else
  echo "Error: log4j2.properties file not found!"
  cleanup
  exit 1
fi

# Step 7: Start Elasticsearch
echo "Starting Elasticsearch..."
# Enable remote debugging on port 5005 (no suspend)
export ES_JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
./${ES_DIR}/bin/elasticsearch -d

# Step 8: Wait for Elasticsearch to start
echo "Waiting for Elasticsearch to start..."
sleep 10  # You can adjust this if Elasticsearch needs more time to start

# Step 9: Check if Elasticsearch is running
if curl -s "http://localhost:9200" | grep -q "You Know, for Search"; then
  echo "Elasticsearch is running at http://localhost:9200"
else
  echo "Error: Elasticsearch did not start successfully."
  cleanup
  exit 1
fi

# Step 10: Run the Python notebook to index data into Elasticsearch using papermill
echo "Running Python notebook to index data into Elasticsearch using papermill..."
install_dependencies  # Install dependencies before running the notebook
papermill "${NOTEBOOK_NAME}" output_notebook.ipynb

if [ $? -ne 0 ]; then
  echo "Error: Failed to execute Python notebook with papermill."
  cleanup
  exit 1
fi

# Step 11: Tail the logs and keep Elasticsearch running
LOG_FILE="${ES_DIR}/logs/elasticsearch.log"
echo "Tailing Elasticsearch logs..."
tail -f "${LOG_FILE}"

# After exiting the script manually, clean up (Optional)
trap cleanup EXIT
