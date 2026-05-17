import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

class MenuItem {
    int id;
    String name, category;
    double price;

    public MenuItem(int id, String name, String category, double price) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
    }

    @Override
    public String toString() {
        return name + " - ₱" + String.format("%.2f", price);
    }
}

class Voucher {
    String code;
    double discount;

    public Voucher(String code, double discount) {
        this.code = code;
        this.discount = discount;
    }
}

class CartItem {
    MenuItem item;
    int quantity;

    public CartItem(MenuItem item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    double getSubtotal() {
        return item.price * quantity;
    }
}

class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/restaurant_kiosk";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");  
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}

class MenuDAO {
    public static List<MenuItem> getAllItems() {
        List<MenuItem> items = new ArrayList<>();
        String sql = "SELECT * FROM menu_items ORDER BY category, name";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(new MenuItem(rs.getInt("id"), rs.getString("name"),
                        rs.getString("category"), rs.getDouble("price")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public static List<String> getCategories() {
        List<String> cats = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM menu_items ORDER BY category";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) cats.add(rs.getString("category"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cats;
    }

    public static void addItem(String name, String category, double price) {
        String sql = "INSERT INTO menu_items (name, category, price) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, category);
            pstmt.setDouble(3, price);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateItem(int id, String name, String category, double price) {
        String sql = "UPDATE menu_items SET name=?, category=?, price=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, category);
            pstmt.setDouble(3, price);
            pstmt.setInt(4, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteItem(int id) {
        String sql = "DELETE FROM menu_items WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

class VoucherDAO {
    public static Voucher getVoucher(String code) {
        String sql = "SELECT * FROM vouchers WHERE code=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Voucher(rs.getString("code"), rs.getDouble("discount_amount"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Voucher> getAllVouchers() {
        List<Voucher> list = new ArrayList<>();
        String sql = "SELECT * FROM vouchers";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Voucher(rs.getString("code"), rs.getDouble("discount_amount")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void addVoucher(String code, double discount) {
        String sql = "INSERT INTO vouchers (code, discount_amount) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code.toUpperCase());
            pstmt.setDouble(2, discount);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteVoucher(String code) {
        String sql = "DELETE FROM vouchers WHERE code=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

class OrderDAO {
    public static void saveOrder(String orderType, double total, List<CartItem> cartItems) {
        String orderSql = "INSERT INTO orders (order_type, total) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, orderType);
            pstmt.setDouble(2, total);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int orderId = rs.getInt(1);
                String itemSql = "INSERT INTO order_items (order_id, menu_item_id, quantity, price) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt2 = conn.prepareStatement(itemSql)) {
                    for (CartItem ci : cartItems) {
                        pstmt2.setInt(1, orderId);
                        pstmt2.setInt(2, ci.item.id);
                        pstmt2.setInt(3, ci.quantity);
                        pstmt2.setDouble(4, ci.item.price);
                        pstmt2.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

public class RestaurantKiosk extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private String orderType = "Dine-In";
    private List<CartItem> cart = new ArrayList<>();
    private Voucher appliedVoucher = null;
    private boolean pwdApplied = false;
    private static final double TAX_RATE = 0.12;
    private CategoryPanel categoryPanel;
    private CartPanel cartPanel;

    public RestaurantKiosk() {
        setTitle("Restaurant Kiosk - Self Ordering System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 600));

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(new WelcomePanel(), "welcome");
        categoryPanel = new CategoryPanel();
        mainPanel.add(categoryPanel, "categories");
        cartPanel = new CartPanel();
        mainPanel.add(cartPanel, "cart");

        add(mainPanel);
        cardLayout.show(mainPanel, "welcome");
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btn.setBackground(bg);
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(12, 25, 12, 25));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public void refreshCategoryButton() {
        if (categoryPanel != null) {
            categoryPanel.updateCartButton();
        }
        if (cartPanel != null) {
            cartPanel.refreshDisplay();
        }
    }

    class WelcomePanel extends JPanel {
        public WelcomePanel() {
            setLayout(new GridBagLayout());
            setBackground(new Color(207, 207, 207));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(15, 15, 15, 15);
            gbc.gridwidth = GridBagConstraints.REMAINDER;

            JLabel title = new JLabel("Welcome to our Restaurant Kiosk", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 28));
            title.setForeground(new Color(44, 43, 43));
            add(title, gbc);

            JButton dineIn = createStyledButton("Dine-In", new Color(207, 207, 207));
            JButton takeOut = createStyledButton("Take-Out", new Color(207, 207, 207));
            JButton admin = createStyledButton("Admin Panel", new Color(207, 207, 207));

            dineIn.addActionListener(e -> {
                orderType = "Dine-In";
                loadCategoryScreen();
            });
            takeOut.addActionListener(e -> {
                orderType = "Take-Out";
                loadCategoryScreen();
            });
            admin.addActionListener(e -> {
                if (authenticateAdmin()) {
                    new AdminFrame().setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid Admin Password!");
                }
            });

            add(dineIn, gbc);
            add(takeOut, gbc);
            add(admin, gbc);

//            JLabel footer = new JLabel("Please select ordering type to continue");
//            footer.setFont(new Font("Arial", Font.ITALIC, 12));
//            add(footer, gbc);
        }

        private boolean authenticateAdmin() {
            String pass = JOptionPane.showInputDialog(this, "Enter Admin PIN:");
            return "1234".equals(pass);
        }
    }

    class CategoryPanel extends JPanel {
        private JButton viewCartButton;

        public CategoryPanel() {
            setLayout(new BorderLayout(10, 10));
            setBackground(Color.LIGHT_GRAY);
            setBorder(new EmptyBorder(20, 20, 20, 20));

            JLabel header = new JLabel("Select Category", SwingConstants.CENTER);
            header.setFont(new Font("Segoe UI", Font.BOLD, 24));
            add(header, BorderLayout.NORTH);

            JPanel catGrid = new JPanel(new GridLayout(0, 2, 15, 15));
            catGrid.setBackground(Color.LIGHT_GRAY);
            List<String> categories = MenuDAO.getCategories();
            for (String cat : categories) {
                JButton catBtn = new JButton(cat);
                catBtn.setFont(new Font("Segoe UI", Font.BOLD, 20));
                catBtn.setBackground(new Color(207, 207, 207));
                catBtn.setForeground(Color.BLACK);
                catBtn.addActionListener(e -> showMenuForCategory(cat));
                catGrid.add(catBtn);
            }

            viewCartButton = new JButton("View Cart (" + cart.size() + " items)");
            viewCartButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
            viewCartButton.setBackground(new Color(207, 207, 207));
            viewCartButton.setForeground(Color.BLACK);
            viewCartButton.addActionListener(e -> cardLayout.show(mainPanel, "cart"));

            JButton back = new JButton("Back to Welcome");
            back.setFont(new Font("Segoe UI", Font.BOLD, 16));
            back.setBackground(new Color(207, 207, 207));
            back.setForeground(Color.BLACK);
            back.addActionListener(e -> cardLayout.show(mainPanel, "welcome"));

            JPanel bottom = new JPanel(new GridLayout(1, 2, 10, 10));
            bottom.add(viewCartButton);
            bottom.add(back);

            add(new JScrollPane(catGrid), BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);
        }

        public void updateCartButton() {
            viewCartButton.setText("View Cart (" + cart.size() + " items)");
        }

        private void showMenuForCategory(String category) {
            JFrame menuFrame = new JFrame(category + " Menu");
            menuFrame.setSize(500, 550);
            menuFrame.setLocationRelativeTo(RestaurantKiosk.this);
            menuFrame.setLayout(new BorderLayout());

            DefaultListModel<MenuItem> model = new DefaultListModel<>();
            List<MenuItem> items = MenuDAO.getAllItems().stream()
                    .filter(i -> i.category.equals(category))
                    .collect(Collectors.toList());
            items.forEach(model::addElement);

            JList<MenuItem> list = new JList<>(model);
            list.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    MenuItem item = (MenuItem) value;
                    label.setText(String.format("%s - ₱%.2f", item.name, item.price));
                    label.setFont(new Font("Arial", Font.PLAIN, 16));
                    return label;
                }
            });

            JPanel controls = new JPanel();
            JLabel quantityLabel = new JLabel("Quantity:");
            quantityLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            controls.add(quantityLabel);

            JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
            controls.add(qtySpinner);

            JButton addBtn = new JButton("Add to Cart");
            addBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            addBtn.addActionListener(e -> {
                MenuItem selected = list.getSelectedValue();
                if (selected != null) {
                    int qty = (Integer) qtySpinner.getValue();
                    addToCart(selected, qty);
                    JOptionPane.showMessageDialog(menuFrame, qty + " x " + selected.name + " added!");
                    menuFrame.dispose();
                    cardLayout.show(mainPanel, "categories");
                    refreshCategoryButton();
                } else {
                    JOptionPane.showMessageDialog(menuFrame, "Please select an item");
                }
            });

            JButton cancel = new JButton("Cancel");
            cancel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            cancel.addActionListener(e -> menuFrame.dispose());

            controls.add(addBtn);
            controls.add(cancel);

            menuFrame.add(new JScrollPane(list), BorderLayout.CENTER);
            menuFrame.add(controls, BorderLayout.SOUTH);
            menuFrame.setVisible(true);
        }
    }

    class CartPanel extends JPanel {
        private DefaultListModel<String> cartModel;
        private JList<String> cartList;
        private JLabel subtotalLabel, pwdDiscountLabel, afterPwdLabel, voucherLabel, taxLabel, totalLabel;
        private JButton applyPwdBtn;

        public CartPanel() {
            setLayout(new BorderLayout(10, 10));
            setBorder(new EmptyBorder(15, 15, 15, 15));

            JLabel title = new JLabel("Your Order (" + orderType + ")", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 22));
            add(title, BorderLayout.NORTH);

            cartModel = new DefaultListModel<>();
            cartList = new JList<>(cartModel);
            cartList.setFont(new Font("Monospaced", Font.PLAIN, 14));

            cartList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int idx = cartList.locationToIndex(e.getPoint());
                        if (idx >= 0 && idx < cart.size()) {
                            CartItem ci = cart.get(idx);
                            String newQtyStr = JOptionPane.showInputDialog(CartPanel.this,
                                    "Enter new quantity for " + ci.item.name + " (current: " + ci.quantity + "):",
                                    ci.quantity);
                            if (newQtyStr != null) {
                                try {
                                    int newQty = Integer.parseInt(newQtyStr);
                                    if (newQty > 0 && newQty <= 20) {
                                        ci.quantity = newQty;
                                        refreshDisplay();
                                        refreshCategoryButton();
                                    } else if (newQty <= 0) {
                                        cart.remove(idx);
                                        refreshDisplay();
                                        refreshCategoryButton();
                                    } else {
                                        JOptionPane.showMessageDialog(CartPanel.this,
                                                "Quantity must be between 1 and 20.");
                                    }
                                } catch (NumberFormatException ex) {
                                    JOptionPane.showMessageDialog(CartPanel.this,
                                            "Invalid number.");
                                }
                            }
                        }
                    }
                }
            });

            add(new JScrollPane(cartList), BorderLayout.CENTER);

            JPanel detailsPanel = new JPanel();
            detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));

            TitledBorder orderBorder = new TitledBorder("Order Summary");
            orderBorder.setTitleFont(new Font("Segoe UI", Font.BOLD, 14));
            orderBorder.setTitleJustification(TitledBorder.LEFT);
            detailsPanel.setBorder(orderBorder);
            detailsPanel.setPreferredSize(new Dimension(300, 0));

            subtotalLabel = new JLabel("Subtotal: ₱0.00");
            pwdDiscountLabel = new JLabel("PWD/Senior Discount (20%): ₱0.00");
            afterPwdLabel = new JLabel("After PWD Discount: ₱0.00");
            voucherLabel = new JLabel("Voucher Discount: ₱0.00");
            taxLabel = new JLabel("Tax (12%): ₱0.00");
            totalLabel = new JLabel("Total: ₱0.00");

            subtotalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            pwdDiscountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            afterPwdLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            voucherLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            taxLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            totalLabel.setHorizontalAlignment(SwingConstants.LEFT);
            totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

            Font labelFont = new Font("Segoe UI", Font.BOLD, 12);
            subtotalLabel.setFont(labelFont);
            pwdDiscountLabel.setFont(labelFont);
            afterPwdLabel.setFont(labelFont);
            voucherLabel.setFont(labelFont);
            taxLabel.setFont(labelFont);

            applyPwdBtn = new JButton("Apply PWD / Senior Discount");
            applyPwdBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            applyPwdBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            applyPwdBtn.addActionListener(e -> applyPwdDiscount());

            JPanel voucherPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            voucherPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel promoCodeLabel = new JLabel("Promo Code:");
            promoCodeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            JTextField voucherField = new JTextField(10);
            JButton applyVoucherBtn = new JButton("Apply Voucher");
            applyVoucherBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            applyVoucherBtn.addActionListener(e -> {
                String code = voucherField.getText().trim();
                Voucher v = VoucherDAO.getVoucher(code);
                if (v != null) {
                    appliedVoucher = v;
                    refreshDisplay();
                    JOptionPane.showMessageDialog(this, "Voucher Applied: " + v.code + " - ₱" + v.discount + " off");
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid Voucher Code");
                }
                voucherField.setText("");
            });
            voucherPanel.add(promoCodeLabel);
            voucherPanel.add(voucherField);
            voucherPanel.add(applyVoucherBtn);

            detailsPanel.add(subtotalLabel);
            detailsPanel.add(Box.createVerticalStrut(5));
            detailsPanel.add(pwdDiscountLabel);
            detailsPanel.add(Box.createVerticalStrut(5));
            detailsPanel.add(afterPwdLabel);
            detailsPanel.add(Box.createVerticalStrut(5));
            detailsPanel.add(voucherLabel);
            detailsPanel.add(Box.createVerticalStrut(5));
            detailsPanel.add(taxLabel);
            detailsPanel.add(Box.createVerticalStrut(10));
            detailsPanel.add(totalLabel);
            detailsPanel.add(Box.createVerticalStrut(10));
            detailsPanel.add(applyPwdBtn);
            detailsPanel.add(voucherPanel);

            JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 10, 10));
            JButton removeBtn = new JButton("Remove Selected");
            JButton clearBtn = new JButton("Clear Cart");
            JButton checkoutBtn = new JButton("Proceed to Payment");
            JButton backBtn = new JButton("Back to Categories");

            Font buttonFont = new Font("Segoe UI", Font.BOLD, 12);
            removeBtn.setFont(buttonFont);
            clearBtn.setFont(buttonFont);
            checkoutBtn.setFont(buttonFont);
            backBtn.setFont(buttonFont);

            removeBtn.addActionListener(e -> {
                int idx = cartList.getSelectedIndex();
                if (idx >= 0 && idx < cart.size()) {
                    cart.remove(idx);
                    resetDiscounts();
                    refreshDisplay();
                    refreshCategoryButton();
                }
            });
            clearBtn.addActionListener(e -> {
                cart.clear();
                resetDiscounts();
                refreshDisplay();
                refreshCategoryButton();
            });
            checkoutBtn.addActionListener(e -> processPayment());
            backBtn.addActionListener(e -> cardLayout.show(mainPanel, "categories"));

            buttonPanel.add(removeBtn);
            buttonPanel.add(clearBtn);
            buttonPanel.add(checkoutBtn);
            buttonPanel.add(backBtn);

            add(detailsPanel, BorderLayout.EAST);
            add(buttonPanel, BorderLayout.SOUTH);
            refreshDisplay();
        }

        public void refreshDisplay() {
            cartModel.clear();
            for (CartItem ci : cart) {
                cartModel.addElement(ci.item.name + " x" + ci.quantity + " = ₱" + String.format("%.2f", ci.getSubtotal()));
            }

            double subtotal = cart.stream().mapToDouble(CartItem::getSubtotal).sum();
            double pwdDiscount = pwdApplied ? subtotal * 0.20 : 0;
            double afterPwd = subtotal - pwdDiscount;
            double voucherDiscount = (appliedVoucher != null) ? appliedVoucher.discount : 0;
            double afterVoucher = afterPwd - voucherDiscount;
            double tax = afterPwd * TAX_RATE;
            double total = afterVoucher + tax;

            subtotalLabel.setText("Subtotal: ₱" + String.format("%.2f", subtotal));
            pwdDiscountLabel.setText("PWD/Senior Discount (20%): ₱" + String.format("%.2f", pwdDiscount));
            afterPwdLabel.setText("After PWD Discount: ₱" + String.format("%.2f", afterPwd));
            voucherLabel.setText("Voucher Discount: ₱" + String.format("%.2f", voucherDiscount));
            taxLabel.setText("Tax (12%): ₱" + String.format("%.2f", tax));
            totalLabel.setText("Total: ₱" + String.format("%.2f", total));
        }

        private void resetDiscounts() {
            pwdApplied = false;
            appliedVoucher = null;
        }

        private void applyPwdDiscount() {
            if (pwdApplied) {
                JOptionPane.showMessageDialog(this, "PWD discount already applied.");
                return;
            }
            String name = JOptionPane.showInputDialog(this, "Enter PWD/Senior Name:");
            if (name == null || name.trim().isEmpty()) return;
            String id = JOptionPane.showInputDialog(this, "Enter PWD/Senior ID Number:");
            if (id == null || id.trim().isEmpty()) return;
            pwdApplied = true;
            refreshDisplay();
            JOptionPane.showMessageDialog(this, "20% PWD discount applied for " + name);
        }

        private void processPayment() {
            if (cart.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Cart is empty!");
                return;
            }
            double subtotal = cart.stream().mapToDouble(CartItem::getSubtotal).sum();
            double pwdDiscount = pwdApplied ? subtotal * 0.20 : 0;
            double afterPwd = subtotal - pwdDiscount;
            double voucherDiscount = (appliedVoucher != null) ? appliedVoucher.discount : 0;
            double afterVoucher = afterPwd - voucherDiscount;
            double tax = afterPwd * TAX_RATE;
            double total = afterVoucher + tax;

            String[] options = {"Cash", "Card"};
            int choice = JOptionPane.showOptionDialog(this, "Total Amount: ₱" + String.format("%.2f", total) + "\nSelect Payment Method",
                    "Payment", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

            boolean success = false;
            if (choice == 0) {
                String input = JOptionPane.showInputDialog(this, "Enter Cash Amount:");
                try {
                    double cash = Double.parseDouble(input);
                    if (cash >= total) {
                        double change = cash - total;
                        JOptionPane.showMessageDialog(this, "Payment Successful!\nChange: ₱" + String.format("%.2f", change));
                        success = true;
                    } else {
                        JOptionPane.showMessageDialog(this, "Insufficient cash!");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid amount");
                }
            } else if (choice == 1) {
                JOptionPane.showMessageDialog(this, "Card Payment Processed Successfully!");
                success = true;
            }

            if (success) {
                OrderDAO.saveOrder(orderType, total, cart);
                JOptionPane.showMessageDialog(this, "Order Completed!\nThank you for dining with us.");
                cart.clear();
                resetDiscounts();
                refreshDisplay();
                refreshCategoryButton();
                cardLayout.show(mainPanel, "welcome");
            }
        }
    }

    class AdminFrame extends JFrame {
        private JTable menuTable;
        private DefaultTableModel menuTableModel;
        private JTable voucherTable;
        private DefaultTableModel voucherTableModel;

        public AdminFrame() {
            setTitle("Admin Control Panel");
            setSize(900, 600);
            setLocationRelativeTo(RestaurantKiosk.this);
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Manage Menu", createMenuPanel());
            tabs.addTab("Manage Vouchers", createVoucherPanel());
            add(tabs);
        }

        private JPanel createMenuPanel() {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            String[] cols = {"ID", "Name", "Category", "Price (₱)"};
            menuTableModel = new DefaultTableModel(cols, 0) {
                @Override
                public boolean isCellEditable(int row, int col) { return false; }
            };
            menuTable = new JTable(menuTableModel);
            loadMenuTable();

            JPanel btnPanel = new JPanel(new FlowLayout());
            JButton addBtn = new JButton("Add Item");
            JButton editBtn = new JButton("Edit Item");
            JButton deleteBtn = new JButton("Delete Item");
            JButton refreshBtn = new JButton("Refresh");

            addBtn.addActionListener(e -> showAddMenuDialog());
            editBtn.addActionListener(e -> showEditMenuDialog());
            deleteBtn.addActionListener(e -> deleteMenuItem());
            refreshBtn.addActionListener(e -> loadMenuTable());

            btnPanel.add(addBtn);
            btnPanel.add(editBtn);
            btnPanel.add(deleteBtn);
            btnPanel.add(refreshBtn);

            panel.add(new JScrollPane(menuTable), BorderLayout.CENTER);
            panel.add(btnPanel, BorderLayout.SOUTH);
            return panel;
        }

        private void loadMenuTable() {
            menuTableModel.setRowCount(0);
            List<MenuItem> items = MenuDAO.getAllItems();
            for (MenuItem mi : items) {
                menuTableModel.addRow(new Object[]{mi.id, mi.name, mi.category, mi.price});
            }
        }

        private void showAddMenuDialog() {
            JTextField nameField = new JTextField();

            JComboBox<String> catCombo = new JComboBox<>(MenuDAO.getCategories().toArray(new String[0]));
            catCombo.setEditable(true);

            JTextField priceField = new JTextField();
            Object[] msg = {"Name:", nameField, "Category (or type new):", catCombo, "Price:", priceField};
            int opt = JOptionPane.showConfirmDialog(this, msg, "Add Menu Item", JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.OK_OPTION) {
                try {
                    double price = Double.parseDouble(priceField.getText());
                    String category = catCombo.getEditor().getItem().toString().trim();
                    if (category.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Category cannot be empty.");
                        return;
                    }
                    MenuDAO.addItem(nameField.getText(), category, price);
                    loadMenuTable();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Invalid price");
                }
            }
        }

        private void showEditMenuDialog() {
            int row = menuTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select an item"); return; }
            int id = (int) menuTableModel.getValueAt(row, 0);
            String oldName = (String) menuTableModel.getValueAt(row, 1);
            String oldCat = (String) menuTableModel.getValueAt(row, 2);
            double oldPrice = (double) menuTableModel.getValueAt(row, 3);

            JTextField nameField = new JTextField(oldName);

            JComboBox<String> catCombo = new JComboBox<>(MenuDAO.getCategories().toArray(new String[0]));
            catCombo.setEditable(true);
            catCombo.setSelectedItem(oldCat);

            JTextField priceField = new JTextField(String.valueOf(oldPrice));
            Object[] msg = {"Name:", nameField, "Category (or type new):", catCombo, "Price:", priceField};
            int opt = JOptionPane.showConfirmDialog(this, msg, "Edit Item", JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.OK_OPTION) {
                try {
                    double price = Double.parseDouble(priceField.getText());
                    String category = catCombo.getEditor().getItem().toString().trim();
                    if (category.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Category cannot be empty.");
                        return;
                    }
                    MenuDAO.updateItem(id, nameField.getText(), category, price);
                    loadMenuTable();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Invalid price");
                }
            }
        }

        private void deleteMenuItem() {
            int row = menuTable.getSelectedRow();
            if (row == -1) return;
            int id = (int) menuTableModel.getValueAt(row, 0);
            int confirm = JOptionPane.showConfirmDialog(this, "Delete this item?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                MenuDAO.deleteItem(id);
                loadMenuTable();
            }
        }

        private JPanel createVoucherPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            String[] cols = {"Voucher Code", "Discount (₱)"};
            voucherTableModel = new DefaultTableModel(cols, 0);
            voucherTable = new JTable(voucherTableModel);
            loadVoucherTable();

            JPanel btnPanel = new JPanel();
            JButton addVoucher = new JButton("Add Voucher");
            JButton delVoucher = new JButton("Delete Voucher");
            addVoucher.addActionListener(e -> {
                String code = JOptionPane.showInputDialog(this, "Code:");
                if (code != null && !code.isEmpty()) {
                    String discStr = JOptionPane.showInputDialog(this, "Discount Amount:");
                    try {
                        double disc = Double.parseDouble(discStr);
                        VoucherDAO.addVoucher(code, disc);
                        loadVoucherTable();
                    } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Invalid number"); }
                }
            });
            delVoucher.addActionListener(e -> {
                int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this voucher?", "Voucher Deletion", JOptionPane.YES_NO_OPTION);
                if(choice == JOptionPane.YES_OPTION){
                    int row = voucherTable.getSelectedRow();
                    if (row >= 0) {
                        String code = (String) voucherTableModel.getValueAt(row, 0);
                        VoucherDAO.deleteVoucher(code);
                        loadVoucherTable();
                    }
                }
                
            });
            btnPanel.add(addVoucher);
            btnPanel.add(delVoucher);
            panel.add(new JScrollPane(voucherTable), BorderLayout.CENTER);
            panel.add(btnPanel, BorderLayout.SOUTH);
            return panel;
        }

        private void loadVoucherTable() {
            voucherTableModel.setRowCount(0);
            List<Voucher> vouchers = VoucherDAO.getAllVouchers();
            for (Voucher v : vouchers) {
                voucherTableModel.addRow(new Object[]{v.code, v.discount});
            }
        }
    }

    private void loadCategoryScreen() {
        cardLayout.show(mainPanel, "categories");
    }

    private void addToCart(MenuItem item, int quantity) {
        for (CartItem ci : cart) {
            if (ci.item.id == item.id) {
                ci.quantity += quantity;
                refreshCategoryButton();
                return;
            }
        }
        cart.add(new CartItem(item, quantity));
        refreshCategoryButton();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) { }
            new RestaurantKiosk().setVisible(true);
        });
    }
}
