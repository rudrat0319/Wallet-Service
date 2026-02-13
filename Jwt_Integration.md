# JWT Authentication Integration Guide

## Overview

This wallet service is designed to integrate seamlessly with any application using JWT authentication. **The service does not manage user passwords or issue tokens** - it only validates JWT tokens issued by your parent application.

---

## 🔑 Authentication Model

### What This Service Does:
✅ Validates JWT tokens from Authorization header  
✅ Extracts user ID from JWT claims  
✅ Associates wallet operations with authenticated users  

### What This Service Does NOT Do:
❌ Store user passwords  
❌ Issue JWT tokens  
❌ Handle user login/logout  
❌ Manage user sessions  

**Your parent application** handles all authentication and issues JWT tokens. This service simply validates them.

---

## 🚀 Quick Start

### Development Mode (Testing)

For testing without JWT:

```yaml
# application.yml
jwt:
  enabled: false  # Use X-User-Id header instead of JWT
```

```bash
# Test with X-User-Id header
curl -X GET "http://localhost:8080/api/v1/wallets/balance?assetType=GOLD_COINS" \
  -H "X-User-Id: 1"
```

### Production Mode (JWT)

```yaml
# application.yml
jwt:
  enabled: true
  secret: ${JWT_SECRET}  # Set via environment variable
```

```bash
# Test with JWT token
curl -X GET "http://localhost:8080/api/v1/wallets/balance?assetType=GOLD_COINS" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 📋 JWT Requirements

### Required Claims

Your JWT token **must** include:

```json
{
  "userId": 1,           // Required: User ID (Long)
  "email": "user@example.com",  // Optional: For logging
  "sub": "user@example.com",    // Standard: Subject
  "iat": 1707825600,     // Standard: Issued at
  "exp": 1707912000      // Standard: Expiration
}
```

### Supported Claim Names for User ID

The service accepts any of these claim names (in order of preference):
- `userId` (recommended)
- `user_id` (snake_case alternative)
- `id` (fallback)

### JWT Format

```
Authorization: Bearer <token>
```

Example:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsImVtYWlsIjoidXNlckBleGFtcGxlLmNvbSIsInN1YiI6InVzZXJAZXhhbXBsZS5jb20iLCJpYXQiOjE3MDc4MjU2MDAsImV4cCI6MTcwNzkxMjAwMH0.signature
```

---

## 🔐 Configuration

### Environment Variables

```bash
# Required in production
export JWT_SECRET="your-256-bit-secret-key-here"
export JWT_ENABLED=true

# Optional
export JWT_EXPIRATION_HOURS=24  # For documentation only
```

### application.yml

```yaml
jwt:
  # Secret key for JWT validation
  # MUST be at least 256 bits (32 characters)
  # Generate: openssl rand -base64 32
  secret: ${JWT_SECRET:your-secret-key-change-this-in-production-must-be-at-least-256-bits-long}
  
  # Enable/disable JWT authentication
  # false = use X-User-Id header (development)
  # true = use JWT tokens (production)
  enabled: ${JWT_ENABLED:false}
  
  # Token expiration (documentation only)
  # This service validates tokens but doesn't issue them
  expiration-hours: 24
```

### Docker Deployment

```yaml
# docker-compose.yml
services:
  wallet-service:
    environment:
      JWT_SECRET: "your-production-secret-key-min-256-bits"
      JWT_ENABLED: "true"
```

---

## 🔧 Integration Examples

### Example 1: Node.js Parent Application

**Issuing tokens in your app:**

```javascript
// server.js (Your parent application)
const jwt = require('jsonwebtoken');

app.post('/login', async (req, res) => {
  // Your authentication logic
  const user = await authenticateUser(req.body);
  
  // Issue JWT token
  const token = jwt.sign(
    {
      userId: user.id,        // Required by wallet service
      email: user.email,
      sub: user.email
    },
    process.env.JWT_SECRET,   // Same secret as wallet service
    { expiresIn: '24h' }
  );
  
  res.json({ token });
});

// Call wallet service
app.get('/my-wallet', async (req, res) => {
  const token = req.headers.authorization;
  
  const response = await fetch('http://wallet-service:8080/api/v1/wallets/balance?assetType=GOLD_COINS', {
    headers: {
      'Authorization': token  // Forward the same token
    }
  });
  
  const balance = await response.json();
  res.json(balance);
});
```

### Example 2: Python/Flask Parent Application

```python
# app.py (Your parent application)
import jwt
import requests
from flask import Flask, request, jsonify

app = Flask(__name__)
JWT_SECRET = os.getenv('JWT_SECRET')
WALLET_SERVICE_URL = 'http://wallet-service:8080/api/v1'

@app.route('/login', methods=['POST'])
def login():
    # Your authentication logic
    user = authenticate_user(request.json)
    
    # Issue JWT token
    token = jwt.encode(
        {
            'userId': user.id,      # Required by wallet service
            'email': user.email,
            'sub': user.email,
            'exp': datetime.utcnow() + timedelta(hours=24)
        },
        JWT_SECRET,
        algorithm='HS256'
    )
    
    return jsonify({'token': token})

@app.route('/wallet/balance')
def get_wallet_balance():
    # Forward JWT token to wallet service
    auth_header = request.headers.get('Authorization')
    
    response = requests.get(
        f'{WALLET_SERVICE_URL}/wallets/balance',
        params={'assetType': 'GOLD_COINS'},
        headers={'Authorization': auth_header}
    )
    
    return jsonify(response.json())
```

### Example 3: Java/Spring Boot Parent Application

```java
// AuthController.java (Your parent application)
@RestController
public class AuthController {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // Your authentication logic
        User user = authenticationService.authenticate(request);
        
        // Issue JWT token with userId claim
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        
        String token = jwtUtil.generateToken(user.getEmail(), claims);
        
        return ResponseEntity.ok(new TokenResponse(token));
    }
}

// WalletProxyController.java
@RestController
@RequestMapping("/api/wallet")
public class WalletProxyController {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String assetType) {
        
        // Forward request to wallet service
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ResponseEntity<BalanceResponse> response = restTemplate.exchange(
            "http://wallet-service:8080/api/v1/wallets/balance?assetType=" + assetType,
            HttpMethod.GET,
            entity,
            BalanceResponse.class
        );
        
        return ResponseEntity.ok(response.getBody());
    }
}
```

---

## 🧪 Testing

### Generate Test JWT Token

**Using jwt.io:**
1. Go to https://jwt.io
2. Select algorithm: HS256
3. Payload:
```json
{
  "userId": 1,
  "email": "john.doe@example.com",
  "sub": "john.doe@example.com",
  "iat": 1707825600,
  "exp": 2707912000
}
```
4. Secret: `your-secret-key-change-this-in-production-must-be-at-least-256-bits-long`
5. Copy the generated token

**Using command line:**

```bash
# Install jwt-cli: cargo install jwt-cli

# Generate token
jwt encode \
  --secret "your-secret-key-change-this-in-production-must-be-at-least-256-bits-long" \
  --exp "+24h" \
  '{"userId": 1, "email": "john.doe@example.com", "sub": "john.doe@example.com"}'
```

**Test the token:**

```bash
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -X GET "http://localhost:8080/api/v1/wallets/balance?assetType=GOLD_COINS" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🔍 Error Handling

### Common Errors

**1. Token Expired**
```json
{
  "error": "Unauthorized",
  "message": "Token expired"
}
```
**Solution:** Request new token from parent application

**2. Invalid Token**
```json
{
  "error": "Unauthorized", 
  "message": "Invalid token"
}
```
**Solution:** Check token format and signature

**3. Missing userId Claim**
```json
{
  "error": "Internal Server Error",
  "message": "JWT token must contain userId claim"
}
```
**Solution:** Ensure token includes `userId`, `user_id`, or `id` claim

**4. Secret Mismatch**
- Symptom: "Invalid token" error
- **Solution:** Ensure JWT_SECRET is identical in both applications

---

## 🛡️ Security Best Practices

### 1. Secret Key Management

**❌ Don't:**
```yaml
jwt:
  secret: "my-secret"  # Too short, hardcoded
```

**✅ Do:**
```bash
# Generate strong secret
openssl rand -base64 32

# Set via environment variable
export JWT_SECRET="generated-secret-here"
```

### 2. Token Storage (Client-side)

**❌ Don't:** Store in localStorage (XSS vulnerable)  
**✅ Do:** Store in httpOnly cookies

### 3. Token Expiration

**❌ Don't:** Issue tokens with no expiration  
**✅ Do:** Set reasonable expiration (15-60 minutes)  
**✅ Do:** Implement refresh tokens for longer sessions

### 4. HTTPS Only

**❌ Don't:** Send tokens over HTTP  
**✅ Do:** Always use HTTPS in production

---

## 📊 Token Flow Diagram

```
┌─────────────┐
│   Client    │
│  (Browser)  │
└──────┬──────┘
       │ 1. Login (username/password)
       ▼
┌─────────────────────┐
│  Parent Application │
│  (Your Auth Service)│
└──────┬──────────────┘
       │ 2. Issue JWT token
       │    {userId: 1, email: "user@example.com", exp: ...}
       ▼
┌─────────────┐
│   Client    │
│ Stores JWT  │
└──────┬──────┘
       │ 3. API Request with JWT
       │    Authorization: Bearer <token>
       ▼
┌─────────────────────┐
│  Wallet Service     │
│ (This Service)      │
│                     │
│ 1. Validate token   │
│ 2. Extract userId   │
│ 3. Process request  │
│ 4. Return response  │
└─────────────────────┘
```

---

## 🔄 Migration from Legacy Auth

If migrating from a system with passwords:

1. **Keep existing user IDs**
```java
// Map external user ID to wallet service user ID
user.setExternalId(legacySystem.getUserId());
```

2. **Gradual rollout**
```yaml
# Phase 1: Support both
jwt:
  enabled: false  # Allow X-User-Id for testing

# Phase 2: JWT only
jwt:
  enabled: true   # Enforce JWT validation
```

3. **No data migration needed**
- User passwords are never stored in wallet service
- Existing wallet data remains unchanged



## 🎯 Summary

✅ **No password storage** - This is a wallet service, not an auth service  
✅ **JWT validation only** - Your app issues tokens, we validate them  
✅ **Flexible integration** - Works with any JWT-capable system  
✅ **Development mode** - Test with X-User-Id header  
✅ **Production ready** - Full JWT validation with proper error handling  

**Your authentication, our wallets. Simple integration.** 🚀
