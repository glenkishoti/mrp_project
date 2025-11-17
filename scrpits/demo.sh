#!/usr/bin/env bash

BASE="http://localhost:8080"
USERS="$BASE/api/users"

echo "=== Register ==="
curl -i -X POST "$USERS/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"secret"}'

echo
echo "=== Login ==="
LOGIN_RESPONSE=$(curl -s -X POST "$USERS/login" \
  -H "Content-Type: application/json" \
  -d '{"Username":"alice","Password":"secret"}')

echo "Login Response: $LOGIN_RESPONSE"

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d '"' -f4)

echo
echo "Extracted Token: $TOKEN"

echo
echo "=== Profile (GET) ==="
curl -i -X GET "$USERS/alice/profile" \
  -H "Authorization: Bearer $TOKEN"

echo
