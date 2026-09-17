# BarStock

Aplicación full-stack para controlar el inventario de un bar, precios, margen potencial y facturas de proveedores.

## Stack

- Backend: Java 17, Spring Boot, Spring Data JPA
- Frontend: React, TypeScript, Vite y React Router
- Base de datos: MySQL 8 (producción/local) y H2 para la demo desplegada
- Deploy: Docker y Railway

## Funcionalidades del MVP

- Dashboard con valor del stock, facturación potencial y profit estimado.
- Alta y edición de productos, precios, stock mínimo y cantidad disponible.
- Ajustes rápidos de inventario con trazabilidad.
- Gestión de proveedores.
- Registro de facturas de compra; al registrarlas se incrementa el stock automáticamente.
- Alertas de stock bajo.

## Ejecutar con MySQL Workbench

1. Ejecutar `database/bar_stock.sql` en MySQL Workbench.
2. Configurar las variables:

```bash
export SPRING_PROFILES_ACTIVE=mysql
export MYSQL_URL='jdbc:mysql://localhost:3306/bar_stock?useSSL=false&serverTimezone=UTC'
export MYSQL_USER='root'
export MYSQL_PASSWORD='tu_password'
```

3. Iniciar Spring Boot y, en otra terminal, el frontend:

```bash
mvn spring-boot:run
cd frontend && npm install && npm run dev
```

La app estará en `http://localhost:5173` y la API en `http://localhost:8080/api`.

## Docker

```bash
docker build -t barstock .
docker run -p 8080:8080 barstock
```
