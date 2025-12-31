# COCO Dataset Viewer

A JavaFX application for viewing COCO datasets with annotations. This application allows users to browse COCO datasets, view images with bounding boxes and segmentation masks, and search through images.

## Features

- Browse COCO dataset directories with a tree view
- Display images with bounding boxes and segmentation masks overlaid
- Search functionality to filter images by name
- View annotation details for each image
- Support for COCO annotation formats (instances, captions, person_keypoints)

## Requirements

- Java 17 or higher
- Maven 3.6.0 or higher

## Build Instructions

1. Clone the repository
2. Navigate to the project directory
3. Run the following command to compile the application:

```bash
mvn compile
```

4. To package the application:

```bash
mvn package
```

## Running the Application

To run the application directly with Maven:

```bash
mvn javafx:run
```

Or run the compiled JAR file:

```bash
java -jar target/coco-viewer-0.1.jar
```

## Usage

1. Launch the application
2. Click "Select Directory" button to choose a COCO dataset directory
3. The left pane will display subdirectories of the dataset
4. Select a subdirectory to view its images in the center pane
5. Use the search field to filter images by name
6. Select an image to view it with annotations in the right pane
7. Annotations will be displayed with bounding boxes and segmentation masks in different colors

## Project Structure

- `org.sj.CocoViewer.java` - Main JavaFX application class
- `org.sj.CocoAnnotationParser.java` - Handles parsing and visualization of COCO annotations
- `pom.xml` - Maven configuration file with dependencies

## COCO Dataset Format

This application expects a standard COCO dataset format with:
- Subdirectories containing images
- JSON annotation files (instances, captions, or person_keypoints)
- Images in common formats (JPG, PNG, BMP)

## Technical Details

The application uses:
- JavaFX for the user interface
- Gson for JSON parsing
- Custom visualization for bounding boxes and segmentation masks
- Color coding for different object categories
