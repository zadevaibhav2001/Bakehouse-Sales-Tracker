// API Base URL - Uses relative path when deployed, or EC2 IP for local testing
const API_BASE = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    ? 'http://3.110.159.51:8080/api'  // For local development
    : '/api';  // For production (Nginx will proxy to backend)

// Initialize app
document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    initMobileMenu();
    setDefaultDates();
    loadDashboard();
    loadProducts();
    loadCreateOrders();
});

// Set today's date as default for all date inputs
function setDefaultDates() {
    const today = new Date().toISOString().split('T')[0];
    
    // Orders section dates
    const startDate = document.getElementById('startDate');
    const endDate = document.getElementById('endDate');
    if (startDate) startDate.value = today;
    if (endDate) endDate.value = today;
    
    // Reports section dates
    const reportStartDate = document.getElementById('reportStartDate');
    const reportEndDate = document.getElementById('reportEndDate');
    if (reportStartDate) reportStartDate.value = today;
    if (reportEndDate) reportEndDate.value = today;
}

// Navigation
function initNavigation() {
    const navLinks = document.querySelectorAll('.nav-menu a');
    const navMenu = document.getElementById('navMenu');
    
    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const target = link.getAttribute('href').substring(1);
            showSection(target);
            
            navLinks.forEach(l => l.classList.remove('active'));
            link.classList.add('active');
            
            // Close mobile menu after clicking
            if (navMenu) {
                navMenu.classList.remove('active');
            }
        });
    });
}

// Mobile Menu
function initMobileMenu() {
    const toggle = document.getElementById('mobileMenuToggle');
    const navMenu = document.getElementById('navMenu');
    
    const closeMenu = () => {
        navMenu.classList.remove('active');
        toggle.querySelector('i').className = 'fas fa-bars';
    };
    
    const openMenu = () => {
        navMenu.classList.add('active');
        toggle.querySelector('i').className = 'fas fa-times';
    };
    
    if (toggle && navMenu) {
        // Toggle menu
        toggle.addEventListener('click', (e) => {
            e.stopPropagation();
            if (navMenu.classList.contains('active')) {
                closeMenu();
            } else {
                openMenu();
            }
        });
        
        // Close menu when clicking on the menu itself (close button area at top)
        navMenu.addEventListener('click', (e) => {
            // Check if click is in the top 60px (close button area)
            const rect = navMenu.getBoundingClientRect();
            if (e.clientY - rect.top < 60 && e.clientX > rect.right - 100) {
                closeMenu();
            }
        });
        
        // Close menu when clicking outside
        document.addEventListener('click', (e) => {
            if (navMenu.classList.contains('active') && 
                !toggle.contains(e.target) && 
                !navMenu.contains(e.target)) {
                closeMenu();
            }
        });
        
        // Close menu on escape key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && navMenu.classList.contains('active')) {
                closeMenu();
            }
        });
    }
}

function showSection(sectionId) {
    document.querySelectorAll('.section').forEach(section => {
        section.classList.remove('active');
    });
    document.getElementById(sectionId).classList.add('active');
    
    // Only reload data if not already loaded
    if (sectionId === 'dashboard') loadDashboard();
    if (sectionId === 'products') loadProducts();
    if (sectionId === 'create-orders' && !window.ordersLoaded) loadCreateOrders();
    if (sectionId === 'order-history') loadOrderHistory();
}

// Dashboard
async function loadDashboard() {
    try {
        const today = new Date().toISOString().split('T')[0];
        const [orders, todayRevenue, products] = await Promise.all([
            fetch(`${API_BASE}/orders`).then(r => r.json()),
            fetch(`${API_BASE}/orders/revenue/between?start=${today}T00:00:00&end=${today}T23:59:59`).then(r => r.json()),
            fetch(`${API_BASE}/products`).then(r => r.json())
        ]);

        document.getElementById('totalOrders').textContent = orders.length;
        document.getElementById('totalRevenue').textContent = `₹${todayRevenue.totalRevenue.toFixed(2)}`;
        document.getElementById('totalProducts').textContent = products.length;

        displayRecentOrders(orders.slice(0, 5));
    } catch (error) {
        Swal.fire({
            icon: 'error',
            title: 'Error loading dashboard',
            text: 'Failed to load dashboard data'
        });
    }
}

function displayRecentOrders(orders) {
    const container = document.getElementById('recentOrdersList');
    if (orders.length === 0) {
        container.innerHTML = '<p>No orders yet</p>';
        return;
    }

    container.innerHTML = orders.map(order => `
        <div class="order-item">
            <div class="order-info">
                <h4>${order.product.name}</h4>
                <p>Quantity: ${order.quantity} | ${formatDate(order.orderDateTime)}</p>
            </div>
            <div class="order-cost">₹${order.totalCost.toFixed(2)}</div>
        </div>
    `).join('');
}

// Products
async function loadProducts() {
    try {
        const response = await fetch(`${API_BASE}/products`);
        const products = await response.json();
        displayProducts(products);
    } catch (error) {
        Swal.fire({
            icon: 'error',
            title: 'Error loading products',
            text: 'Failed to load products'
        });
    }
}

function displayProducts(products) {
    const container = document.getElementById('productsList');
    if (products.length === 0) {
        container.innerHTML = '<p>No products found</p>';
        return;
    }

    container.innerHTML = products.map(product => `
        <div class="product-card">
            <div class="product-header">
                <div>
                    <div class="product-name">${product.name}</div>
                    <span class="product-status ${product.inStock ? 'status-in-stock' : 'status-out-of-stock'}">
                        ${product.inStock ? 'In Stock' : 'Out of Stock'}
                    </span>
                </div>
                <div class="product-price">₹${product.price.toFixed(2)}</div>
            </div>
            <div class="product-actions">
                <button class="btn btn-secondary" onclick="toggleStock(${product.id}, ${!product.inStock})">
                    <i class="fas fa-${product.inStock ? 'times' : 'check'}"></i>
                    ${product.inStock ? 'Mark Out' : 'Mark In'}
                </button>
                <button class="btn btn-danger" onclick="deleteProduct(${product.id})">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        </div>
    `).join('');
}

async function addProduct(event) {
    event.preventDefault();
    const form = event.target;
    const formData = new FormData(form);
    
    const product = {
        name: formData.get('name'),
        price: parseFloat(formData.get('price')),
        inStock: formData.get('inStock') === 'on'
    };

    try {
        const response = await fetch(`${API_BASE}/products`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(product)
        });

        if (response.ok) {
            closeModal('addProductModal');
            form.reset();
            loadProducts();
            reloadOrderMenu();
            showSuccess('Product added successfully');
        } else if (response.status === 409) {
            const error = await response.json();
            Swal.fire({
                icon: 'error',
                title: 'Product already exists',
                text: error.message || 'A product with this name already exists'
            });
        } else {
            Swal.fire({
                icon: 'error',
                title: 'Failed to add product',
                text: 'An error occurred while adding the product'
            });
        }
    } catch (error) {
        Swal.fire({
            icon: 'error',
            title: 'Error adding product',
            text: 'Failed to add product'
        });
    }
}

async function toggleStock(productId, inStock) {
    try {
        const response = await fetch(`${API_BASE}/products/${productId}/stock?inStock=${inStock}`, {
            method: 'PATCH'
        });

        if (response.ok) {
            loadProducts();
            reloadOrderMenu();
            showSuccess('Stock status updated');
        }
    } catch (error) {
        Swal.fire({
            icon: 'error',
            title: 'Error updating stock',
            text: 'Failed to update stock status'
        });
    }
}

async function deleteProduct(productId) {
    try {
        const result = await Swal.fire({
            title: 'Are you sure?',
            text: 'You want to delete this product?',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d33',
            cancelButtonColor: '#3085d6',
            confirmButtonText: 'Yes, delete it!'
        });
        if (!result.isConfirmed) return;
    } catch (e) {
        if (!confirm('Are you sure you want to delete this product?')) return;
    }

    try {
        const response = await fetch(`${API_BASE}/products/${productId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            loadProducts();
            reloadOrderMenu();
            showSuccess('Product deleted successfully');
        } else if (response.status === 409) {
            // Conflict - product has existing orders
            const error = await response.json();
            Swal.fire({
                icon: 'error',
                title: 'Cannot delete product',
                text: error.message || 'Cannot delete product with existing orders'
            });
        } else if (response.status === 404) {
            Swal.fire({
                icon: 'error',
                title: 'Product not found',
                text: 'The product you are trying to delete was not found'
            });
        } else {
            Swal.fire({
                icon: 'error',
                title: 'Failed to delete product',
                text: 'An error occurred while deleting the product'
            });
        }
    } catch (error) {
        Swal.fire({
            icon: 'error',
            title: 'Error deleting product',
            text: 'Failed to delete product'
        });
    }
}

function exportProducts() {
    window.open(`${API_BASE}/products/export/excel`, '_blank');
}

// Create Orders
async function loadCreateOrders() {
    try {
        const productsResponse = await fetch(`${API_BASE}/products/in-stock`);
        const products = await productsResponse.json();
        
        displayOrderProducts(products);
        window.ordersLoaded = true;
    } catch (error) {
        Swal.fire({
            icon: 'error',
            title: 'Error loading products',
            text: 'Failed to load products for ordering'
        });
    }
}

// Order History
async function loadOrderHistory() {
    try {
        const ordersResponse = await fetch(`${API_BASE}/orders/recent`);
        const orders = await ordersResponse.json();
        
        displayOrders(orders);
    } catch (error) {
        Swal.fire({
            icon: 'error',
            title: 'Error loading order history',
            text: 'Failed to load order history'
        });
    }
}

function displayOrderProducts(products) {
    const container = document.getElementById('orderProductsList');
    if (products.length === 0) {
        container.innerHTML = '<p>No products available</p>';
        return;
    }

    container.innerHTML = products.map(product => `
        <div class="order-product-item">
            <div class="product-info">
                <h4>${product.name}</h4>
                <p>₹${product.price.toFixed(2)} each</p>
            </div>
            <div class="quantity-controls">
                <button class="quantity-btn" onclick="changeQuantity(${product.id}, -1)">
                    <i class="fas fa-minus"></i>
                </button>
                <span class="quantity-display" id="qty-${product.id}">0</span>
                <button class="quantity-btn" onclick="changeQuantity(${product.id}, 1)">
                    <i class="fas fa-plus"></i>
                </button>
                <button class="add-order-btn" id="add-btn-${product.id}" onclick="createOrder(${product.id})" disabled>
                    Add
                </button>
            </div>
        </div>
    `).join('');
}

let quantities = {};

function reloadOrderMenu() {
    window.ordersLoaded = false;
    if (document.getElementById('create-orders').classList.contains('active')) {
        loadCreateOrders();
    }
}

function navigateToOrderHistory() {
    showSection('order-history');
    document.querySelectorAll('.nav-menu a').forEach(l => l.classList.remove('active'));
    document.querySelector('.nav-menu a[href="#order-history"]').classList.add('active');
    loadOrderHistory();
}

function navigateToProducts() {
    showSection('products');
    document.querySelectorAll('.nav-menu a').forEach(l => l.classList.remove('active'));
    document.querySelector('.nav-menu a[href="#products"]').classList.add('active');
    loadProducts();
}

async function navigateToInStockProducts() {
    showSection('products');
    document.querySelectorAll('.nav-menu a').forEach(l => l.classList.remove('active'));
    document.querySelector('.nav-menu a[href="#products"]').classList.add('active');
    
    // Set filter to in stock and load filtered products
    document.getElementById('stockFilter').value = 'inStock';
    const response = await fetch(`${API_BASE}/products/in-stock`);
    const products = await response.json();
    displayProducts(products);
}

function navigateToDashboard() {
    showSection('dashboard');
    document.querySelectorAll('.nav-menu a').forEach(l => l.classList.remove('active'));
    document.querySelector('.nav-menu a[href="#dashboard"]').classList.add('active');
    loadDashboard();
}

function navigateToCreateOrders() {
    showSection('create-orders');
    document.querySelectorAll('.nav-menu a').forEach(l => l.classList.remove('active'));
    document.querySelector('.nav-menu a[href="#create-orders"]').classList.add('active');
    if (!window.ordersLoaded) loadCreateOrders();
}

function navigateToTodaysReport() {
    const today = new Date().toISOString().split('T')[0];
    showSection('reports');
    document.querySelectorAll('.nav-menu a').forEach(l => l.classList.remove('active'));
    document.querySelector('.nav-menu a[href="#reports"]').classList.add('active');
    
    // Set today's date in both fields
    document.getElementById('reportStartDate').value = today;
    document.getElementById('reportEndDate').value = today;
    
    // Generate the report automatically
    getRevenueReport();
}

function changeQuantity(productId, change) {
    if (!quantities[productId]) quantities[productId] = 0;
    quantities[productId] = Math.max(0, quantities[productId] + change);
    
    document.getElementById(`qty-${productId}`).textContent = quantities[productId];
    document.getElementById(`add-btn-${productId}`).disabled = quantities[productId] === 0;
}

async function createOrder(productId) {
    const quantity = quantities[productId];
    if (!quantity || quantity <= 0) return;

    try {
        const response = await fetch(`${API_BASE}/orders/create`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ productId, quantity })
        });

        if (response.ok) {
            quantities[productId] = 0;
            document.getElementById(`qty-${productId}`).textContent = '0';
            document.getElementById(`add-btn-${productId}`).disabled = true;
            loadOrderHistory();
            loadDashboard();
            showSuccess('Order created successfully');
        }
    } catch (error) {
        Swal.fire({
            icon: 'error',
            title: 'Error creating order',
            text: 'Failed to create order'
        });
    }
}

// Helper function to check if order can be deleted (within 1 hour)
function isOrderDeletable(orderDateTime) {
    const orderTime = new Date(orderDateTime);
    const oneHourAgo = new Date(Date.now() - 60 * 60 * 1000);
    // Return true if order is within 1 hour (deletable)
    // Return false if order is older than 1 hour (not deletable)
    return orderTime > oneHourAgo;
}

function displayOrders(orders) {
    const container = document.getElementById('ordersList');
    if (orders.length === 0) {
        container.innerHTML = '<p>No orders found</p>';
        return;
    }

    container.innerHTML = `
        <table>
            <thead>
                <tr>
                    <th>Product</th>
                    <th>Quantity</th>
                    <th>Total Cost</th>
                    <th>Date</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                ${orders.map(order => {
                    const isDeletable = isOrderDeletable(order.orderDateTime);
                    return `
                        <tr>
                            <td>${order.product.name}</td>
                            <td>${order.quantity}</td>
                            <td>₹${order.totalCost.toFixed(2)}</td>
                            <td>${formatDate(order.orderDateTime)}</td>
                            <td>
                                <button class="btn btn-danger" 
                                        onclick="deleteOrder('${order.orderId}', '${order.orderDateTime}')" 
                                        ${isDeletable ? '' : 'disabled title="Cannot delete orders older than 1 hour"'}>
                                    <i class="fas fa-trash"></i>
                                </button>
                            </td>
                        </tr>
                    `;
                }).join('')}
            </tbody>
        </table>
    `;
}





async function deleteOrder(orderId, orderDateTime) {
    // Client-side validation - check if order is within 1 hour
    if (!isOrderDeletable(orderDateTime)) {
        const orderTime = new Date(orderDateTime);
        const hoursAgo = Math.floor((Date.now() - orderTime.getTime()) / (1000 * 60 * 60));
        
        Swal.fire({
            icon: 'info',
            title: 'Order Deletion Restricted',
            html: `This order was placed more than 1 hour ago and can no longer be deleted.<br><br>
                   <strong>Order placed:</strong> ${formatDate(orderDateTime)}<br>
                   <strong>Time elapsed:</strong> ${hoursAgo} hour(s) ago<br><br>
                   <small>Orders can only be deleted within 1 hour of creation.</small>`,
            confirmButtonText: 'Understood',
            confirmButtonColor: '#3085d6'
        });
        return;
    }

    try {
        const result = await Swal.fire({
            title: 'Are you sure?',
            text: 'You want to delete this order?',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d33',
            cancelButtonColor: '#3085d6',
            confirmButtonText: 'Yes, delete it!'
        });
        if (!result.isConfirmed) return;
    } catch (e) {
        if (!confirm('Are you sure you want to delete this order?')) return;
    }

    try {
        const response = await fetch(`${API_BASE}/orders/${orderId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            loadOrderHistory();
            loadDashboard();
            showSuccess('Order deleted successfully');
        } else if (response.status === 403) {
            const error = await response.json();
            Swal.fire({
                icon: 'error',
                title: 'Cannot Delete Order',
                text: error.error || 'Orders can only be deleted within 1 hour of creation'
            });
        } else {
            Swal.fire({
                icon: 'error',
                title: 'Failed to delete order',
                text: 'Could not delete the order'
            });
        }
    } catch (error) {
        Swal.fire({
            icon: 'error',
            title: 'Error deleting order',
            text: 'Failed to delete order'
        });
    }
}

async function filterOrders() {
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;

    if (!startDate || !endDate) {
        Swal.fire({
            icon: 'warning',
            title: 'Missing dates',
            text: 'Please select both start and end dates'
        });
        return;
    }

    try {
        const url = `${API_BASE}/orders/between?start=${startDate}T00:00:00&end=${endDate}T23:59:59`;
        const response = await fetch(url);
        const orders = await response.json();
        displayOrders(orders);
    } catch (error) {
        Swal.fire({
            icon: 'error',
            title: 'Error filtering orders',
            text: 'Failed to filter orders'
        });
    }
}

function exportOrders() {
    window.open(`${API_BASE}/orders/export/excel`, '_blank');
}

// Reports
async function getRevenueReport() {
    const startDate = document.getElementById('reportStartDate').value;
    const endDate = document.getElementById('reportEndDate').value;

    if (!startDate || !endDate) {
        Swal.fire({
            icon: 'warning',
            title: 'Missing dates',
            text: 'Please select both start and end dates'
        });
        return;
    }

    try {
        const url = `${API_BASE}/orders/revenue/between?start=${startDate}T00:00:00&end=${endDate}T23:59:59`;
        const response = await fetch(url);
        const data = await response.json();
        
        // Also fetch orders for product breakdown
        const ordersUrl = `${API_BASE}/orders/between?start=${startDate}T00:00:00&end=${endDate}T23:59:59`;
        const ordersResponse = await fetch(ordersUrl);
        const orders = await ordersResponse.json();
        
        // Calculate product counts
        const productCounts = {};
        orders.forEach(order => {
            const productName = order.product.name;
            if (productCounts[productName]) {
                productCounts[productName] += order.quantity;
            } else {
                productCounts[productName] = order.quantity;
            }
        });
        
        const productBreakdown = Object.entries(productCounts)
            .map(([name, count]) => `<li>${name}: ${count} units</li>`)
            .join('');
        
        document.getElementById('revenueResult').innerHTML = `
            <h4>Revenue Report</h4>
            <p><strong>Period:</strong> ${startDate} to ${endDate}</p>
            <p><strong>Total Revenue:</strong> <span style="color: var(--success-color); font-size: 1.5rem;">₹${data.totalRevenue.toFixed(2)}</span></p>
            <h5 style="margin-top: 1rem;">Products Sold:</h5>
            <ul style="margin-top: 0.5rem;">${productBreakdown || '<li>No products sold in this period</li>'}</ul>
        `;
    } catch (error) {
        Swal.fire({
            icon: 'error',
            title: 'Error getting revenue report',
            text: 'Failed to generate revenue report'
        });
    }
}

async function getHighValueOrders() {
    const minCost = document.getElementById('minCost').value;

    if (!minCost) {
        Swal.fire({
            icon: 'warning',
            title: 'Missing minimum cost',
            text: 'Please enter a minimum cost'
        });
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/orders/high-value?minCost=${minCost}`);
        const orders = await response.json();
        
        if (orders.length === 0) {
            document.getElementById('highValueResult').innerHTML = '<p>No orders found above this amount</p>';
            return;
        }

        document.getElementById('highValueResult').innerHTML = `
            <h4>High Value Orders (>${minCost})</h4>
            <ul style="list-style: none; padding: 0;">
                ${orders.map(order => `
                    <li style="padding: 0.5rem; border-bottom: 1px solid var(--border-color);">
                        <strong>${order.product.name}</strong> - 
                        Qty: ${order.quantity} - 
                        <span style="color: var(--success-color);">₹${order.totalCost.toFixed(2)}</span>
                    </li>
                `).join('')}
            </ul>
        `;
    } catch (error) {
        Swal.fire({
            icon: 'error',
            title: 'Error getting high value orders',
            text: 'Failed to get high value orders'
        });
    }
}

// Modal Functions
function showAddProductModal() {
    document.getElementById('addProductModal').classList.add('active');
}



function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('active');
}

// Utility Functions
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
}

function showSuccess(message) {
    Swal.fire({
        icon: 'success',
        text: message,
        toast: true,
        position: 'top-end',
        timer: 500,
        showConfirmButton: false,
        timerProgressBar: true
    });
}

function showError(message) {
    Swal.fire({
        icon: 'error',
        title: 'Error!',
        text: message
    });
}

// Search and Filter
document.getElementById('productSearch')?.addEventListener('input', async (e) => {
    const searchTerm = e.target.value.toLowerCase();
    const response = await fetch(`${API_BASE}/products`);
    const products = await response.json();
    const filtered = products.filter(p => p.name.toLowerCase().includes(searchTerm));
    displayProducts(filtered);
});

document.getElementById('stockFilter')?.addEventListener('change', async (e) => {
    const filter = e.target.value;
    let url = `${API_BASE}/products`;
    
    if (filter === 'inStock') {
        url += '/in-stock';
    }
    
    const response = await fetch(url);
    let products = await response.json();
    
    if (filter === 'outOfStock') {
        products = products.filter(p => !p.inStock);
    }
    
    displayProducts(products);
});
