#!/bin/bash
set -e

# Register test user
echo "=== REGISTER ==="
curl -s -X POST http://localhost:3000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@smartdd.com","password":"test123456","name":"Test User"}' || true

echo ""
echo "=== LOGIN ==="
LOGIN=$(curl -s -X POST http://localhost:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@smartdd.com","password":"test123456"}')
echo "$LOGIN"

TOKEN=$(echo "$LOGIN" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "=== TOKEN: $TOKEN ==="

echo "=== GENERATE QR ==="
QR_RESP=$(curl -s -X POST http://localhost:3000/api/v1/qr \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"lat":19.4326,"lng":-99.1332,"radius":50}')
echo "$QR_RESP"
