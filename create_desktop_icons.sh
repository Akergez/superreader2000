#!/bin/bash
set -e

cd /Users/a1234/StudioProjects/app

# Create resources directory
mkdir -p desktopApp/src/jvmMain/resources
cd desktopApp/src/jvmMain/resources

# Use existing ic_launcher-playstore.png (512x512) from Android app
ICON_SRC="/Users/a1234/StudioProjects/app/superreader2000/src/main/ic_launcher-playstore.png"

if [ ! -s "$ICON_SRC" ]; then
    echo "Error: No valid source icon found at $ICON_SRC"
    echo "Please provide a 512x512 or larger PNG icon at that location"
    exit 1
fi

# Copy as icon.png for Linux
cp "$ICON_SRC" icon.png

# Get dimensions
WIDTH=$(sips -g pixelWidth icon.png | tail -1 | awk '{print $2}')
echo "Source icon width: $WIDTH"

# Create iconset for macOS
mkdir -p icon.iconset

# Generate all required sizes (will upscale if source is smaller)
sips -z 16 16 icon.png --out icon.iconset/icon_16x16.png
sips -z 32 32 icon.png --out icon.iconset/icon_16x16@2x.png
sips -z 32 32 icon.png --out icon.iconset/icon_32x32.png
sips -z 64 64 icon.png --out icon.iconset/icon_32x32@2x.png
sips -z 128 128 icon.png --out icon.iconset/icon_128x128.png
sips -z 256 256 icon.png --out icon.iconset/icon_128x128@2x.png
sips -z 256 256 icon.png --out icon.iconset/icon_256x256.png
sips -z 512 512 icon.png --out icon.iconset/icon_256x256@2x.png
sips -z 512 512 icon.png --out icon.iconset/icon_512x512.png
sips -z 1024 1024 icon.png --out icon.iconset/icon_512x512@2x.png

# Convert to icns
iconutil -c icns icon.iconset -o icon.icns

# Clean up iconset folder
rm -rf icon.iconset

echo "Created icons:"
ls -la icon.png icon.icns

echo "Done! Icons created successfully."

