# Auntie

An Android app that downloads TikTok videos. Share a TikTok link to this app and it will fetch and download the video to your device.

## Features

- Receive shared TikTok links via Android share intent
- Automatically resolve shortened TikTok URLs
- Download videos using the system DownloadManager
- Videos saved to Downloads folder

## Setup

1. Create a `secrets.properties` file in the project root:
```properties
RAPIDAPI_KEY=your_rapidapi_key_here
RAPIDAPI_HOST=your_rapidapi_host_here
```

2. Build and install the app

## Usage

1. Open TikTok
2. Find a video you want to download
3. Tap Share → Select "Auntie"
4. The video will download automatically

## Requirements

- Android 5.0 (API 21) or higher
- RapidAPI account with TikTok API access
