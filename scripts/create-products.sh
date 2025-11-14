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