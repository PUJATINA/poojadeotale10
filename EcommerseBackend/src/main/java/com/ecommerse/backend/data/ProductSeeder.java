package com.ecommerse.backend.data;

import com.ecommerse.backend.entity.Product;
import com.ecommerse.backend.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ProductSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;

    public ProductSeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        byte[] defaultImageBytes = loadDefaultImageBytes();
        String defaultContentType = "image/svg+xml";

        String[] brands = {"Nova", "Aster", "UrbanMint", "Velora", "Zentro"};
        Map<String, Integer> categoryTargets = Map.of(
                "Ladies Wear", 100,
                "Gents Wear", 100,
                "Kids Wear", 100,
                "Mobile Phone", 50,
                "Laptop", 50,
                "Furniture", 50,
                "Carpet", 50
        );

        for (Map.Entry<String, Integer> entry : categoryTargets.entrySet()) {
            String category = entry.getKey();
            int targetCount = entry.getValue();
            long existingInCategory = productRepository.countByCategoryIgnoreCase(category);

            if (existingInCategory >= targetCount) {
                continue;
            }

            int toCreate = (int) (targetCount - existingInCategory);
            List<Product> products = buildProducts(category, toCreate, (int) existingInCategory + 1, brands, defaultImageBytes, defaultContentType);
            productRepository.saveAll(products);
        }

        syncExistingProductImagesInDb(defaultImageBytes, defaultContentType);
    }

    private void syncExistingProductImagesInDb(byte[] defaultImageBytes, String defaultContentType) {
        List<Product> allProducts = productRepository.findAll();
        boolean changed = false;

        for (Product product : allProducts) {
            if (product.getImageData() == null || product.getImageData().length == 0) {
                product.setImageData(defaultImageBytes);
                product.setImageContentType(defaultContentType);
                product.setImageUrl("");
                changed = true;
            }
        }

        if (changed) {
            productRepository.saveAll(allProducts);
        }
    }

    private List<Product> buildProducts(
            String category,
            int count,
            int startIndex,
            String[] brands,
            byte[] defaultImageBytes,
            String defaultContentType) {
        List<Product> products = new ArrayList<>();

        String[] ladiesTypes = {"Kurti", "Saree", "Top", "Jeans", "Dress"};
        String[] gentsTypes = {"Shirt", "T-Shirt", "Jeans", "Trouser", "Jacket"};
        String[] kidsTypes = {"Frock", "T-Shirt", "Shorts", "Hoodie", "Set"};
        String[] mobileTypes = {"Smartphone", "5G Phone", "Budget Phone", "Flagship Phone", "Camera Phone"};
        String[] laptopTypes = {"Ultrabook", "Gaming Laptop", "Business Laptop", "Student Laptop", "Creator Laptop"};
        String[] furnitureTypes = {"Sofa", "Dining Table", "Bed", "Wardrobe", "Coffee Table"};
        String[] carpetTypes = {"Wool Carpet", "Cotton Carpet", "Runner Carpet", "Designer Carpet", "Soft Carpet"};

        for (int i = 0; i < count; i++) {
            int serial = startIndex + i;
            String brand = brands[i % brands.length];
            String productType = pickType(category, i, ladiesTypes, gentsTypes, kidsTypes, mobileTypes, laptopTypes, furnitureTypes, carpetTypes);

            String name = brand + " " + productType + " " + serial;
            String description = "Quality " + category.toLowerCase() + " item from " + brand + ". Product no. " + serial + ".";

            BigDecimal price = BigDecimal.valueOf(basePrice(category) + ((serial * 41) % 1200));
            int stock = 40 + ((serial * 13) % 180);

            products.add(create(name, description, price, stock, category, brand, defaultImageBytes, defaultContentType));
        }

        return products;
    }

    private String pickType(
            String category,
            int index,
            String[] ladiesTypes,
            String[] gentsTypes,
            String[] kidsTypes,
            String[] mobileTypes,
            String[] laptopTypes,
            String[] furnitureTypes,
            String[] carpetTypes) {
        if ("Ladies Wear".equalsIgnoreCase(category)) {
            return ladiesTypes[index % ladiesTypes.length];
        }
        if ("Gents Wear".equalsIgnoreCase(category)) {
            return gentsTypes[index % gentsTypes.length];
        }
        if ("Kids Wear".equalsIgnoreCase(category)) {
            return kidsTypes[index % kidsTypes.length];
        }
        if ("Mobile Phone".equalsIgnoreCase(category)) {
            return mobileTypes[index % mobileTypes.length];
        }
        if ("Laptop".equalsIgnoreCase(category)) {
            return laptopTypes[index % laptopTypes.length];
        }
        if ("Furniture".equalsIgnoreCase(category)) {
            return furnitureTypes[index % furnitureTypes.length];
        }
        return carpetTypes[index % carpetTypes.length];
    }

    private long basePrice(String category) {
        if ("Mobile Phone".equalsIgnoreCase(category)) {
            return 10000;
        }
        if ("Laptop".equalsIgnoreCase(category)) {
            return 35000;
        }
        if ("Furniture".equalsIgnoreCase(category)) {
            return 5000;
        }
        if ("Carpet".equalsIgnoreCase(category)) {
            return 2000;
        }
        return 700;
    }

    private Product create(
            String name,
            String description,
            BigDecimal price,
            int stock,
            String category,
            String brand,
            byte[] defaultImageBytes,
            String defaultContentType) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setImageUrl("");
        product.setImageData(defaultImageBytes);
        product.setImageContentType(defaultContentType);
        product.setPrice(price);
        product.setStock(stock);
        product.setCategory(category);
        product.setBrand(brand);
        return product;
    }

    private byte[] loadDefaultImageBytes() {
        try {
            ClassPathResource resource = new ClassPathResource("static/product-placeholder.svg");
            return resource.getInputStream().readAllBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to load default product image", exception);
        }
    }
}
