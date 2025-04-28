#!/usr/bin/env bash
set -euo pipefail

HOST="db"
PORT="5432"
USER="sws_user"
DB="jdbc:postgresql://db:5432/sws"

echo "⏳ - $(date +%T) - Waiting for $HOST:$PORT (user $USER) PostgreSQL to be ready..."
until pg_isready -h "$HOST" -p "$PORT" -U "$USER" -d "$DB"; do
  >&2 echo "Database is unavailable - waiting..."
  sleep 1
done
echo "✅ PostgreSQL online!"

echo "🚀 Starting Wallet Service..."
exec java -jar app.jar
