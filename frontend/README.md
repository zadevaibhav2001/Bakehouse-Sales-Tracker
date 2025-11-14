# The Bakehouse - Sales Dashboard

A modern, responsive web application for managing bakery sales, products, and orders.

## Features

### Dashboard
- Real-time statistics (Total Orders, Revenue, Products, Stock Status)
- Recent orders overview
- Quick access to key metrics

### Products Management
- Add new products with name, price, and stock status
- Update stock status (In Stock / Out of Stock)
- Delete products
- Search and filter products
- Export products to Excel

### Orders Management
- Create new orders
- View all orders in a table format
- Filter orders by date range
- Delete orders
- Export orders to Excel
- Automatic total cost calculation

### Reports
- Revenue reports by date range
- High-value orders search
- Export capabilities for all data

## Setup

### Prerequisites
- Backend API running on `http://localhost:8080`
- Modern web browser

### Installation

1. **Serve the frontend files:**

   Using Python:
   ```bash
   cd frontend
   python3 -m http.server 3000
   ```

   Using Node.js (http-server):
   ```bash
   npm install -g http-server
   cd frontend
   http-server -p 3000
   ```

   Using VS Code Live Server:
   - Install "Live Server" extension
   - Right-click on `index.html`
   - Select "Open with Live Server"

2. **Access the application:**
   Open your browser and navigate to `http://localhost:3000`

## Configuration

If your backend API is running on a different port or host, update the `API_BASE` constant in `js/app.js`:

```javascript
const API_BASE = 'http://your-backend-url:port/api';
```

## Usage

### Adding a Product
1. Navigate to the "Products" section
2. Click "Add Product" button
3. Fill in product name, price, and stock status
4. Click "Add Product" to save

### Creating an Order
1. Navigate to the "Orders" section
2. Click "New Order" button
3. Select a product from the dropdown
4. Enter quantity
5. Click "Create Order"
6. Total cost is calculated automatically

### Generating Reports
1. Navigate to the "Reports" section
2. For revenue reports: Select date range and click "Generate"
3. For high-value orders: Enter minimum cost and click "Search"

### Exporting Data
- Click the "Export Excel" button in Products or Orders sections
- The file will download automatically with a timestamp

## Design

The application features:
- **Color Scheme**: Warm bakery colors (browns, oranges)
- **Responsive Design**: Works on desktop, tablet, and mobile
- **Modern UI**: Clean cards, smooth transitions, and intuitive navigation
- **Icons**: Font Awesome icons for visual clarity

## Browser Support

- Chrome (recommended)
- Firefox
- Safari
- Edge

## Troubleshooting

### CORS Issues
If you encounter CORS errors, ensure your backend has CORS enabled for the frontend origin.

Add to your Spring Boot application:
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE");
    }
}
```

### API Connection Failed
- Verify the backend is running on `http://localhost:8080`
- Check the browser console for error messages
- Ensure the API endpoints are accessible

## Future Enhancements

- User authentication
- Advanced analytics and charts
- Print receipts
- Inventory management
- Customer management
- Multi-language support
