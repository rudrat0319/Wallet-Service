#!/bin/bash

echo "==================================================="
echo "  Wallet Service Setup Script"
echo "==================================================="
echo ""

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

echo "✅ Docker and Docker Compose are installed"
echo ""

# Build and start services
echo "🏗️  Building Docker images..."
docker-compose build

echo ""
echo "🚀 Starting services..."
docker-compose up -d

echo ""
echo "⏳ Waiting for services to be healthy..."
sleep 10

# Check if services are running
if docker-compose ps | grep -q "wallet-service"; then
    echo "✅ Wallet Service is running"
else
    echo "❌ Wallet Service failed to start"
    docker-compose logs wallet-service
    exit 1
fi

if docker-compose ps | grep -q "postgres"; then
    echo "✅ PostgreSQL is running"
else
    echo "❌ PostgreSQL failed to start"
    docker-compose logs postgres
    exit 1
fi

echo ""
echo "==================================================="
echo "  Setup Complete! 🎉"
echo "==================================================="
echo ""
echo "Application URL: http://localhost:8080"
echo "Health Check: http://localhost:8080/actuator/health"
echo ""
echo "Database Info:"
echo "  Host: localhost"
echo "  Port: 5432"
echo "  Database: walletdb"
echo "  Username: wallet_user"
echo "  Password: wallet_pass"
echo ""
echo "pgAdmin (optional): http://localhost:5050"
echo "  Email: admin@wallet.local"
echo "  Password: admin"
echo ""
echo "To view logs: docker-compose logs -f wallet-service"
echo "To stop: docker-compose down"
echo ""
echo "Test Users:"
echo "  User ID 1: john.doe@example.com"
echo "  User ID 2: jane.smith@example.com"
echo ""

