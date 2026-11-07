package com.shopkart.config;

import com.shopkart.model.Product;
import com.shopkart.model.User;
import com.shopkart.repository.ProductRepository;
import com.shopkart.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class DataSeeder {

    private static final int DEFAULT_STOCK = 100;

    /** Rotating discount percentages to make the catalog look like a real sale. */
    private static final int[] DISCOUNTS = {10, 15, 20, 25, 30, 35, 40, 45, 50, 55};

    /** Relevant, stable image URL for a product (keyword-tagged, locked per name). */
    private static String imageFor(String name, String keyword) {
        int lock = Math.abs(name.hashCode()) % 100000;
        return "https://loremflickr.com/400/300/" + keyword + "?lock=" + lock;
    }

    /** Given a selling price and a discount %, derive a believable original MRP. */
    private static BigDecimal mrpFor(BigDecimal price, int discountPct) {
        // price = mrp * (1 - d/100)  =>  mrp = price / (1 - d/100)
        BigDecimal factor = BigDecimal.valueOf(100 - discountPct)
                .divide(BigDecimal.valueOf(100));
        return price.divide(factor, 0, RoundingMode.HALF_UP); // whole-rupee MRP
    }

    /**
     * Catalog across 6 categories — ~16 distinct models each.
     * Each item is {name, price, imageKeyword}.
     */
    private static Map<String, Object[][]> catalog() {
        Map<String, Object[][]> catalog = new LinkedHashMap<>();
        catalog.put("Electronics", new Object[][]{
                {"Wireless Headphones", 2499, "headphones"}, {"Smart Watch", 3999, "smartwatch"},
                {"Bluetooth Speaker", 1899, "speaker"}, {"Laptop 14-inch", 54999, "laptop"},
                {"Smartphone 5G", 18999, "smartphone"}, {"Android Tablet", 15999, "tablet"},
                {"Mechanical Keyboard", 3499, "keyboard"}, {"Gaming Mouse", 1299, "computermouse"},
                {"27-inch Monitor", 12999, "monitor"}, {"Power Bank 20000mAh", 1599, "powerbank"},
                {"Wireless Earbuds", 1999, "earbuds"}, {"DSLR Camera", 48999, "camera"},
                {"Action Camera", 8999, "actioncamera"}, {"Wi-Fi Router", 2299, "router"},
                {"External SSD 1TB", 7999, "harddrive"}, {"Smart LED TV 42-inch", 32999, "television"}});
        catalog.put("Fashion", new Object[][]{
                {"Running Shoes", 1799, "runningshoes"}, {"Laptop Backpack", 1299, "backpack"},
                {"Denim Jacket", 2199, "denimjacket"}, {"Sunglasses", 999, "sunglasses"},
                {"Analog Wrist Watch", 2499, "wristwatch"}, {"Leather Wallet", 799, "wallet"},
                {"Cotton T-Shirt", 499, "tshirt"}, {"Formal Shirt", 1099, "shirt"},
                {"Casual Sneakers", 1999, "sneakers"}, {"Baseball Cap", 399, "cap"},
                {"Hooded Sweatshirt", 1499, "hoodie"}, {"Chino Trousers", 1299, "trousers"},
                {"Leather Belt", 699, "belt"}, {"Ankle Socks Pack", 299, "socks"},
                {"Silk Scarf", 599, "scarf"}, {"Formal Shoes", 2299, "leathershoes"}});
        catalog.put("Home", new Object[][]{
                {"Coffee Maker", 2999, "coffeemaker"}, {"LED Desk Lamp", 899, "desklamp"},
                {"Vacuum Cleaner", 6999, "vacuumcleaner"}, {"Air Purifier", 8999, "airpurifier"},
                {"Non-stick Cookware Set", 2499, "cookware"}, {"Cotton Bed Sheet", 1299, "bedsheet"},
                {"Wall Clock", 699, "wallclock"}, {"Table Fan", 1499, "fan"},
                {"Steel Water Bottle", 499, "waterbottle"}, {"Storage Organizer Box", 599, "storagebox"},
                {"Microwave Oven", 8499, "microwave"}, {"Electric Kettle", 1199, "kettle"},
                {"Dinner Plate Set", 1599, "plates"}, {"Blackout Curtains", 1399, "curtains"},
                {"Wooden Photo Frame", 449, "photoframe"}, {"Scented Candle Set", 699, "candle"}});
        catalog.put("Sports", new Object[][]{
                {"Yoga Mat", 699, "yogamat"}, {"Dumbbell Set 10kg", 1999, "dumbbell"},
                {"Cricket Bat", 2499, "cricketbat"}, {"Football", 899, "football"},
                {"Badminton Racket", 1199, "badminton"}, {"Cycling Helmet", 1499, "helmet"},
                {"Skipping Rope", 299, "jumprope"}, {"Tennis Ball Pack", 399, "tennisball"},
                {"Gym Gloves", 599, "gymgloves"}, {"Foldable Treadmill", 24999, "treadmill"},
                {"Basketball", 1099, "basketball"}, {"Resistance Bands Set", 799, "resistanceband"},
                {"Camping Tent", 4999, "tent"}, {"Trekking Backpack", 2999, "hikingbackpack"},
                {"Sports Water Bottle", 349, "sportsbottle"}, {"Table Tennis Set", 1299, "tabletennis"}});
        catalog.put("Books", new Object[][]{
                {"Bestseller Novel", 399, "book"}, {"Cookbook", 599, "cookbook"},
                {"Ruled Notebook", 149, "notebook"}, {"Comic Book", 299, "comics"},
                {"English Dictionary", 499, "dictionary"}, {"Science Textbook", 699, "textbook"},
                {"Children's Story Book", 249, "childrensbook"}, {"Self-Help Guide", 449, "book"},
                {"Mystery Thriller", 399, "novel"}, {"Poetry Collection", 299, "poetry"},
                {"Business Strategy Book", 599, "book"}, {"Travel Guide", 499, "travelbook"}});
        catalog.put("Beauty", new Object[][]{
                {"Face Wash", 249, "facewash"}, {"Perfume 100ml", 1499, "perfume"},
                {"Hair Dryer", 1299, "hairdryer"}, {"Matte Lipstick", 399, "lipstick"},
                {"Sunscreen SPF50", 349, "sunscreen"}, {"Moisturizing Cream", 499, "facecream"},
                {"Shampoo 650ml", 399, "shampoo"}, {"Beard Trimmer", 1799, "trimmer"},
                {"Nail Polish Set", 299, "nailpolish"}, {"Facial Serum", 899, "serum"},
                {"Makeup Brush Set", 799, "makeupbrush"}, {"Kajal Eyeliner", 199, "eyeliner"}});
        return catalog;
    }

    private static List<Product> buildCatalog() {
        List<Product> products = new ArrayList<>();
        int idx = 0;
        for (Map.Entry<String, Object[][]> entry : catalog().entrySet()) {
            String category = entry.getKey();
            for (Object[] item : entry.getValue()) {
                String name = (String) item[0];
                BigDecimal price = new BigDecimal((int) item[1]);
                String keyword = (String) item[2];
                int discount = DISCOUNTS[idx++ % DISCOUNTS.length];
                products.add(Product.builder()
                        .name(name)
                        .description(name + " - premium quality " + category.toLowerCase() + " product")
                        .price(price)
                        .mrp(mrpFor(price, discount))
                        .category(category)
                        .imageUrl(imageFor(name, keyword))
                        .stock(DEFAULT_STOCK)
                        .build());
            }
        }
        return products;
    }

    @Bean
    public CommandLineRunner seed(ProductRepository productRepository,
                                  UserRepository userRepository,
                                  PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                userRepository.save(User.builder()
                        .name("Admin")
                        .email("admin@shopkart.com")
                        // Seed password comes from env; falls back to a dev-only default.
                        .password(passwordEncoder.encode(
                                System.getenv().getOrDefault("ADMIN_SEED_PASSWORD", "Admin@12345")))
                        .role(User.Role.ADMIN)
                        .build());
            }

            List<Product> full = buildCatalog();

            // Insert only catalog products that don't already exist (match by name),
            // so re-runs add new models without duplicating existing ones.
            Set<String> existingNames = productRepository.findAll().stream()
                    .map(Product::getName)
                    .collect(Collectors.toSet());
            List<Product> toAdd = full.stream()
                    .filter(p -> !existingNames.contains(p.getName()))
                    .toList();
            if (!toAdd.isEmpty()) {
                productRepository.saveAll(toAdd);
            }

            // Ensure every product has a discount MRP and is fully stocked (stock = 100).
            List<Product> all = productRepository.findAll();
            for (Product p : all) {
                if (p.getMrp() == null || p.getMrp().compareTo(p.getPrice()) <= 0) {
                    p.setMrp(mrpFor(p.getPrice(), DISCOUNTS[Math.abs(p.getName().hashCode()) % DISCOUNTS.length]));
                }
                p.setStock(DEFAULT_STOCK);
            }
            productRepository.saveAll(all);
        };
    }
}
