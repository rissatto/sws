#!/bin/bash
set -e

# Espera o banco ficar disponível
echo "⏳ Waiting for PostgreSQL to be ready..."
until pg_isready -h db -p 5432 -U wallet_user > /dev/null 2>&1; do
  >&2 echo "Database is unavailable - waiting..."
  sleep 2
done
echo "✅ PostgreSQL online!"

# Espera o Flyway concluir
echo "⏳ Waiting for Flyway..."
until [ -f /flyway/flyway.done ]; do
  echo "⌛ Flyway migrations not finished..."
  sleep 2
done
echo "✅ Flyway Done!"

# Check if the wallets table exists (implies Flyway migration ran)
until PGPASSWORD=wallet_pass psql -h db -U wallet_user -d wallet -c "SELECT 1 FROM wallets LIMIT 1;" > /dev/null 2>&1; do
  sleep 2
done

echo "🚀 Starting Wallet Service..."
exec java -jar app.jar
