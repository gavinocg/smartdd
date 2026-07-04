#!/bin/bash
echo "=== Health ==="
curl -s http://localhost:3000/api/v1/health
echo ""

echo "=== Login as admin ==="
RESP=$(curl -s http://localhost:3000/api/v1/auth/login -X POST -H "Content-Type: application/json" -d '{"email":"admin@smartdd.com","password":"admin123456"}')
echo "$RESP"
TOKEN=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('token','NO_TOKEN'))" 2>/dev/null)
echo "Token: ${TOKEN:0:30}..."

echo ""
echo "=== Create QR ==="
curl -s http://localhost:3000/api/v1/qr -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d '{"lat":-23.5505,"lng":-46.6333,"radiusMeters":50}'
echo ""
