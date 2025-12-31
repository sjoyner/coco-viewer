import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Polygon;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;

import java.io.*;
import java.util.*;
import java.util.List;

public class CocoAnnotationParser {
    
    private JsonObject cocoData;
    private Map<String, JsonObject> imageMap;
    private Map<String, List<Annotation>> annotationsByImage;
    private Map<Integer, Category> categoryMap;
    
    public CocoAnnotationParser(String annotationFilePath) throws IOException {
        loadAnnotations(annotationFilePath);
    }
    
    private void loadAnnotations(String annotationFilePath) throws IOException {
        Gson gson = new Gson();
        StringBuilder content = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(annotationFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }
        
        cocoData = gson.fromJson(content.toString(), JsonObject.class);
        
        // Create mappings for efficient lookup
        imageMap = new HashMap<>();
        JsonArray images = cocoData.getAsJsonArray("images");
        if (images != null) {
            for (JsonElement imageElement : images) {
                JsonObject imageObj = imageElement.getAsJsonObject();
                String id = imageObj.get("id").getAsString();
                imageMap.put(id, imageObj);
            }
        }
        
        // Create category mapping
        categoryMap = new HashMap<>();
        JsonArray categories = cocoData.getAsJsonArray("categories");
        if (categories != null) {
            for (JsonElement categoryElement : categories) {
                JsonObject categoryObj = categoryElement.getAsJsonObject();
                int id = categoryObj.get("id").getAsInt();
                categoryMap.put(id, new Category(
                    id,
                    categoryObj.get("name").getAsString(),
                    getColorForCategory(id)
                ));
            }
        }
        
        // Group annotations by image
        annotationsByImage = new HashMap<>();
        JsonArray annotations = cocoData.getAsJsonArray("annotations");
        if (annotations != null) {
            for (JsonElement annotationElement : annotations) {
                JsonObject annotationObj = annotationElement.getAsJsonObject();
                String imageId = annotationObj.get("image_id").getAsString();
                
                List<Annotation> annList = annotationsByImage.computeIfAbsent(imageId, k -> new ArrayList<>());
                
                // Extract bounding box
                JsonArray bboxArray = annotationObj.getAsJsonArray("bbox");
                double[] bbox = new double[4];
                if (bboxArray != null) {
                    for (int i = 0; i < 4; i++) {
                        bbox[i] = bboxArray.get(i).getAsDouble();
                    }
                }
                
                // Extract segmentation (if exists)
                JsonArray segmentationArray = annotationObj.getAsJsonArray("segmentation");
                double[][] segmentation = null;
                if (segmentationArray != null && segmentationArray.size() > 0) {
                    segmentation = new double[segmentationArray.size()][];
                    for (int i = 0; i < segmentationArray.size(); i++) {
                        JsonArray segPart = segmentationArray.get(i).getAsJsonArray();
                        segmentation[i] = new double[segPart.size()];
                        for (int j = 0; j < segPart.size(); j++) {
                            segmentation[i][j] = segPart.get(j).getAsDouble();
                        }
                    }
                }
                
                int categoryId = annotationObj.get("category_id").getAsInt();
                Category category = categoryMap.get(categoryId);
                
                annList.add(new Annotation(
                    annotationObj.get("id").getAsInt(),
                    imageId,
                    categoryId,
                    category,
                    bbox,
                    segmentation
                ));
            }
        }
    }
    
    public List<Annotation> getAnnotationsForImage(String imageId) {
        return annotationsByImage.getOrDefault(imageId, new ArrayList<>());
    }
    
    public JsonObject getImageInfo(String imageId) {
        return imageMap.get(imageId);
    }
    
    public List<Category> getAllCategories() {
        return new ArrayList<>(categoryMap.values());
    }
    
    private Color getColorForCategory(int categoryId) {
        // Generate a consistent color based on category ID
        Random random = new Random(categoryId * 7); // Use a seed for consistent color per category
        return Color.color(
            random.nextDouble(),
            random.nextDouble(),
            random.nextDouble()
        );
    }
    
    // Method to visualize annotations on an image
    public StackPane visualizeAnnotations(Image image, String imageId) {
        Group group = new Group();
        
        // Add the image
        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
        group.getChildren().add(imageView);
        
        // Add annotations (bounding boxes and segmentation)
        List<Annotation> annotations = getAnnotationsForImage(imageId);
        for (Annotation annotation : annotations) {
            // Draw bounding box
            if (annotation.bbox != null && annotation.bbox.length >= 4) {
                Rectangle bbox = new Rectangle();
                bbox.setX(annotation.bbox[0]);
                bbox.setY(annotation.bbox[1]);
                bbox.setWidth(annotation.bbox[2]);
                bbox.setHeight(annotation.bbox[3]);
                bbox.setStroke(annotation.category.color);
                bbox.setFill(null);
                bbox.setStrokeWidth(2);
                
                group.getChildren().add(bbox);
                
                // Add category label
                javafx.scene.text.Text label = new javafx.scene.text.Text();
                label.setX(annotation.bbox[0]);
                label.setY(annotation.bbox[1] - 5);
                label.setText(annotation.category.name);
                label.setFill(annotation.category.color);
                group.getChildren().add(label);
            }
            
            // Draw segmentation (if exists)
            if (annotation.segmentation != null) {
                for (double[] segPoints : annotation.segmentation) {
                    if (segPoints.length >= 6 && segPoints.length % 2 == 0) { // At least 3 points (6 coordinates)
                        Polygon polygon = new Polygon();
                        double[] points = new double[segPoints.length];
                        System.arraycopy(segPoints, 0, points, 0, segPoints.length);
                        // Add points individually since addAll expects Double values
                        for (double point : points) {
                            polygon.getPoints().add(point);
                        }
                        polygon.setFill(annotation.category.color);
                        polygon.setOpacity(0.3);
                        polygon.setStroke(annotation.category.color);
                        polygon.setStrokeWidth(1);
                        
                        group.getChildren().add(polygon);
                    }
                }
            }
        }
        
        StackPane stackPane = new StackPane();
        stackPane.getChildren().add(group);
        return stackPane;
    }
    
    
    // Inner classes for data structures
    public static class Category {
        public final int id;
        public final String name;
        public final Color color;
        
        public Category(int id, String name, Color color) {
            this.id = id;
            this.name = name;
            this.color = color;
        }
    }
    
    public static class Annotation {
        public final int id;
        public final String imageId;
        public final int categoryId;
        public final Category category;
        public final double[] bbox; // [x, y, width, height]
        public final double[][] segmentation; // Array of [x,y] point arrays
        
        public Annotation(int id, String imageId, int categoryId, Category category, 
                         double[] bbox, double[][] segmentation) {
            this.id = id;
            this.imageId = imageId;
            this.categoryId = categoryId;
            this.category = category;
            this.bbox = bbox;
            this.segmentation = segmentation;
        }
    }
}