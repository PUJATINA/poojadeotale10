# E-Commerce Frontend (React + Vite)

This project contains the frontend for a Flipkart-style e-commerce app with:

- Login page
- Registration page
- Session-based authentication flow (no JWT in frontend)
- Product listing page
- API integration points for Java Spring Boot backend

## Run

```bash
npm install
npm run dev
```

Frontend runs at `http://localhost:5173` by default.

## Environment

Create `.env` (or use `.env.local`) with:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

If not set, it defaults to `http://localhost:8080/api`.

## Spring Boot API contracts expected

The frontend calls these endpoints:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /api/products`

Important:

- Frontend sends requests using `credentials: "include"` for cookie/session auth.
- Backend should enable CORS with credentials for `http://localhost:5173`.
- Return user object for login/register/me, for example:

```json
{
  "id": 1,
  "name": "Demo User",
  "email": "demo@example.com"
}
```

- Return product list for `/products`, for example:

```json
[
  {
    "id": 1,
    "name": "Running Shoes",
    "description": "Lightweight and comfortable",
    "price": 2499,
    "imageUrl": "https://example.com/shoe.jpg"
  }
]
```
