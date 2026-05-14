package com.clothify;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/products")
public class ProductsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private String selected(String value, String selectedValue) {
        if (value == null || selectedValue == null) {
            return "";
        }
        return value.equalsIgnoreCase(selectedValue) ? "selected" : "";
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        String contextPath = request.getContextPath();
        HttpSession session = request.getSession(false);

        String category = request.getParameter("category");
        String size = request.getParameter("size");
        String color = request.getParameter("color");
        String search = request.getParameter("q");

        if (category == null) category = "";
        if (size == null) size = "";
        if (color == null) color = "";
        if (search == null) search = "";

        category = category.trim();
        size = size.trim();
        color = color.trim();
        search = search.trim();

        try {
            Connection con = DBConnection.getConnection();

            StringBuilder sql = new StringBuilder();

            sql.append("SELECT ");
            sql.append("p.product_id, ");
            sql.append("p.product_name, ");
            sql.append("p.description, ");
            sql.append("p.price, ");
            sql.append("p.image_url, ");
            sql.append("v.variant_id, ");
            sql.append("v.size, ");
            sql.append("v.color, ");
            sql.append("v.stock_quantity, ");
            sql.append("c.category_name, ");
            sql.append("c.gender ");
            sql.append("FROM products p ");
            sql.append("JOIN categories c ON p.category_id = c.category_id ");
            sql.append("JOIN product_variants v ON p.product_id = v.product_id ");
            sql.append("WHERE 1 = 1 ");

            if (!category.isEmpty()) {
                sql.append("AND c.gender = ? ");
            }

            if (!size.isEmpty()) {
                sql.append("AND v.size = ? ");
            }

            if (!color.isEmpty()) {
                sql.append("AND v.color = ? ");
            }

            if (!search.isEmpty()) {
                if (search.equalsIgnoreCase("men")
                        || search.equalsIgnoreCase("women")
                        || search.equalsIgnoreCase("kids")
                        || search.equalsIgnoreCase("unisex")) {

                    sql.append("AND c.gender = ? ");

                } else {
                    sql.append("AND (");
                    sql.append("p.product_name LIKE ? ");
                    sql.append("OR c.category_name LIKE ? ");
                    sql.append("OR c.gender LIKE ? ");
                    sql.append("OR v.size LIKE ? ");
                    sql.append("OR v.color LIKE ? ");
                    sql.append(") ");
                }
            }

            sql.append("ORDER BY p.product_id, v.variant_id");

            PreparedStatement ps = con.prepareStatement(sql.toString());

            int index = 1;

            if (!category.isEmpty()) {
                ps.setString(index++, category);
            }

            if (!size.isEmpty()) {
                ps.setString(index++, size);
            }

            if (!color.isEmpty()) {
                ps.setString(index++, color);
            }

            if (!search.isEmpty()) {
                if (search.equalsIgnoreCase("men")
                        || search.equalsIgnoreCase("women")
                        || search.equalsIgnoreCase("kids")
                        || search.equalsIgnoreCase("unisex")) {

                    String genderSearch = search.substring(0, 1).toUpperCase() + search.substring(1).toLowerCase();
                    ps.setString(index++, genderSearch);

                } else {
                    String searchValue = "%" + search + "%";
                    ps.setString(index++, searchValue);
                    ps.setString(index++, searchValue);
                    ps.setString(index++, searchValue);
                    ps.setString(index++, searchValue);
                    ps.setString(index++, searchValue);
                }
            }

            ResultSet rs = ps.executeQuery();

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<title>Products - Clothify</title>");
            out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css?v=1200'>");
            out.println("</head>");
            out.println("<body>");

            out.println(NavigationUtil.getNavbar(session));

            out.println("<section class='page-title'>");

            if (!search.isEmpty()) {
                out.println("<h1>Search Results</h1>");
                out.println("<p>Showing results for: <b>" + search + "</b></p>");
            } else {
                out.println("<h1>Products</h1>");
                out.println("<p>Explore latest fashion products</p>");
            }

            out.println("</section>");

            out.println("<form action='products' method='get' class='filter-box'>");

            out.println("<select name='category'>");
            out.println("<option value=''>All Categories</option>");
            out.println("<option value='Men' " + selected("Men", category) + ">Men</option>");
            out.println("<option value='Women' " + selected("Women", category) + ">Women</option>");
            out.println("<option value='Kids' " + selected("Kids", category) + ">Kids</option>");
            out.println("<option value='Unisex' " + selected("Unisex", category) + ">Unisex</option>");
            out.println("</select>");

            out.println("<select name='size'>");
            out.println("<option value=''>All Sizes</option>");
            out.println("<option value='S' " + selected("S", size) + ">S</option>");
            out.println("<option value='M' " + selected("M", size) + ">M</option>");
            out.println("<option value='L' " + selected("L", size) + ">L</option>");
            out.println("<option value='XL' " + selected("XL", size) + ">XL</option>");
            out.println("<option value='28' " + selected("28", size) + ">28</option>");
            out.println("<option value='30' " + selected("30", size) + ">30</option>");
            out.println("<option value='32' " + selected("32", size) + ">32</option>");
            out.println("<option value='34' " + selected("34", size) + ">34</option>");
            out.println("<option value='Free Size' " + selected("Free Size", size) + ">Free Size</option>");
            out.println("</select>");

            out.println("<select name='color'>");
            out.println("<option value=''>All Colors</option>");
            out.println("<option value='Black' " + selected("Black", color) + ">Black</option>");
            out.println("<option value='White' " + selected("White", color) + ">White</option>");
            out.println("<option value='Blue' " + selected("Blue", color) + ">Blue</option>");
            out.println("<option value='Red' " + selected("Red", color) + ">Red</option>");
            out.println("<option value='Green' " + selected("Green", color) + ">Green</option>");
            out.println("<option value='Pink' " + selected("Pink", color) + ">Pink</option>");
            out.println("<option value='Yellow' " + selected("Yellow", color) + ">Yellow</option>");
            out.println("<option value='Brown' " + selected("Brown", color) + ">Brown</option>");
            out.println("<option value='Maroon' " + selected("Maroon", color) + ">Maroon</option>");
            out.println("<option value='Grey' " + selected("Grey", color) + ">Grey</option>");
            out.println("</select>");

            if (!search.isEmpty()) {
                out.println("<input type='hidden' name='q' value='" + search + "'>");
            }

            out.println("<button type='submit'>Filter</button>");
            out.println("<a href='products'>Clear</a>");

            out.println("</form>");

            out.println("<section class='products'>");

            boolean hasProducts = false;

            while (rs.next()) {
                hasProducts = true;

                int productId = rs.getInt("product_id");
                int variantId = rs.getInt("variant_id");
                String productName = rs.getString("product_name");
                double price = rs.getDouble("price");
                String productSize = rs.getString("size");
                String productColor = rs.getString("color");
                int stockQuantity = rs.getInt("stock_quantity");
                String categoryName = rs.getString("category_name");
                String gender = rs.getString("gender");

                String imageUrl = rs.getString("image_url");

                if (imageUrl == null || imageUrl.trim().isEmpty()) {
                    imageUrl = contextPath + "/images/cat-tshirt.jpg";
                } else if (!imageUrl.startsWith("http") && !imageUrl.startsWith(contextPath)) {
                    imageUrl = contextPath + "/" + imageUrl;
                }

                out.println("<div class='product-card'>");

                out.println("<img src='" + imageUrl + "' alt='" + productName + "'>");

                out.println("<h3>" + productName + "</h3>");
                out.println("<p>₹" + price + "</p>");
                out.println("<h4>Category: " + categoryName + "</h4>");
                out.println("<h4>Gender: " + gender + "</h4>");
                out.println("<h4>Size: " + productSize + "</h4>");
                out.println("<h4>Color: " + productColor + "</h4>");

                if (stockQuantity > 0) {
                    out.println("<form action='addtocart' method='post'>");
                    out.println("<input type='hidden' name='product_id' value='" + productId + "'>");
                    out.println("<input type='hidden' name='variant_id' value='" + variantId + "'>");
                    out.println("<input type='number' name='quantity' value='1' min='1' max='" + stockQuantity + "'>");
                    out.println("<br>");
                    out.println("<button type='submit'>Add to Cart</button>");
                    out.println("</form>");
                } else {
                    out.println("<button disabled style='background:#999;'>Out of Stock</button>");
                }

                out.println("</div>");
            }

            if (!hasProducts) {
                out.println("<div class='no-products-box'>");
                out.println("<h2>No products found</h2>");
                out.println("<p>Try searching Men, Women, Kids, Unisex, Shoes, Watches, Bags, Sarees, T-Shirts.</p>");
                out.println("<a href='products'>View All Products</a>");
                out.println("</div>");
            }

            out.println("</section>");

            out.println("</body>");
            out.println("</html>");

        } catch (Exception e) {
            e.printStackTrace();

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<title>Error</title>");
            out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css?v=1200'>");
            out.println("</head>");
            out.println("<body>");
            out.println("<section class='page-title'>");
            out.println("<h1>Error loading products</h1>");
            out.println("<p>" + e.getMessage() + "</p>");
            out.println("</section>");
            out.println("</body>");
            out.println("</html>");
        }
    }
}