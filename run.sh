#!/usr/bin/env bash
set -e

# Automatically detect Java 21 LTS
if [ -z "$JAVA_HOME" ]; then
  if [ -d "/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home" ]; then
    export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home"
  elif [ -x "/usr/libexec/java_home" ]; then
    export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home 2>/dev/null || echo "")
  fi
fi

if [ -n "$JAVA_HOME" ]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "==============================================================="
echo " Building Fitbit 3D Application..."
echo " Using Java: $(which java || echo 'java not found')"
java -version
echo "==============================================================="

mkdir -p bin data

# Compile Java sources
javac -d bin $(find src -name "*.java")
echo "[✔] Compilation successful!"

PORT="${1:-8080}"

echo "[✔] Starting Fitbit 3D on port $PORT..."
echo "[✔] Web Interface: http://localhost:$PORT"
echo "==============================================================="

# Optional: Auto-open browser on macOS
if command -v open >/dev/null 2>&1; then
  (sleep 1 && open "http://localhost:$PORT") &
fi

# Run application
exec java -cp bin fitbit.Main "$PORT"
