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
# Create Sample Orders (using new product IDs 1–10)
# ---------------------------------------

echo "🛒 Creating Orders..."

# Order 1: Korean Buns × 5
curl -X POST "$API_BASE/orders/create" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":5}' && echo ""

sleep 1

# Order 2: Cheesecake Slice × 3
curl -X POST "$API_BASE/orders/create" \
  -H "Content-Type: application/json" \
  -d '{"productId":2,"quantity":3}' && echo ""

sleep 1

# Order 3: Brownies × 4
curl -X POST "$API_BASE/orders/create" \
  -H "Content-Type: application/json" \
  -d '{"productId":3,"quantity":4}' && echo ""

sleep 1

# Order 4: Pastry × 6
curl -X POST "$API_BASE/orders/create" \
  -H "Content-Type: application/json" \
  -d '{"productId":4,"quantity":6}' && echo ""

sleep 1

# Order 5: Cake Pops × 10
curl -X POST "$API_BASE/orders/create" \
  -H "Content-Type: application/json" \
  -d '{"productId":5,"quantity":10}' && echo ""

sleep 1

# Order 6: Nankhatai × 8
curl -X POST "$API_BASE/orders/create" \
  -H "Content-Type: application/json" \
  -d '{"productId":6,"quantity":8}' && echo ""

sleep 1

# Order 7: Chocolate Chip Cookie 250gm × 2
curl -X POST "$API_BASE/orders/create" \
  -H "Content-Type: application/json" \
  -d '{"productId":7,"quantity":2}' && echo ""

sleep 1

# Order 8: Chocolate Chip Cookie (piece) × 12
curl -X POST "$API_BASE/orders/create" \
  -H "Content-Type: application/json" \
  -d '{"productId":8,"quantity":12}' && echo ""

sleep 1

# Order 9: Jars × 5
curl -X POST "$API_BASE/orders/create" \
  -H "Content-Type: application/json" \
  -d '{"productId":9,"quantity":5}' && echo ""

sleep 1

# Order 10: Oreo Jar × 7
curl -X POST "$API_BASE/orders/create" \
  -H "Content-Type: application/json" \
  -d '{"productId":10,"quantity":7}' && echo ""

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
