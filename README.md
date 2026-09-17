# BarStock

Aplicación full-stack para controlar el inventario de un bar, precios, margen potencial y facturas de proveedores.

## Stack

- Backend: Java 17, Spring Boot, Spring Data JPA
- Frontend: React, TypeScript, Vite y React Router
- Base de datos: MySQL 8 (producción/local) y H2 para la demo desplegada
- Deploy: Docker y Railway

## Demo pública

https://bar-stock-control-production.up.railway.app

La demo utiliza H2 en memoria: cuentas, contraseñas modificadas, stock e imágenes se pierden al reiniciar o redesplegar el servicio. Para conservar datos reales, configurar MySQL con el perfil `mysql` indicado debajo. Se requiere registro o login. Las cuentas comparten el inventario del bar.

## Usuarios y administrador

- Registro público con rol `USER`, login y logout por sesión, cookies HttpOnly y protección CSRF.
- Contraseñas protegidas con BCrypt; mínimo de 12 caracteres al registrarse o cambiar la contraseña.
- Cambiar la contraseña desde **Mi cuenta**.
- El administrador tiene rol `ADMIN` y puede consultar la lista de usuarios.
- Configurar `ADMIN_EMAIL` y `ADMIN_PASSWORD` (12–72 caracteres) antes de iniciar el backend. Se crea el administrador solo si aún no existe. Sin contraseña configurada no se crea un administrador por defecto.
- Las credenciales de Railway se configuran mediante variables de entorno y no se guardan en Git.
- Railway usa cookies Secure sobre HTTPS. Para ejecución local por HTTP no activar `SERVER_SERVLET_SESSION_COOKIE_SECURE`.

## PUL e imágenes

Cada producto requiere un `pulCode` como texto, separado del SKU interno; conserva ceros iniciales y no depende del nombre. Se puede buscar por PUL, nombre, SKU o proveedor. Los códigos `DEMO-*` son ejemplos y deben reemplazarse por los códigos reales del proveedor.

Desde **Stock → Agregar producto / Editar** se puede subir, reemplazar o quitar una imagen PNG, JPEG o WebP de hasta 2 MB. Se guarda en la base de datos y solo se entrega a usuarios autenticados.

Si ya existe una base de datos de la versión anterior, ejecutar una vez `database/migrations/002_users_product_images_pul.sql` y reemplazar los PUL `PENDING-*`. Para una instalación nueva usar `database/bar_stock.sql`.

La gestión de sesiones y CSRF sigue la [documentación de Spring Security](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).

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
