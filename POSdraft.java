import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

class MenuItem {
    String name, category;
    double price;

    public MenuItem(String name, String category, double price) {
        this.name = name;
        this.category = category;
        this.price = price;
    }
    @Override
    public String toString() { return name + " - ₱" + price; }
}

public class POSdraft extends JFrame {
    private ArrayList<MenuItem> menu = new ArrayList<>();
    private ArrayList<MenuItem> cart = new ArrayList<>();
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private boolean isTakeOut = false;

    public POSdraft() {
        // Sample only
        menu.add(new MenuItem("Siomai Rice", "Meals", 150.0));
        menu.add(new MenuItem("Sinigang", "Meals", 180.0));
        menu.add(new MenuItem("Iced Latte", "Drinks", 120.0));
        menu.add(new MenuItem("Mango Shake", "Drinks", 90.0));
        menu.add(new MenuItem("Leche Flan", "Desserts", 75.0));
        menu.add(new MenuItem("Halo-Halo", "Desserts", 110.0));
        menu.add(new MenuItem("Ice Cream", "Desserts", 50.0));

        setTitle("Restaurant and Ordering System");
        setSize(500, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Different screens
        mainPanel.add(createWelcomeScreen(), "Welcome");
        mainPanel.add(createCategoryScreen(), "Categories");

        add(mainPanel);
        cardLayout.show(mainPanel, "Welcome");
    }

    //Main menu
    private JPanel createWelcomeScreen() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        JLabel label = new JLabel("Welcome to Restaurant and Ordering System!", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 20));

        JButton dineInBtn = new JButton("Dine-In");
        JButton takeOutBtn = new JButton("Take-Out");
        JButton adminBtn = new JButton("Add Menu Items");

        dineInBtn.addActionListener(e -> { isTakeOut = false; cardLayout.show(mainPanel, "Categories"); });
        takeOutBtn.addActionListener(e -> { isTakeOut = true; cardLayout.show(mainPanel, "Categories"); });
        adminBtn.addActionListener(e -> openAdminPanel());

        panel.add(label);
        panel.add(dineInBtn);
        panel.add(takeOutBtn);
        panel.add(adminBtn);
        return panel;
    }

    //Category
    private JPanel createCategoryScreen() {
        JPanel panel = new JPanel(new GridLayout(5, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        JButton mealsBtn = new JButton("Meals");
        JButton drinksBtn = new JButton("Drinks");
        JButton dessertsBtn = new JButton("Desserts");
        JButton viewCartBtn = new JButton("View Cart / Checkout");
        JButton backBtn = new JButton("Return to Order Type");

        mealsBtn.addActionListener(e -> showMenuByCategory("Meals"));
        drinksBtn.addActionListener(e -> showMenuByCategory("Drinks"));
        dessertsBtn.addActionListener(e -> showMenuByCategory("Desserts"));
        viewCartBtn.addActionListener(e -> openCartPanel());
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "Welcome"));

        panel.add(new JLabel("Select Category:", SwingConstants.CENTER));
        panel.add(mealsBtn); panel.add(drinksBtn); panel.add(dessertsBtn);
        panel.add(viewCartBtn); panel.add(backBtn);
        return panel;
    }

    //Menu
    private void showMenuByCategory(String category) {
        JFrame frame = new JFrame(category);
        DefaultListModel<MenuItem> listModel = new DefaultListModel<>();

        // Filter items
        for (MenuItem item : menu) {
            if (item.category.equalsIgnoreCase(category)) listModel.addElement(item);
        }

        JList<MenuItem> list = new JList<>(listModel);
        JButton addBtn = new JButton("Add to Cart");
        JButton returnBtn = new JButton("Return to Categories");

        addBtn.addActionListener(e -> {
            MenuItem selected = list.getSelectedValue();
            if (selected != null) {
                cart.add(selected);
                JOptionPane.showMessageDialog(frame, selected.name + " added to cart!");
            }
        });

        returnBtn.addActionListener(e -> frame.dispose());

        JPanel south = new JPanel();
        south.add(addBtn);
        south.add(returnBtn);

        frame.add(new JScrollPane(list), BorderLayout.CENTER);
        frame.add(south, BorderLayout.SOUTH);
        frame.setSize(300, 400);
        frame.setLocationRelativeTo(this);
        frame.setVisible(true);
    }

    //Cart
    private void openCartPanel() {
        JFrame frame = new JFrame("View Cart");
        DefaultListModel<String> model = new DefaultListModel<>();
        updateCartModel(model);

        JList<String> cartList = new JList<>(model);
        JButton removeBtn = new JButton("Remove Selected");
        JButton payBtn = new JButton("Proceed to Payment");

        removeBtn.addActionListener(e -> {
            int idx = cartList.getSelectedIndex();
            if (idx != -1) {
                cart.remove(idx);
                updateCartModel(model);
            }
        });

        payBtn.addActionListener(e -> processPayment(frame));

        frame.setLayout(new BorderLayout());
        frame.add(new JLabel("Current Order (" + (isTakeOut ? "Take-Out" : "Dine-In") + "):"), BorderLayout.NORTH);
        frame.add(new JScrollPane(cartList), BorderLayout.CENTER);

        JPanel controls = new JPanel();
        controls.add(removeBtn);
        controls.add(payBtn);
        frame.add(controls, BorderLayout.SOUTH);

        frame.setSize(350, 450);
        frame.setLocationRelativeTo(this);
        frame.setVisible(true);
    }

    private void updateCartModel(DefaultListModel<String> model) {
        model.clear();
        for (MenuItem item : cart) model.addElement(item.name + " - ₱" + item.price);
    }

    private void processPayment(JFrame cartFrame) {
        double total = cart.stream().mapToDouble(i -> i.price).sum();

        // PWD
        int isPwd = JOptionPane.showConfirmDialog(null, "Apply PWD Discount (20%)?", "Discount", JOptionPane.YES_NO_OPTION);
        if (isPwd == JOptionPane.YES_OPTION) {
            String name = JOptionPane.showInputDialog("Enter PWD Name:");
            String id = JOptionPane.showInputDialog("Enter PWD ID Number:");
            if (name != null && id != null && !id.isEmpty()) {
                total *= 0.80;
                JOptionPane.showMessageDialog(null, "Discount Applied for " + name);
            }
        }

        // Voucher
        String voucher = JOptionPane.showInputDialog("Enter Promo Code:");
        if ("SAVE10".equalsIgnoreCase(voucher)) {
            total -= 10;
            JOptionPane.showMessageDialog(null, "Voucher Applied!");
        }

        // Payment Method
        String[] methods = {"Cash", "QR Code / GCash"};
        int method = JOptionPane.showOptionDialog(null, "Total: ₱" + total + "\nSelect Payment:",
                "Payment", 0, 0, null, methods, methods[0]);

        JOptionPane.showMessageDialog(null, "Order Successful!\nMethod: " + methods[method] + "\nThank you!");
        cart.clear();
        cartFrame.dispose();
    }

    private void openAdminPanel() {
        JFrame adminFrame = new JFrame("Add Menu");
        adminFrame.setLayout(new GridLayout(4, 2));

        JTextField nameField = new JTextField();
        JTextField priceField = new JTextField();
        String[] cats = {"Meals", "Drinks", "Desserts"};
        JComboBox<String> catBox = new JComboBox<>(cats);
        JButton addBtn = new JButton("Add Item");

        addBtn.addActionListener(e -> {
            try {
                menu.add(new MenuItem(nameField.getText(), (String)catBox.getSelectedItem(), Double.parseDouble(priceField.getText())));
                JOptionPane.showMessageDialog(adminFrame, "Item added to menu!");
            } catch (Exception ex) { JOptionPane.showMessageDialog(adminFrame, "Error in input."); }
        });

        adminFrame.add(new JLabel("Name:")); adminFrame.add(nameField);
        adminFrame.add(new JLabel("Price:")); adminFrame.add(priceField);
        adminFrame.add(new JLabel("Category:")); adminFrame.add(catBox);
        adminFrame.add(addBtn);
        adminFrame.pack();
        adminFrame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new POSdraft().setVisible(true));
    }
}
