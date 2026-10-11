package com.canteen.app.service;

import com.canteen.app.model.*;
import com.canteen.app.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final UpiPaymentService upiPaymentService;

    public DataInitializer(CategoryRepository categoryRepository,
                           MenuItemRepository menuItemRepository,
                           OrderRepository orderRepository,
                           UserRepository userRepository,
                           UpiPaymentService upiPaymentService) {
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.upiPaymentService = upiPaymentService;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return; // Data already seeded
        }

        // 1. Seed Users
        User admin = new User("Canteen Incharge", "admin@vnrvjiet.in", "9876543210", "VNR-STAFF-01", "Canteen Operations", "vnr@canteen", Role.CANTEEN_ADMIN);
        User student = new User("Rahul Sharma", "rahul.21071a0501@vnrvjiet.in", "9876543211", "21071A0501", "Computer Science (CSE)", "student123", Role.STUDENT);
        userRepository.saveAll(List.of(admin, student));

        // 2. Seed Categories
        Category breakfast = categoryRepository.save(new Category("Breakfast", "Freshly cooked South Indian & North Indian tiffins", 1, "bi-sunrise"));
        Category meals = categoryRepository.save(new Category("Meals & Rice", "Wholesome lunch bowls and biryanis", 2, "bi-egg-fried"));
        Category snacks = categoryRepository.save(new Category("Snacks & Chaat", "Quick college bites and evening cravings", 3, "bi-fire"));
        Category beverages = categoryRepository.save(new Category("Beverages", "Refreshing hot coffee, tea, and cool juices", 4, "bi-cup-straw"));

        // 3. Seed Menu Items
        MenuItem dosa = new MenuItem(breakfast, "Ghee Masala Dosa", "Crispy golden crepe with potato masala and fresh coconut chutney",
                new BigDecimal("60.00"), "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?w=600&auto=format&fit=crop&q=80", true, true, 8);

        MenuItem idli = new MenuItem(breakfast, "Steamed Idli Sambar (3 Pcs)", "Piping hot fluffy rice cakes with spicy drumstick sambar",
                new BigDecimal("45.00"), "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=600&auto=format&fit=crop&q=80", true, true, 5);

        MenuItem poori = new MenuItem(breakfast, "Poori Bhaji (3 Pcs)", "Puffed whole wheat bread with spicy potato curry",
                new BigDecimal("55.00"), "https://images.unsplash.com/photo-1601050690597-df0568f70950?w=600&auto=format&fit=crop&q=80", true, true, 10);

        MenuItem vegBiryani = new MenuItem(meals, "Hyderabadi Veg Dum Biryani", "Fragrant basmati rice layered with paneer, vegetables & aromatic spices",
                new BigDecimal("120.00"), "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=600&auto=format&fit=crop&q=80", true, true, 12);

        MenuItem mealsThali = new MenuItem(meals, "Executive South Indian Thali", "Rice, Sambar, Rasam, Curd, 2 Veg Curries, Papad & Sweet",
                new BigDecimal("100.00"), "https://images.unsplash.com/photo-1610057099431-d73a1c9d2f2f?w=600&auto=format&fit=crop&q=80", true, true, 7);

        MenuItem samosa = new MenuItem(snacks, "Samosa (2 Pcs) with Chutney", "Crispy spiced potato pastries served with mint and tamarind dips",
                new BigDecimal("30.00"), "https://images.unsplash.com/photo-1601050690597-df0568f70950?w=600&auto=format&fit=crop&q=80", true, true, 4);

        MenuItem pavBhaji = new MenuItem(snacks, "Butter Pav Bhaji", "Rich mashed vegetable curry topped with melted butter and toasted buns",
                new BigDecimal("80.00"), "https://images.unsplash.com/photo-1606491956689-2ea866880c84?w=600&auto=format&fit=crop&q=80", true, true, 10);

        MenuItem coldCoffee = new MenuItem(beverages, "Thick Cold Coffee", "Chilled whipped coffee blend with chocolate drizzle",
                new BigDecimal("50.00"), "https://images.unsplash.com/photo-1517256064527-09c73fc73e38?w=600&auto=format&fit=crop&q=80", true, true, 4);

        MenuItem filterCoffee = new MenuItem(beverages, "Authentic Filter Coffee", "Freshly brewed South Indian filter coffee in traditional tumbler",
                new BigDecimal("25.00"), "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600&auto=format&fit=crop&q=80", true, true, 3);

        MenuItem masalaChai = new MenuItem(beverages, "Special Masala Chai", "Kadak spiced tea infused with ginger, cardamom, and clove",
                new BigDecimal("20.00"), "https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=600&auto=format&fit=crop&q=80", true, true, 3);

        menuItemRepository.saveAll(List.of(dosa, idli, poori, vegBiryani, mealsThali, samosa, pavBhaji, coldCoffee, filterCoffee, masalaChai));

        // 4. Create Initial Sample Orders for Demonstration
        Order sampleOrder1 = new Order();
        sampleOrder1.setOrderNumber("ORD-20261011-101");
        sampleOrder1.setTokenNumber(101);
        sampleOrder1.setCustomerName("Ananya Rao");
        sampleOrder1.setStudentRollNo("21071A0512");
        sampleOrder1.setPhone("9876501001");
        sampleOrder1.setOrderStatus(OrderStatus.ORDERED);
        sampleOrder1.setPaymentStatus(PaymentStatus.PAID);
        sampleOrder1.setTotalAmount(new BigDecimal("105.00"));
        sampleOrder1.setOrderTime(LocalDateTime.now().minusMinutes(6));
        sampleOrder1.addItem(new OrderItem(dosa, 1));
        sampleOrder1.addItem(new OrderItem(idli, 1));
        Order saved1 = orderRepository.save(sampleOrder1);
        upiPaymentService.createPaymentForOrder(saved1);

        Order sampleOrder2 = new Order();
        sampleOrder2.setOrderNumber("ORD-20261011-102");
        sampleOrder2.setTokenNumber(102);
        sampleOrder2.setCustomerName("Karthik Verma");
        sampleOrder2.setStudentRollNo("22071A0440");
        sampleOrder2.setPhone("9876502002");
        sampleOrder2.setOrderStatus(OrderStatus.PREPARING);
        sampleOrder2.setPaymentStatus(PaymentStatus.PAID);
        sampleOrder2.setTotalAmount(new BigDecimal("145.00"));
        sampleOrder2.setOrderTime(LocalDateTime.now().minusMinutes(14));
        sampleOrder2.setPreparationStartTime(LocalDateTime.now().minusMinutes(10));
        sampleOrder2.addItem(new OrderItem(vegBiryani, 1));
        sampleOrder2.addItem(new OrderItem(filterCoffee, 1));
        Order saved2 = orderRepository.save(sampleOrder2);
        upiPaymentService.createPaymentForOrder(saved2);

        Order sampleOrder3 = new Order();
        sampleOrder3.setOrderNumber("ORD-20261011-103");
        sampleOrder3.setTokenNumber(103);
        sampleOrder3.setCustomerName("Pooja Nair");
        sampleOrder3.setStudentRollNo("23071A0589");
        sampleOrder3.setPhone("9876503003");
        sampleOrder3.setOrderStatus(OrderStatus.READY_TO_COLLECT);
        sampleOrder3.setPaymentStatus(PaymentStatus.PAID);
        sampleOrder3.setTotalAmount(new BigDecimal("80.00"));
        sampleOrder3.setOrderTime(LocalDateTime.now().minusMinutes(22));
        sampleOrder3.setPreparationStartTime(LocalDateTime.now().minusMinutes(18));
        sampleOrder3.setReadyTime(LocalDateTime.now().minusMinutes(2));
        sampleOrder3.addItem(new OrderItem(pavBhaji, 1));
        Order saved3 = orderRepository.save(sampleOrder3);
        upiPaymentService.createPaymentForOrder(saved3);
    }
}
