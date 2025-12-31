# Coco Viewer Application

### Purpose

This is a JavaFx application that is for viewing COCO datasets.
When the application is launched, a left pane that displays a label and a button will be shown.
This allows the user to select a file directory that is a COCO dataset.

COCO datasets have subdirectories that contain images and an annotation file that describes the image.
In the left pane, the user should be shown a tree view that displays the subdirectories.
When the user selects a subdirectory, a new pane will show all of the image entries in a new pane next to the left pane.

This pane should also have an input text at the top that allows the user to search for specific images.
If a user types in the text field the images will be filtered to only show images that match the search term.

When a user selects an image, a new pane will show the image and the annotation for that image.
The image will be shown with the bounding box and segmentation mask marked and the category for the bounding box 
and segmentation will be shown in the same color as the bounding box or mask.

### Agent Directions
- Do not edit the .gitignore file