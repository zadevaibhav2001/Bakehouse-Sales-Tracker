#!/bin/bash

# Populate The Bakehouse Database with New Product List
API_BASE="http://ec2-3-110-159-51.ap-south-1.compute.amazonaws.com/api"

echo "🍞 Populating The Bakehouse Database with NEW items..."
echo ""

# Create Products
echo "📦 Creating Products..."

# 1. Korean Buns - ₹100
curl -X POST "$API_BASE/products" \
  -H "Content-Type: application/json" \
  -d '{"name":"Korean Buns","price":100,"inStock":true}' && echo ""

# 2. Cheesecake Slice - ₹150
curl -X POST "$API_BASE/products" \
  -H "Content-Type: application/json" \
  -d '{"name":"Cheesecake Slice","price":150,"inStock":true}' && echo ""

# 3. Brownies - ₹150
curl -X POST "$API_BASE/products" \
  -H "Content-Type: application/json" \
  -d '{"name":"Brownies","price":150,"inStock":true}' && echo ""

# 4. Pastry - ₹100
curl -X POST "$API_BASE/products" \
  -H "Content-Type: application/json" \
  -d '{"name":"Pastry","price":100,"inStock":true}' && echo ""

# 5. Cake Pops - ₹50
curl -X POST "$API_BASE/products" \
  -H "Content-Type: application/json" \
  -d '{"name":"Cake Pops","price":50,"inStock":true}' && echo ""

# 6. Nankhatai - ₹150
curl -X POST "$API_BASE/products" \
  -H "Content-Type: application/json" \
  -d '{"name":"Nankhatai","price":150,"inStock":true}' && echo ""

# 7. Chocolate Chip Cookie (250 gm) - ₹300
curl -X POST "$API_BASE/products" \
  -H "Content-Type: application/json" \
  -d '{"name":"Chocolate Chip Cookie (250gm)","price":300,"inStock":true}' && echo ""

# 8. Chocolate Chip Cookie (per piece) - ₹50
curl -X POST "$API_BASE/products" \
  -H "Content-Type: application/json" \
  -d '{"name":"Chocolate Chip Cookie (Piece)","price":50,"inStock":true}' && echo ""

# 9. Jars - ₹150
curl -X POST "$API_BASE/products" \
  -H "Content-Type: application/json" \
  -d '{"name":"Jars","price":150,"inStock":true}' && echo ""

# 10. Oreo Jar - ₹100
curl -X POST "$API_BASE/products" \
  -H "Content-Type: application/json" \
  -d '{"name":"Oreo Jar","price":100,"inStock":true}' && echo ""

echo ""
echo "✅ Products created!"
echo ""
sleep 2

# ---------------------------------------
# Get Product IDs and Create Sample Orders
# ---------------------------------------

echo "🛒 Creating Orders..."

# Get all products and extract IDs
PRODUCTS_JSON=$(curl -s "$API_BASE/products")

# Extract product IDs using basic parsing
PRODUCT_IDS=($(echo "$PRODUCTS_JSON" | grep -o '"id":[0-9]*' | cut -d':' -f2))

echo "Found ${#PRODUCT_IDS[@]} products with IDs: ${PRODUCT_IDS[@]}"

# Create orders using actual product IDs
if [ ${#PRODUCT_IDS[@]} -ge 1 ]; then
    # Order 1: First product × 5
    curl -X POST "$API_BASE/orders/create" \
      -H "Content-Type: application/json" \
      -d "{\"productId\":${PRODUCT_IDS[0]},\"quantity\":5}" && echo ""
fi


if [ ${#PRODUCT_IDS[@]} -ge 2 ]; then
    sleep 1
    curl -X POST "$API_BASE/orders/create" \
      -H "Content-Type: application/json" \
      -d "{\"productId\":${PRODUCT_IDS[1]},\"quantity\":3}" && echo ""
fi

if [ ${#PRODUCT_IDS[@]} -ge 3 ]; then
    sleep 1
    curl -X POST "$API_BASE/orders/create" \
      -H "Content-Type: application/json" \
      -d "{\"productId\":${PRODUCT_IDS[2]},\"quantity\":4}" && echo ""
fi

if [ ${#PRODUCT_IDS[@]} -ge 4 ]; then
    sleep 1
    curl -X POST "$API_BASE/orders/create" \
      -H "Content-Type: application/json" \
      -d "{\"productId\":${PRODUCT_IDS[3]},\"quantity\":6}" && echo ""
fi

if [ ${#PRODUCT_IDS[@]} -ge 5 ]; then
    sleep 1
    curl -X POST "$API_BASE/orders/create" \
      -H "Content-Type: application/json" \
      -d "{\"productId\":${PRODUCT_IDS[4]},\"quantity\":10}" && echo ""
fi

echo ""
echo "✅ Orders created!"
echo ""

# ---------------------------------------
# Summary
# ---------------------------------------

echo "📊 Database Summary:"
echo "===================="
curl -s "$API_BASE/products" | grep -o '"name"' | wc -l | xargs echo "Products:"
curl -s "$API_BASE/orders" | grep -o '"orderId"' | wc -l | xargs echo "Orders:"
echo ""

echo "💰 Total Revenue:"
curl -s "$API_BASE/orders/revenue/total" | grep -o '"totalRevenue":[0-9.]*' | cut -d':' -f2

echo ""
echo "✨ Database populated successfully!"
echo "🌐 Open http://localhost:3000 to view the website"
