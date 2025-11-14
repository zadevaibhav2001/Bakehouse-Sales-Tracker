#!/bin/bash

echo "🌐 Starting The Bakehouse Frontend Server..."
echo ""
echo "Frontend will be available at: http://localhost:3000"
echo "Make sure your backend is running on: http://localhost:8080"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

# Check if Python 3 is available
if command -v python3 &> /dev/null; then
    python3 -m http.server 3000
elif command -v python &> /dev/null; then
    python -m http.server 3000
else
    echo "Error: Python is not installed"
    echo "Please install Python or use another web server"
    exit 1
fi
