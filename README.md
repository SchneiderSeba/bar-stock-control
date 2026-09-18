# BarStock

Aplicación para administrar el stock de un bar, los precios de sus productos y las facturas de proveedores. Backend en Java 17 con Spring Boot y frontend en React + TypeScript.

[Probar la demo](https://bar-stock-control-production.up.railway.app) · [Repositorio](https://github.com/SchneiderSeba/bar-stock-control)

## Cómo funciona

Las cuentas registradas comparten el inventario del mismo bar. Después de iniciar sesión, puedes:

1. **Dashboard:** consultar el valor del inventario, ingresos potenciales, ganancias potenciales y productos con stock bajo.
2. **Productos:** crear y editar ítems con SKU, nombre, categoría, formato, proveedores y sus SKU, stock mínimo, precio de venta e imagen. Cada ítem nuevo empieza con stock y costo en cero; no se carga cantidad en el catálogo.
3. **Stock:** consultar cantidades, buscar productos y registrar salidas o consumos. Las entradas se registran mediante facturas. Editar el catálogo no modifica el stock ni el costo de compra.
4. **Imágenes:** subir, reemplazar o quitar una imagen desde el formulario de Productos. Se aceptan PNG, JPEG y WebP de hasta 2 MB. Las imágenes se guardan en la base de datos y requieren sesión para consultarlas.
5. **Ajustes:** descontar cantidades con un motivo. El backend registra un movimiento y rechaza ajustes que dejarían stock negativo.
6. **Proveedores:** registrar el nombre comercial, contacto, email, teléfono e identificación fiscal.
7. **Facturas:** registrar una compra con proveedor, número, fecha, estado e importes. El stock aumenta, el costo de cada producto se actualiza al costo de la compra y se registra un movimiento `PURCHASE`. La operación se ejecuta en una transacción.
8. **Mi cuenta:** cambiar la contraseña o cerrar sesión.
9. **Usuarios** (administrador): consultar las cuentas y sus roles.

Cada producto conserva un **SKU interno**, obligatorio y único en el inventario. Además tiene una lista de **proveedores y SKU del proveedor**: por ejemplo, Guinness puede tener `50055` con un proveedor y `9878` con otro; ambos ingresan al mismo stock. Un mismo proveedor puede tener varios códigos para el mismo ítem, pero cada combinación proveedor + SKU solo puede identificar un producto. Los códigos se guardan como texto y conservan ceros iniciales. En Productos → Editar ítem → Proveedores y sus SKU puedes agregar o quitar esas asignaciones. En Proveedores → Editar proveedor puedes actualizar nombre, contacto, email, teléfono y Tax ID sin cambiar su identidad ni sus relaciones.

### Kegs de 50 L, 30 L o 20 L

En **Productos**, al crear o editar un ítem, selecciona **Formato → Keg (stock en litros)** y el tamaño: **50 L keg**, **30 L keg** o **20 L keg**.

- El stock comienza en cero y se recibe al cargar facturas. Stock disponible, stock mínimo y salidas se expresan en **litros**, incluidos saldos parciales (por ejemplo 42.5 L).
- La tabla muestra litros disponibles y su equivalente en kegs: 75 L en un producto de 30 L equivalen a 2.5 kegs.
- Costo y precio de venta se expresan **por keg** del tamaño seleccionado.
- El dashboard calcula el valor como `(litros / tamaño del keg) × precio por keg`; el margen porcentual sigue utilizando los precios por keg.
- En las facturas se ingresan **cantidad de kegs y costo por keg**. Una compra de 2 kegs de 30 L suma 60 L al inventario. Los movimientos de esos productos se guardan en litros.
- Los productos de ejemplo de 50 L pasan de 8 y 5 kegs a 400 y 250 L, respectivamente. Sus mínimos pasan de 3 kegs a 150 L y sus precios se mantienen.

Para una base MySQL anterior, ejecutar una sola vez `database/migrations/004_keg_litres.sql` después de las migraciones que correspondan. Convierte stock, mínimos y movimientos existentes de unidades de keg a litros; conserva precios y líneas de factura. El script infiere 20/30 L del nombre y usa 50 L cuando no encuentra un tamaño: revisa esa asignación antes de ejecutar la conversión. Para instalación nueva, el SQL principal ya incluye tamaños y cantidades en litros.

### Flujo de una entrega

1. Crea el producto en **Productos**. Si es un keg, selecciona 50, 30 o 20 L. El ítem empieza en cero.
2. Ve a **Facturas → Cargar factura**, elige proveedor, número y fecha.
3. Selecciona cada ítem por el SKU del proveedor elegido, indica cantidad y costo por unidad/keg y usa **Agregar ítem** para completar la entrega.
4. Revisa los subtotales, el total y los litros/unidades que se recibirán. Registra la factura.
5. Consulta el stock actualizado y el dashboard. Por ejemplo, 2 kegs de 30 L y 5 botellas ingresan como 60 L y 5 botellas en sus respectivos ítems.

### Qué significan los indicadores

| Indicador | Cálculo |
| --- | --- |
| Valor del stock | Suma de cantidad valorizada × costo actual |
| Ingresos potenciales | Suma de cantidad valorizada × precio de venta |
| Ganancia potencial | Ingresos potenciales − valor del stock |
| Margen de producto | (Precio de venta − costo) / precio de venta × 100 |
| Stock bajo | Cantidad menor o igual al stock mínimo |

Son estimaciones del inventario disponible: no representan ventas realizadas ni beneficio neto. No se incluyen impuestos, gastos operativos ni un módulo de ventas.

### Alcance actual

La interfaz de facturas permite agregar y quitar varias líneas, seleccionando los ítems ya creados, sus cantidades y costos. Muestra el subtotal, el total y el stock que se recibirá. Se rechazan facturas vacías, cantidades no positivas y números duplicados para el mismo proveedor. Los estados son `PENDING`, `PAID` y `OVERDUE`; todos incrementan el stock al registrar la compra. Los movimientos se almacenan, pero todavía no hay una pantalla de historial. El administrador puede consultar usuarios; no hay gestión de roles desde la interfaz.

## Arquitectura y estructura

En desarrollo, Vite sirve React en el puerto 5173 y reenvía `/api` al backend del puerto 8080. React utiliza rutas relativas y la misma cookie de sesión; no hace falta configurar una URL de API en el frontend.

En Docker y Railway, el frontend compilado se incluye dentro del JAR. Spring Boot sirve tanto las páginas como la API desde un único servicio. Las rutas `/stock`, `/products`, `/suppliers` y `/invoices` se reenvían a `index.html` para que React Router las gestione.

```text
bar-stock-control/
├── frontend/                    # React, TypeScript, Vite y React Router
│   └── src/
│       ├── App.tsx              # Navegación, dashboard, proveedores y facturas
│       ├── Auth.tsx             # Login, registro, cuenta y usuarios
│       ├── Stock.tsx            # Consulta de cantidades y salidas
│       ├── Products.tsx         # Catálogo e imágenes, sin stock inicial
│       ├── Invoices.tsx         # Facturas con múltiples ítems
│       └── api.ts               # HTTP, cookies de sesión y token CSRF
├── src/main/java/com/barstock/
│   ├── api/                     # Endpoints de negocio y autenticación
│   ├── config/                  # Seguridad, datos de ejemplo y administrador
│   ├── model/                   # Entidades JPA
│   └── repository/              # Acceso a datos con Spring Data JPA
├── src/main/resources/application.yml
├── src/test/                    # Pruebas de integración del backend
├── database/bar_stock.sql       # Instalación nueva en MySQL
├── database/migrations/         # Actualizaciones de una base existente
├── pom.xml                      # Dependencias y compilación Java
└── Dockerfile                   # Compila y empaqueta frontend + backend
```

Las tablas principales son `app_users`, `suppliers`, `products`, `supplier_invoices`, `supplier_invoice_items` y `stock_movements`.

## Requisitos para ejecutarlo en local

- JDK 17 o superior; el contenedor usa Java 17.
- Maven 3.9.x disponible en `PATH` (el repositorio no incluye Maven Wrapper).
- Node.js 22.12 o superior y npm para el frontend.
- Git para clonar el proyecto.
- MySQL 8 y MySQL Workbench solo si quieres datos persistentes.
- Docker con su motor iniciado solo para la opción Docker.

Comprueba las herramientas:

```text
java -version
mvn -version
node --version
npm --version
```

Clona y entra en el proyecto:

```text
git clone https://github.com/SchneiderSeba/bar-stock-control.git
cd bar-stock-control
```

Si ya tienes esta carpeta, empieza directamente dentro de `bar-stock-control`, donde está `pom.xml`.

## Opción 1: inicio rápido con H2 (sin instalar MySQL)

Es la forma más rápida de probar la app. Spring Boot crea las tablas y carga cuatro productos y dos proveedores de ejemplo. H2 funciona en memoria: cuentas, imágenes y cambios se pierden al detener o reiniciar el backend.

### Terminal 1: backend

Desde la raíz del proyecto, en **PowerShell**:

```powershell
$env:ADMIN_EMAIL = "admin@barstock.app"
$env:ADMIN_PASSWORD = Read-Host "Contraseña inicial del admin (12 a 72 caracteres)"
mvn spring-boot:run
```

En **Bash** (Linux/macOS):

```bash
export ADMIN_EMAIL='admin@barstock.app'
read -rsp 'Contraseña inicial del admin: ' ADMIN_PASSWORD; echo
export ADMIN_PASSWORD
mvn spring-boot:run
```

Usa una contraseña de 12 a 72 caracteres; BCrypt también limita a 72 bytes en UTF-8. El administrador se crea al iniciar si su email aún no existe. Si no configuras `ADMIN_PASSWORD`, no se crea un administrador, pero puedes registrar una cuenta normal desde la app. La variable no reemplaza la contraseña de una cuenta existente.

Si reutilizas una terminal previamente configurada para MySQL, elimina `SPRING_PROFILES_ACTIVE` antes de usar H2 (`Remove-Item Env:SPRING_PROFILES_ACTIVE` en PowerShell, `unset SPRING_PROFILES_ACTIVE` en Bash).

### Terminal 2: frontend

Desde otra terminal, entra en la carpeta del proyecto:

```text
cd frontend
npm ci
npm run dev
```

Abre **http://localhost:5173**, inicia sesión con el administrador configurado o pulsa **Crear una cuenta**. La API estará en **http://localhost:8080/api**. El login se mantiene al recargar mientras la sesión siga vigente.

Detén ambos procesos con `Ctrl+C`. Para ejecutar comandos adicionales utiliza otra terminal.

## Opción 2: ejecutar con MySQL persistente

### 1. Crear una base nueva

Inicia MySQL y conéctate desde MySQL Workbench. Abre `database/bar_stock.sql` y ejecútalo completo. Crea la base `bar_stock`, sus tablas y datos de ejemplo.

El script de instalación es para una base nueva: no vuelvas a ejecutarlo sobre una instalación existente porque las tablas ya estarán creadas. Para actualizar, sigue la sección de migraciones (incluida la 004 para kegs).

### 2. Configurar el backend

En **PowerShell**, desde la raíz del proyecto:

```powershell
$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:MYSQL_URL = "jdbc:mysql://localhost:3306/bar_stock?useSSL=false&serverTimezone=UTC"
$env:MYSQL_USER = "root"
$env:MYSQL_PASSWORD = Read-Host "Contraseña de MySQL"
$env:ADMIN_EMAIL = "admin@barstock.app"
$env:ADMIN_PASSWORD = Read-Host "Contraseña inicial del admin"
mvn spring-boot:run
```

En **Bash**:

```bash
export SPRING_PROFILES_ACTIVE=mysql
export MYSQL_URL='jdbc:mysql://localhost:3306/bar_stock?useSSL=false&serverTimezone=UTC'
export MYSQL_USER=root
read -rsp 'Contraseña de MySQL: ' MYSQL_PASSWORD; echo
export MYSQL_PASSWORD
export ADMIN_EMAIL='admin@barstock.app'
read -rsp 'Contraseña inicial del admin: ' ADMIN_PASSWORD; echo
export ADMIN_PASSWORD
mvn spring-boot:run
```

Sustituye usuario, host y puerto por los de tu servidor. La URL con `useSSL=false` es para el ejemplo local; usa la configuración TLS de tu servidor para una base remota. Con el perfil `mysql`, Hibernate **valida** las tablas: no las crea ni ejecuta las migraciones automáticamente.

### 3. Iniciar React

En otra terminal, ejecuta `npm ci` y `npm run dev` dentro de `frontend`. Abre http://localhost:5173. Los datos de MySQL se conservan al reiniciar; las sesiones requieren volver a iniciar sesión al reiniciar el backend.

### Actualizar una base existente

Haz un respaldo de la base antes de actualizar. Ejecuta los scripts una sola vez según tu versión:

| Estado de la base | Scripts necesarios |
| --- | --- |
| Instalación nueva | Solo `database/bar_stock.sql` |
| Versión original sin usuarios ni imágenes | `002_users_product_images_pul.sql`, después `003_sku_only.sql` y `004_keg_litres.sql` |
| Versión anterior que ya tiene usuarios, imágenes y código PUL | `003_sku_only.sql` y `004_keg_litres.sql` |
| Versión anterior con solo SKU, sin tamaño de keg | `004_keg_litres.sql` |
| Versión actual con SKU y tamaño de keg | Ninguno |

La migración 002 se conserva como parte del historial. La 003 elimina la antigua columna PUL y conserva el SKU y el resto de los datos. No ejecutes estas migraciones después del script de instalación nuevo.

## Opción 3: frontend y backend juntos con Docker

Desde la raíz, con Docker iniciado:

```text
docker build -t barstock .
```

Para iniciar con H2, primero configura `ADMIN_EMAIL` y `ADMIN_PASSWORD` en tu terminal como en la opción 1. Luego:

```text
docker run --rm --name barstock -p 8080:8080 -e ADMIN_EMAIL -e ADMIN_PASSWORD barstock
```

Abre **http://localhost:8080**. No necesitas iniciar Vite por separado. Este contenedor usa H2 en memoria por defecto y pierde sus datos al reiniciar. Se detiene con `Ctrl+C`.

Para usar MySQL desde Docker, configura las seis variables de la opción 2 y pásalas al contenedor:

```text
docker run --rm --name barstock -p 8080:8080 -e SPRING_PROFILES_ACTIVE -e MYSQL_URL -e MYSQL_USER -e MYSQL_PASSWORD -e ADMIN_EMAIL -e ADMIN_PASSWORD barstock
```

La base debe estar creada antes de iniciar. Dentro del contenedor, `localhost` apunta al propio contenedor. En Docker Desktop, usa `host.docker.internal` en `MYSQL_URL` si MySQL está en tu computadora; si está en otro contenedor, usa su nombre en una red Docker compartida. El comando anterior no crea un servicio MySQL.

## Variables de entorno

| Variable | Función / valor predeterminado |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `mysql` para MySQL; sin definir utiliza H2 |
| `MYSQL_URL` | URL JDBC; por defecto base `bar_stock` en `localhost:3306` |
| `MYSQL_USER` | Usuario MySQL; predeterminado `root` |
| `MYSQL_PASSWORD` | Contraseña MySQL; predeterminado vacío |
| `ADMIN_EMAIL` | Email del administrador inicial; predeterminado `admin@barstock.app` |
| `ADMIN_PASSWORD` | Contraseña para crear el administrador inicial; sin valor no se crea |
| `SERVER_PORT` | Puerto del backend local; predeterminado 8080 |
| `PORT` | Puerto utilizado por el comando de inicio del contenedor; predeterminado 8080 |
| `SERVER_SERVLET_SESSION_COOKIE_SECURE` | `true` en HTTPS; no activarlo en desarrollo HTTP |

Las variables de PowerShell y Bash solo afectan a esa terminal y a sus procesos hijos. Spring Boot no lee automáticamente un archivo `.env` en este proyecto. No subas contraseñas al repositorio.

## Autenticación y API

La autenticación utiliza Spring Security, contraseñas BCrypt y sesión HTTP con cookie HttpOnly y SameSite=Lax. La sesión vence tras 8 horas de inactividad. Los registros públicos obtienen siempre rol `USER`; la lista de usuarios exige rol `ADMIN`.

La API de negocio y las imágenes requieren login. `/api/health`, registro, login y obtención del token CSRF son públicos. Las operaciones de escritura, incluidos login y registro, requieren un token CSRF. El cliente en `frontend/src/api.ts` obtiene ese token y adjunta su cabecera automáticamente.

Para usar Postman u otro cliente: llama a `GET /api/auth/csrf`, conserva la cookie y envía el valor `token` en la cabecera indicada por `headerName` al hacer la siguiente operación de escritura. Conserva también la cookie que devuelve el login.

| Método y ruta | Uso |
| --- | --- |
| `GET /api/health` | Estado del backend |
| `GET /api/auth/csrf` | Token para formularios |
| `POST /api/auth/register` | Registro: nombre, email y contraseña |
| `POST /api/auth/login` | Login: email y contraseña |
| `GET /api/auth/me` | Usuario de la sesión |
| `POST /api/auth/logout` | Cerrar sesión |
| `POST /api/auth/password` | Cambiar contraseña actual |
| `GET /api/admin/users` | Lista de usuarios, solo administrador |
| `GET /api/dashboard` | Indicadores del inventario |
| `GET, POST /api/products` | Listar o crear productos |
| `PUT /api/products/{id}` | Editar producto |
| `POST /api/products/{id}/adjust` | Registrar salida (cantidad negativa) |
| `GET, POST, DELETE /api/products/{id}/image` | Leer, subir o quitar imagen; subida multipart con campo `file` |
| `GET, POST /api/suppliers` | Listar o crear proveedores |
| `PUT /api/suppliers/{id}` | Editar datos del proveedor |
| `GET, POST /api/invoices` | Listar o registrar facturas |

## Reportes de ventas en CSV

En **Reportes de ventas**, selecciona día, semana (7 días desde la fecha elegida) o mes calendario, y carga un CSV UTF-8 de hasta 2 MB / 10.000 filas. La tarjeta **Último reporte subido** y el historial muestran el archivo, fechas, productos y estado. Puedes consultar el detalle y descargar el CSV original.

```csv
SKU,nombre del ítem,cantidad vendida,mililitros vendidos
BEER-001,Guinness,10,5000
SPIR-001,Jameson ingrediente de cóctel,3,150
```

Se aceptan comas, punto y coma o tabulaciones como separador, campos entre comillas y encabezados con espacios/acentos o guiones bajos. Las cantidades aceptan punto decimal, o coma decimal en CSV con punto y coma/campos entre comillas; no deben usar separadores de miles.

**Mililitros vendidos es el total de la fila**, no el volumen de una porción. No se multiplica por la cantidad vendida. Si el sistema de ventas desglosa un cóctel en ingredientes, cada ingrediente debe llevar su propio SKU. El nombre es informativo: la identificación usa el SKU interno o los SKU de proveedores del catálogo. Si un código de proveedor coincide con varios productos, el reporte se rechaza y debes utilizar el SKU interno inequívoco.

Se agrupan todas las filas/SKU del mismo producto y se convierte el total una sola vez: para kegs, 5.000 ml descuentan 5 L; para botellas o cajas, configura **Mililitros por unidad (capacidad)** en Productos. Ejemplo: 150 ml de una botella de 700 ml descuentan 0,214286 botellas. Para una caja indica su volumen completo. Las fracciones de stock se guardan con seis decimales. No se infiere la capacidad a partir del nombre.

- **Listo para aplicar:** todos los ítems y cantidades son válidos. Revisa el detalle y pulsa **Aplicar ventas al stock**.
- **Rechazado:** muestra errores por fila, capacidad faltante, stock insuficiente o período superpuesto. No cambia stock. Corrige el CSV/catálogo y vuelve a cargarlo; un archivo rechazado se puede revalidar.
- **Aplicado:** registra movimientos `SALE`, con referencia al reporte, y descuenta todos los productos en una transacción. Se vuelve a comprobar stock y capacidad al aplicar. Un reporte aplicado no puede aplicarse de nuevo.

No se permite cargar de nuevo el mismo archivo para un período listo/aplicado, ni aplicar períodos que se superpongan con otro reporte aplicado, para evitar descontar dos veces las mismas ventas. Por ejemplo, si ya aplicaste un día, no puedes aplicar luego una semana que incluya ese día. Usa una misma granularidad para cada tramo de fechas.

API autenticada: `GET /api/sales-reports`, `POST /api/sales-reports` (multipart: `file`, `period=DAILY|WEEKLY|MONTHLY`, `startDate=YYYY-MM-DD`), `POST /api/sales-reports/{id}/apply` y `GET /api/sales-reports/{id}/file`. Escrituras requieren CSRF. Para MySQL existente aplica una sola vez `database/migrations/006_sales_reports.sql` después de 005; la instalación nueva ya incluye esta estructura. La demo H2 también pierde reportes al reiniciar.

## Compilación y pruebas

Desde la raíz:

```text
mvn test
mvn package
```

Las pruebas cubren registro, permisos, login, cambio de contraseña, SKU internos y por proveedor, edición de proveedores, códigos históricos de facturas, imágenes, catálogo sin stock inicial, facturas con múltiples ítems, conversión de kegs a litros, rechazo de duplicados y rollback si falla una línea. El JAR generado en `target/bar-stock-0.1.0.jar` incluye el backend; para un paquete con la interfaz integrada, usa Docker, que copia la compilación de React dentro del JAR.

Dentro de `frontend`:

```text
npm ci
npm run build
```

Genera los archivos estáticos en `frontend/dist`. `npm run preview` solo previsualiza esa compilación: no reemplaza al backend ni configura el proxy de desarrollo. Para probar toda la app utiliza `npm run dev` con Spring Boot o el contenedor Docker.

## Railway y demo publicada

Railway despliega la rama `main` usando el `Dockerfile`. El contenedor escucha en `PORT` y sirve frontend y backend juntos. Las variables del administrador se configuran en el servicio, con cookies Secure porque la URL usa HTTPS.

**La demo publicada sigue usando H2 en memoria:** cuentas registradas, cambios de contraseña, productos, facturas e imágenes se pierden al reiniciar o redesplegar el servicio. El administrador inicial vuelve a crearse a partir de las variables configuradas. Para conservar datos, configura MySQL, ejecuta el SQL y activa el perfil `mysql`.

## Problemas comunes

| Problema | Qué revisar |
| --- | --- |
| `mvn` o `java` no reconocido | Instalación, `PATH` y `JAVA_HOME`; reiniciar la terminal |
| Error de versión de Node | Usar Node 22.12 o superior y volver a ejecutar `npm ci` |
| Puerto 8080 ocupado | Detener el proceso anterior; si cambias `SERVER_PORT`, actualizar el destino en `frontend/vite.config.ts` |
| Puerto 5173 ocupado | Vite puede elegir otro puerto; abrir la URL que muestra la terminal |
| MySQL rechaza la conexión | Servicio iniciado, host, puerto, usuario, contraseña y permisos sobre `bar_stock` |
| Hibernate indica tablas o columnas faltantes | Ejecutar el SQL de instalación o las migraciones correspondientes |
| Login no mantiene la sesión en local | No usar cookie Secure sobre HTTP; abrir la app desde el servidor de Vite o Docker |
| Respuesta 401 | Iniciar sesión otra vez; comprobar cookies y si el backend se reinició |
| Respuesta 403 | Comprobar rol para rutas admin y token CSRF para escrituras |
| Imagen rechazada | PNG, JPEG o WebP real, máximo 2 MB |
| Cambios desaparecen | Se está usando H2; configurar MySQL para persistencia |

Para bases MySQL existentes, aplica una sola vez `database/migrations/005_supplier_skus.sql` después de 004. Crea la relación de múltiples SKU, conserva las asignaciones proveedor/SKU actuales y agrega el código histórico a las líneas de factura. La instalación nueva ya incluye esta estructura en `database/bar_stock.sql`. Al editar un código luego de una compra, la factura conserva el SKU que se usó al registrarla.
