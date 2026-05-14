package com.clothify;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/home")
public class HomeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        String contextPath = request.getContextPath();
        HttpSession session = request.getSession(false);

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<title>Clothify</title>");
        out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css?v=1100'>");
        out.println("</head>");
        out.println("<body>");

        out.println(NavigationUtil.getNavbar(session));

        /* Top category circles */
        out.println("<section class='top-categories'>");

        out.println("<a href='products?q=Men' class='top-cat-item'>");
        out.println("<img src='images/cat-men.jpg' alt='Men'>");
        out.println("<p>Men</p>");
        out.println("</a>");

        out.println("<a href='products?q=Women' class='top-cat-item'>");
        out.println("<img src='images/cat-women.jpg' alt='Women'>");
        out.println("<p>Women</p>");
        out.println("</a>");

        out.println("<a href='products?q=Kids' class='top-cat-item'>");
        out.println("<img src='images/cat-kids.png' alt='Kids'>");
        out.println("<p>Kids</p>");
        out.println("</a>");

        out.println("<a href='products?q=Shoes' class='top-cat-item'>");
        out.println("<img src='images/cat-shoes.jpg' alt='Shoes'>");
        out.println("<p>Shoes</p>");
        out.println("</a>");

        out.println("<a href='products?q=Watches' class='top-cat-item'>");
        out.println("<img src='images/cat-watch.jpg' alt='Watches'>");
        out.println("<p>Watches</p>");
        out.println("</a>");

        out.println("<a href='products?q=Bags' class='top-cat-item'>");
        out.println("<img src='images/cat-bag.jpg' alt='Bags'>");
        out.println("<p>Bags</p>");
        out.println("</a>");

        out.println("<a href='products?q=Sarees' class='top-cat-item'>");
        out.println("<img src='images/cat-saree.jpg' alt='Sarees'>");
        out.println("<p>Sarees</p>");
        out.println("</a>");

        out.println("<a href='products?q=T-Shirts' class='top-cat-item'>");
        out.println("<img src='images/cat-tshirt.jpg' alt='T-Shirts'>");
        out.println("<p>T-Shirts</p>");
        out.println("</a>");

        out.println("</section>");

        /* Banner section */
        out.println("<section class='banner-section'>");

        out.println("<div class='banner-card'>");
        out.println("<img src='images/banner-kurti.jpg' alt='Kurti Offer'>");
        out.println("<div class='banner-text'>");
        out.println("<h3>Kurta Sets,<br>Kurtis</h3>");
        out.println("<p>Min. 70% Off</p>");
        out.println("<a href='products?q=Kurtis'>Shop Now</a>");
        out.println("</div>");
        out.println("</div>");

        out.println("<div class='banner-card'>");
        out.println("<img src='images/banner-shoes.jpg' alt='Shoes Offer'>");
        out.println("<div class='banner-text'>");
        out.println("<h3>Men's<br>Sneakers</h3>");
        out.println("<p>Under ₹999</p>");
        out.println("<a href='products?q=Shoes'>Shop Now</a>");
        out.println("</div>");
        out.println("</div>");

        out.println("<div class='banner-card'>");
        out.println("<img src='images/banner-dress.jpg' alt='Dress Offer'>");
        out.println("<div class='banner-text'>");
        out.println("<h3>Dresses,<br>Tops</h3>");
        out.println("<p>Under ₹399</p>");
        out.println("<a href='products?q=Dresses'>Shop Now</a>");
        out.println("</div>");
        out.println("</div>");

        out.println("</section>");

        /* Shop by Category */
        out.println("<section class='section-box'>");
        out.println("<div class='section-title-row'>");
        out.println("<h2>Shop by Category</h2>");
        out.println("<a href='products'>View All</a>");
        out.println("</div>");

        out.println("<div class='category-grid'>");

        out.println("<a href='products?q=Men' class='category-card'>");
        out.println("<img src='images/cat-men.jpg' alt='Men'>");
        out.println("<h4>Men</h4>");
        out.println("</a>");

        out.println("<a href='products?q=Women' class='category-card'>");
        out.println("<img src='images/cat-women.jpg' alt='Women'>");
        out.println("<h4>Women</h4>");
        out.println("</a>");

        out.println("<a href='products?q=Kids' class='category-card'>");
        out.println("<img src='images/cat-kids.png' alt='Kids'>");
        out.println("<h4>Kids</h4>");
        out.println("</a>");

        out.println("<a href='products?q=Shoes' class='category-card'>");
        out.println("<img src='images/cat-shoes.jpg' alt='Shoes'>");
        out.println("<h4>Shoes</h4>");
        out.println("</a>");

        out.println("<a href='products?q=Watches' class='category-card'>");
        out.println("<img src='images/cat-watch.jpg' alt='Watches'>");
        out.println("<h4>Watches</h4>");
        out.println("</a>");

        out.println("<a href='products?q=Bags' class='category-card'>");
        out.println("<img src='images/cat-bag.jpg' alt='Bags'>");
        out.println("<h4>Bags</h4>");
        out.println("</a>");

        out.println("<a href='products?q=Sarees' class='category-card'>");
        out.println("<img src='images/cat-saree.jpg' alt='Sarees'>");
        out.println("<h4>Sarees</h4>");
        out.println("</a>");

        out.println("<a href='products?q=T-Shirts' class='category-card'>");
        out.println("<img src='images/cat-tshirt.jpg' alt='T-Shirts'>");
        out.println("<h4>T-Shirts</h4>");
        out.println("</a>");

        out.println("</div>");
        out.println("</section>");

        out.println("</body>");
        out.println("</html>");
    }
}