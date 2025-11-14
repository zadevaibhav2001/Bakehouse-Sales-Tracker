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
    loadOrders();
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
    
    // Reload data when switching sections
    if (sectionId === 'dashboard') loadDashboard();
    if (sectionId === 'products') loadProducts();
    if (sectionId === 'orders') loadOrders();
}

// Dashboard
async function loadDashboard() {
    try {
        const [orders, revenue, products, inStock] = await Promise.all([
            fetch(`${API_BASE}/orders`).then(r => r.json()),
            fetch(`${API_BASE}/orders/revenue/total`).then(r => r.json()),
            fetch(`${API_BASE}/products`).then(r => r.json()),
            fetch(`${API_BASE}/products/in-stock`).then(r => r.json())
        ]);

        document.getElementById('totalOrders').textContent = orders.length;
        document.getElementById('totalRevenue').textContent = `₹${revenue.totalRevenue.toFixed(2)}`;
        document.getElementById('totalProducts').textContent = products.length;
        document.getElementById('inStockProducts').textContent = inStock.length;

        displayRecentOrders(orders.slice(0, 5));
    } catch (error) {
        console.error('Error loading dashboard:', error);
        showError('Failed to load dashboard data');
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
        console.error('Error loading products:', error);
        showError('Failed to load products');
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
            showSuccess('Product added successfully');
        }
    } catch (error) {
        console.error('Error adding product:', error);
        showError('Failed to add product');
    }
}

async function toggleStock(productId, inStock) {
    try {
        const response = await fetch(`${API_BASE}/products/${productId}/stock?inStock=${inStock}`, {
            method: 'PATCH'
        });

        if (response.ok) {
            loadProducts();
            showSuccess('Stock status updated');
        }
    } catch (error) {
        console.error('Error updating stock:', error);
        showError('Failed to update stock status');
    }
}

async function deleteProduct(productId) {
    if (!confirm('Are you sure you want to delete this product?')) return;

    try {
        const response = await fetch(`${API_BASE}/products/${productId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            loadProducts();
            showSuccess('Product deleted successfully');
        } else if (response.status === 409) {
            // Conflict - product has existing orders
            const error = await response.json();
            showError(error.message || 'Cannot delete product with existing orders');
        } else if (response.status === 404) {
            showError('Product not found');
        } else {
            showError('Failed to delete product');
        }
    } catch (error) {
        console.error('Error deleting product:', error);
        showError('Failed to delete product');
    }
}

function exportProducts() {
    window.open(`${API_BASE}/products/export/excel`, '_blank');
}

// Orders
async function loadOrders() {
    try {
        const response = await fetch(`${API_BASE}/orders/recent`);
        const orders = await response.json();
        displayOrders(orders);
        
        // Load products for order form
        const productsResponse = await fetch(`${API_BASE}/products/in-stock`);
        const products = await productsResponse.json();
        populateProductSelect(products);
    } catch (error) {
        console.error('Error loading orders:', error);
        showError('Failed to load orders');
    }
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
                ${orders.map(order => `
                    <tr>
                        <td>${order.product.name}</td>
                        <td>${order.quantity}</td>
                        <td>₹${order.totalCost.toFixed(2)}</td>
                        <td>${formatDate(order.orderDateTime)}</td>
                        <td>
                            <button class="btn btn-danger" onclick="deleteOrder('${order.orderId}')">
                                <i class="fas fa-trash"></i>
                            </button>
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

function populateProductSelect(products) {
    const select = document.getElementById('orderProductSelect');
    select.innerHTML = '<option value="">Select a product</option>' +
        products.map(p => `<option value="${p.id}">${p.name} - ₹${p.price.toFixed(2)}</option>`).join('');
}

async function addOrder(event) {
    event.preventDefault();
    const form = event.target;
    const formData = new FormData(form);
    
    const order = {
        productId: parseInt(formData.get('productId')),
        quantity: parseInt(formData.get('quantity'))
    };

    try {
        const response = await fetch(`${API_BASE}/orders/create`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(order)
        });

        if (response.ok) {
            closeModal('addOrderModal');
            form.reset();
            loadOrders();
            loadDashboard();
            showSuccess('Order created successfully');
        }
    } catch (error) {
        console.error('Error creating order:', error);
        showError('Failed to create order');
    }
}

async function deleteOrder(orderId) {
    if (!confirm('Are you sure you want to delete this order?')) return;

    try {
        const response = await fetch(`${API_BASE}/orders/${orderId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            loadOrders();
            loadDashboard();
            showSuccess('Order deleted successfully');
        }
    } catch (error) {
        console.error('Error deleting order:', error);
        showError('Failed to delete order');
    }
}

async function filterOrders() {
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;

    if (!startDate || !endDate) {
        showError('Please select both start and end dates');
        return;
    }

    try {
        const url = `${API_BASE}/orders/between?start=${startDate}T00:00:00&end=${endDate}T23:59:59`;
        const response = await fetch(url);
        const orders = await response.json();
        displayOrders(orders);
    } catch (error) {
        console.error('Error filtering orders:', error);
        showError('Failed to filter orders');
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
        showError('Please select both start and end dates');
        return;
    }

    try {
        const url = `${API_BASE}/orders/revenue/between?start=${startDate}T00:00:00&end=${endDate}T23:59:59`;
        const response = await fetch(url);
        const data = await response.json();
        
        document.getElementById('revenueResult').innerHTML = `
            <h4>Revenue Report</h4>
            <p><strong>Period:</strong> ${startDate} to ${endDate}</p>
            <p><strong>Total Revenue:</strong> <span style="color: var(--success-color); font-size: 1.5rem;">₹${data.totalRevenue.toFixed(2)}</span></p>
        `;
    } catch (error) {
        console.error('Error getting revenue report:', error);
        showError('Failed to generate revenue report');
    }
}

async function getHighValueOrders() {
    const minCost = document.getElementById('minCost').value;

    if (!minCost) {
        showError('Please enter a minimum cost');
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
        console.error('Error getting high value orders:', error);
        showError('Failed to get high value orders');
    }
}

// Modal Functions
function showAddProductModal() {
    document.getElementById('addProductModal').classList.add('active');
}

function showAddOrderModal() {
    document.getElementById('addOrderModal').classList.add('active');
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
    alert(message); // Replace with a better notification system
}

function showError(message) {
    alert('Error: ' + message); // Replace with a better notification system
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
