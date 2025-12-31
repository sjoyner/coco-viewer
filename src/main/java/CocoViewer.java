import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Orientation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Polygon;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.concurrent.Task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.regex.Matcher;

public class CocoViewer extends Application {
    
    private BorderPane mainPane;
    private TreeView<String> directoryTree;
    private ListView<String> imageList;
    private ImageView imageView;
    private TextArea annotationArea;
    private TextField searchField;
    private File selectedDirectory;
    
    // For storing COCO dataset information
    private Map<String, List<ImageInfo>> imageInfos = new HashMap<>();
    private CocoAnnotationParser annotationParser;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("COCO Dataset Viewer");
        
        // Create main layout
        mainPane = new BorderPane();
        
        // Create left pane with directory selection
        VBox leftPane = createLeftPane();
        
        // Create center pane with image list
        VBox centerPane = createCenterPane();
        
        // Create right pane with image display and annotations
        VBox rightPane = createRightPane();
        
        // Set the panes in the main layout
        mainPane.setLeft(leftPane);
        mainPane.setCenter(centerPane);
        mainPane.setRight(rightPane);
        
        // Create scene and show stage
        Scene scene = new Scene(mainPane, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private VBox createLeftPane() {
        VBox leftPane = new VBox(10);
        leftPane.setPrefWidth(250);
        
        Label directoryLabel = new Label("COCO Dataset Directory:");
        
        Button selectDirectoryBtn = new Button("Select Directory");
        selectDirectoryBtn.setOnAction(e -> selectDirectory());
        
        directoryTree = new TreeView<>();
        directoryTree.setPrefHeight(600);
        directoryTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                handleDirectorySelection(newVal.getValue());
            }
        });
        
        leftPane.getChildren().addAll(directoryLabel, selectDirectoryBtn, directoryTree);
        
        return leftPane;
    }
    
    private VBox createCenterPane() {
        VBox centerPane = new VBox(10);
        centerPane.setPrefWidth(300);
        
        searchField = new TextField();
        searchField.setPromptText("Search images...");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterImages());
        
        imageList = new ListView<>();
        imageList.setPrefHeight(600);
        imageList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                handleImageSelection(newVal);
            }
        });
        
        centerPane.getChildren().addAll(searchField, imageList);
        
        return centerPane;
    }
    
    private VBox createRightPane() {
        VBox rightPane = new VBox(10);
        rightPane.setPrefWidth(600);
        
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(550);
        
        annotationArea = new TextArea();
        annotationArea.setPrefHeight(200);
        annotationArea.setEditable(false);
        
        ScrollPane scrollPane = new ScrollPane(imageView);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        rightPane.getChildren().addAll(scrollPane, annotationArea);
        
        return rightPane;
    }
    
    private void selectDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select COCO Dataset Directory");
        
        if (selectedDirectory != null) {
            directoryChooser.setInitialDirectory(selectedDirectory);
        }
        
        File dir = directoryChooser.showDialog(null);
        if (dir != null) {
            selectedDirectory = dir;
            buildDirectoryTree(dir);
            loadCocoAnnotations(dir);
        }
    }
    
    private void buildDirectoryTree(File rootDir) {
        TreeItem<String> rootItem = new TreeItem<>(rootDir.getName());
        rootItem.setExpanded(true);
        
        // Add subdirectories to the tree
        File[] subdirs = rootDir.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File subdir : subdirs) {
                TreeItem<String> subdirItem = new TreeItem<>(subdir.getName());
                // Add images from this subdirectory to our map
                loadImagesFromDirectory(subdir);
                rootItem.getChildren().add(subdirItem);
            }
        }
        
        directoryTree.setRoot(rootItem);
    }
    
    private void loadCocoAnnotations(File rootDir) {
        // Look for COCO annotation files in the root directory
        File[] annotationFiles = rootDir.listFiles(f -> 
            f.getName().toLowerCase().endsWith(".json") && 
            (f.getName().toLowerCase().contains("instances") || 
             f.getName().toLowerCase().contains("captions") || 
             f.getName().toLowerCase().contains("person_keypoints"))
        );
        
        if (annotationFiles != null && annotationFiles.length > 0) {
            try {
                // Use the first annotation file found
                annotationParser = new CocoAnnotationParser(annotationFiles[0].getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error loading COCO annotations: " + e.getMessage());
            }
        }
    }
    
    private void loadImagesFromDirectory(File directory) {
        String dirName = directory.getName();
        List<ImageInfo> images = new ArrayList<>();
        
        File[] imageFiles = directory.listFiles(f -> {
            String name = f.getName().toLowerCase();
            return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".bmp");
        });
        
        if (imageFiles != null) {
            for (File imageFile : imageFiles) {
                // Try to find the corresponding image ID in the annotations
                String imageName = imageFile.getName();
                String imageId = findImageIdByName(imageName);
                images.add(new ImageInfo(imageFile, imageId));
            }
        }
        
        imageInfos.put(dirName, images);
    }
    
    private String findImageIdByName(String imageName) {
        if (annotationParser != null) {
            // In a complete implementation, we would match the image name to the annotations
            // For now, we'll just return a placeholder
            List<CocoAnnotationParser.Category> categories = annotationParser.getAllCategories();
            if (!categories.isEmpty()) {
                // This is a simplified approach - in a real implementation, 
                // we would need to match the image name to the image ID in the annotations
                return imageName; // Placeholder
            }
        }
        return imageName; // Placeholder
    }
    
    private void handleDirectorySelection(String directoryName) {
        List<String> imageNames = new ArrayList<>();
        List<ImageInfo> images = imageInfos.get(directoryName);
        
        if (images != null) {
            for (ImageInfo imageInfo : images) {
                imageNames.add(imageInfo.file.getName());
            }
        }
        
        ObservableList<String> items = FXCollections.observableArrayList(imageNames);
        imageList.setItems(items);
    }
    
    private void filterImages() {
        String searchTerm = searchField.getText().toLowerCase();
        if (searchTerm == null || searchTerm.isEmpty()) {
            // If no search term, show all images in current directory
            TreeItem<String> selectedItem = directoryTree.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                handleDirectorySelection(selectedItem.getValue());
            }
            return;
        }
        
        // Get current directory's images
        TreeItem<String> selectedItem = directoryTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            String directoryName = selectedItem.getValue();
            List<ImageInfo> allImages = imageInfos.get(directoryName);
            
            if (allImages != null) {
                List<String> filteredImages = new ArrayList<>();
                for (ImageInfo img : allImages) {
                    if (img.file.getName().toLowerCase().contains(searchTerm)) {
                        filteredImages.add(img.file.getName());
                    }
                }
                
                ObservableList<String> items = FXCollections.observableArrayList(filteredImages);
                imageList.setItems(items);
            }
        }
    }
    
    private void handleImageSelection(String imageName) {
        TreeItem<String> selectedDirItem = directoryTree.getSelectionModel().getSelectedItem();
        if (selectedDirItem == null) return;
        
        String directoryName = selectedDirItem.getValue();
        List<ImageInfo> images = imageInfos.get(directoryName);
        
        if (images != null) {
            for (ImageInfo img : images) {
                if (img.file.getName().equals(imageName)) {
                    displayImage(img);
                    break;
                }
            }
        }
    }
    
    private void displayImage(ImageInfo imageInfo) {
        try {
            Image image = new Image(imageInfo.file.toURI().toString());
            
            if (annotationParser != null) {
                // Visualize the image with annotations if available
                StackPane annotatedImage = annotationParser.visualizeAnnotations(image, imageInfo.imageId);
                
                // For now, just display the original image in the ImageView
                // In a complete implementation, we'd display the annotated image
                imageView.setImage(image);
                
                // Show annotation information
                List<CocoAnnotationParser.Annotation> annotations = annotationParser.getAnnotationsForImage(imageInfo.imageId);
                StringBuilder annotationText = new StringBuilder();
                annotationText.append("Image: ").append(imageInfo.file.getName()).append("\n");
                annotationText.append("Path: ").append(imageInfo.file.getAbsolutePath()).append("\n");
                annotationText.append("Size: ").append((int)image.getWidth()).append(" x ").append((int)image.getHeight()).append("\n\n");
                annotationText.append("Annotations: ").append(annotations.size()).append("\n");
                
                for (CocoAnnotationParser.Annotation ann : annotations) {
                    annotationText.append("- Category: ").append(ann.category.name).append("\n");
                    annotationText.append("  BBox: [").append(String.format("%.2f", ann.bbox[0])).append(", ")
                                 .append(String.format("%.2f", ann.bbox[1])).append(", ")
                                 .append(String.format("%.2f", ann.bbox[2])).append(", ")
                                 .append(String.format("%.2f", ann.bbox[3])).append("]\n");
                    
                    if (ann.segmentation != null) {
                        annotationText.append("  Segmentation: ").append(ann.segmentation.length).append(" parts\n");
                    }
                }
                
                annotationArea.setText(annotationText.toString());
            } else {
                // No annotations available
                imageView.setImage(image);
                annotationArea.setText("Image: " + imageInfo.file.getName() + 
                                      "\nPath: " + imageInfo.file.getAbsolutePath() +
                                      "\nSize: " + (int)image.getWidth() + " x " + (int)image.getHeight() +
                                      "\n\nNo annotation file found in the dataset directory.");
            }
        } catch (Exception e) {
            annotationArea.setText("Error loading image: " + e.getMessage());
        }
    }
    
    // Inner class to hold image information
    private static class ImageInfo {
        File file;
        String imageId;
        
        ImageInfo(File file, String imageId) {
            this.file = file;
            this.imageId = imageId;
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}