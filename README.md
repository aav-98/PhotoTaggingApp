# Photo Tagging System

## Overview

This README provides detailed documentation for the "Photo Tagging System" developed by myself and a fellow student from the University of Oslo for our Spring 2024 course project. This Android application, written in Kotlin, enables users to manage their photo collections with advanced tagging and editing features. It supports both online and offline operations, ensuring functionality across various connectivity scenarios.

## Table of Contents

- [Project Description](#project-description)
- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Architecture](#architecture)
- [Known Issues](#known-issues)
- [Contributors](#contributors)
- [License](#license)

## Project Description

The Photo Tagging System allows users to submit new photos, edit existing ones, tag photos with metadata, and view them on a map based on geolocation data. The project was designed with a focus on both usability and technical robustness, offering a seamless experience for managing a digital photo collection.

## Features

- **User Authentication**: Supports login, logout, and password modification.
- **Photo Management**: Users can submit new photos, replace existing photos, and delete photos.
- **Editing Tools**: Edit photos before submission with tools like cropping and applying filters.
- **Offline Capability**: The app functions offline, caching changes and synchronizing with the server when online.
- **Geotagging**: Displays a map showing where photos were taken, extracting geolocation data from photo metadata.

## Installation

1. Clone the repository from GitHub.
2. Open the project in Android Studio or your preferred IDE.
3. Build the project to resolve dependencies.

## Usage

1. **Starting the Server**: Ensure the Eclipse server is running before launching the app to handle backend operations.
2. **Launching the App**: Open the app on your Android device.
3. **Login/Signup**: Authenticate to access your photos.
4. **Adding Photos**: Use the '+' button to add new photos to your collection.
5. **Editing Photos**: Select a photo and use the edit tools to modify it.
6. **Viewing Photos and Map**: When viewing a photo, the associated tags and a map showing the photo's location are displayed in the same fragment.
7. **Offline Use**: The app will automatically cache your actions and sync once the connection to the Eclipse server is restored.

## Architecture

### Data Management
- **Login Data**: Managed through `LoginFormState`, `LoggedInUser`, and `LoginResult`.
- **Photo Data**: Handled using `LiveData`, allowing for real-time updates across the app.

### Technical Components
- **Volley Library**: Used for network calls.
- **Android Image Cropper & GPUImage**: Libraries used for photo editing functionalities.

### Offline Operation
- Utilizes `SharedPreferences` for local data storage, ensuring functionality during network outages.

## Known Issues

- There is a minor bug in the GPU-image library related to OpenGL ES handling, which does not impact functionality but appears in logcat errors.

## Contributors

- Andreas Askim Vatne - `andreaav@uio.no`
- Caroline Santos Alvær - `carolsal@uio.no`

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE) file for details.

