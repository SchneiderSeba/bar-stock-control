# TODO y roadmap de mejoras

Este documento resume mejoras posibles para BarStock tomando como base el estado actual del proyecto: catalogo de productos, proveedores con multiples SKU, facturas con OCR, reportes de ventas CSV, dashboard comparativo, autenticacion, modo oscuro y persistencia en PostgreSQL.

## Prioridad alta

### 1. Historial completo de inventario

Hoy los movimientos existen en la base y en la ficha de producto, pero falta una pantalla dedicada para auditar todo el inventario.

- Crear una seccion "Movimientos" con filtros por producto, proveedor, tipo, fecha y referencia.
- Permitir ver compras, ventas, ajustes y merma en una sola linea temporal.
- Agregar exportacion CSV para auditorias o cierres mensuales.
- Mostrar el stock antes y despues de cada movimiento para investigar diferencias.

Valor: ayuda a entender por que cambio el stock y reduce discusiones cuando algo no cuadra.

### 2. Correccion y anulacion de facturas

Actualmente una factura aumenta stock al registrarse, pero no hay flujo formal para corregir una factura cargada con errores.

- Agregar estado `VOID` o "Anulada".
- Al anular, crear movimientos inversos en vez de borrar datos.
- Permitir editar una factura solo antes de aplicarla al stock, o implementar una correccion con trazabilidad.
- Mostrar motivo, usuario y fecha de anulacion.

Valor: evita arreglos manuales peligrosos cuando un proveedor envia una factura incorrecta o se cargo mal una cantidad.

### 3. Gestion de mermas y roturas

El ajuste manual existe, pero conviene separar mermas reales de correcciones administrativas.

- Crear flujo "Registrar merma" con motivo: vencido, roto, derrame, error de preparacion, degustacion, invitacion.
- Reporte mensual de merma por categoria y producto.
- Comparar merma contra ventas para detectar problemas operativos.

Valor: permite medir perdida real y no mezclarla con ajustes tecnicos.

### 4. Alertas de stock bajo y reposicion

El dashboard muestra productos con stock bajo, pero podria convertirse en una herramienta de compra.

- Crear una vista "Reponer" con productos bajo minimo.
- Calcular cantidad sugerida segun consumo historico de ventas.
- Permitir filtrar por proveedor y generar una lista de pedido.
- Marcar productos criticos cuando el stock no cubre los proximos dias estimados.

Valor: convierte el inventario en una herramienta de compra diaria.

### 5. Mejoras al OCR de facturas — implementado parcialmente

El flujo OCR ya propone datos desde imagen, pero se puede hacer mas robusto.

- Hecho: permitir pegar texto OCR/PDF ademas de subir imagen.
- Hecho: detectar el total de factura y advertir cuando no coincide con la suma de lineas.
- Hecho: mostrar filas sin relacionar y marcar lineas dudosas por baja confianza o datos incompletos.
- Pendiente: guardar una plantilla de lectura por proveedor: posicion de SKU, cantidad, precio y total.
- Aprender correcciones frecuentes del usuario, por ejemplo cuando un texto OCR se corresponde siempre con el mismo SKU.
- Pendiente: pedir confirmacion especial obligatoria para lineas dudosas antes de registrar.

Valor: menos trabajo manual al cargar facturas repetidas del mismo proveedor.

## Funcionalidades de negocio

### 6. Ordenes de compra

Antes de registrar una factura, el bar podria crear un pedido esperado.

- Crear orden de compra por proveedor con productos y cantidades.
- Comparar orden de compra contra factura recibida.
- Marcar diferencias: cantidad recibida menor, producto no pedido, costo distinto.
- Convertir una orden recibida en factura.

Valor: ordena el flujo real de compra y recepcion.

### 7. Costos historicos y margen real — implementado parcialmente

Hoy el producto guarda el costo actual y las facturas guardan el costo de compra. Falta explotar mejor esa informacion.

- Hecho: mostrar costo historico promedio, rango historico y margen historico promedio en la ficha del producto.
- Hecho: comparar proveedores por costo promedio, ultimo costo y ultima compra.
- Hecho: sugerir visualmente el mejor proveedor historico segun costo promedio.
- Pendiente: mostrar evolucion del costo en un grafico por fecha.
- Pendiente: calcular margen por periodo usando costo historico, no solo costo actual.
- Pendiente: detectar aumentos de precio inusuales al cargar facturas.

Valor: permite decidir compras con datos y entender cambios de rentabilidad.

### 8. Recetas o composiciones

El usuario ya explico que algunos items de venta, como cocteles, llegan desglosados por ingredientes en mililitros. A futuro se podria registrar la receta dentro de la app.

- Crear modulo "Recetas" con ingredientes, cantidades y rendimiento.
- Permitir cargar ventas de cocteles por nombre y que la app descuente ingredientes.
- Calcular costo teorico y margen de cada receta.
- Comparar consumo teorico contra consumo real para detectar sobrepouring.

Valor: abre la puerta a controlar cocteleria y no solo productos directos.

### 9. Conteo fisico de inventario

El stock calculado por sistema necesita contrastarse con el stock real del bar.

- Crear sesiones de conteo por zona: bar, deposito, camara, keg room.
- Permitir guardar conteos parciales desde mobile.
- Mostrar diferencia contra stock teorico.
- Aplicar ajustes de cierre con aprobacion y motivo.

Valor: transforma el sistema en una herramienta de control operativo real.

### 10. Multiubicacion

Si el bar tiene deposito, barra principal o varios locales, el stock deberia poder separarse.

- Agregar ubicaciones.
- Registrar movimientos entre ubicaciones.
- Consultar stock por ubicacion y stock total.
- Cargar facturas a una ubicacion destino.

Valor: evita que "hay stock" signifique algo ambiguo cuando el producto esta en otro lugar.

### 11. Pagos y cuentas por pagar

Las facturas tienen estados `PENDING`, `PAID` y `OVERDUE`, pero el modulo financiero puede crecer.

- Registrar fecha de vencimiento, fecha de pago y metodo de pago.
- Mostrar deuda por proveedor.
- Alertar facturas vencidas.
- Exportar pagos por periodo.

Valor: une inventario con control basico de caja/proveedores.

## Dashboard y reportes

### 12. Dashboard operativo

El dashboard ya compara ventas, compras y profit por periodo. Se puede ampliar para decisiones diarias.

- Top productos por venta estimada.
- Top productos por profit.
- Productos con mayor caida de stock.
- Productos sin ventas recientes.
- Tendencia de compras por proveedor.
- Rotacion de inventario: dias estimados restantes.

Valor: convierte el dashboard en una pantalla de gestion, no solo resumen.

### 13. Reporte de rentabilidad

Crear una seccion enfocada en rentabilidad por producto, categoria y proveedor.

- Profit por producto.
- Margen bruto por categoria.
- Compra mensual vs venta mensual.
- Comparacion contra periodo anterior.
- Exportacion CSV.

Valor: ayuda a decidir precios, promociones y compras.

### 14. Reportes programados

Automatizar reportes para el administrador.

- Resumen diario de stock bajo y ventas aplicadas.
- Resumen semanal de compras, ventas, profit y merma.
- Resumen mensual exportable.
- Preparar la logica para enviar por email cuando se conecte un proveedor de correo.

Valor: menos necesidad de entrar a revisar manualmente todos los dias.

## Experiencia de usuario

### 15. Mobile-first para recepcion de mercaderia

La carga de facturas y conteos probablemente se hace con telefono.

- Optimizar formularios de factura para mobile.
- Botones grandes para escanear, confirmar linea y registrar.
- Mantener encabezado de totales visible mientras se revisan lineas.
- Reducir pasos cuando el proveedor ya esta seleccionado.

Valor: mejora el flujo real de uso en deposito o barra.

### 16. Busqueda global

Agregar una busqueda que encuentre productos, SKU, proveedores, facturas y reportes.

- Campo global en el header.
- Resultados agrupados por tipo.
- Accesos rapidos a ficha de producto, proveedor o factura.
- Soporte para SKU interno y SKU de proveedor.

Valor: navegar por codigo o nombre se vuelve mucho mas rapido.

### 17. Mejoras de formularios

Reducir errores de carga.

- Validaciones en vivo antes de enviar.
- Mensajes de error mas especificos por campo.
- Autocompletado por proveedor en facturas.
- Confirmacion visual cuando una accion modifica stock.
- Atajos para duplicar una linea de factura.

Valor: menos friccion y menos errores humanos.

## Usuarios, permisos y seguridad

### 18. Roles mas granulares

Hoy existen `ADMIN` y `USER`. Para operacion real puede hacer falta mas detalle.

- `ADMIN`: configuracion, usuarios y todo el sistema.
- `MANAGER`: productos, proveedores, facturas, reportes y ajustes.
- `STAFF`: consultar stock y cargar conteos o ventas, sin editar precios.
- `VIEWER`: solo lectura.

Valor: permite que mas personas usen el sistema sin dar permisos peligrosos.

### 19. Auditoria por usuario

Registrar quien hizo cada accion importante.

- Usuario que creo, edito o anulo una factura.
- Usuario que aplico un reporte de ventas.
- Usuario que hizo ajustes o mermas.
- Historial de cambios de precios y SKU.

Valor: da trazabilidad cuando hay errores o diferencias.

### 20. Hardening de seguridad

Mejoras para produccion.

- Limitar intentos de login.
- Politica de contrasenas mas clara.
- Recuperacion de contrasena por email.
- Configuracion explicita de CORS si se separa frontend/backend.
- Revisar cabeceras HTTP de seguridad.
- Evitar `ddl-auto: update` en produccion y usar migraciones versionadas.

Valor: reduce riesgo operativo cuando el sistema contiene datos reales del negocio.

## Datos, integraciones y automatizacion

### 21. Migraciones versionadas

El proyecto tiene scripts SQL historicos y Hibernate actualiza schema. Para produccion conviene usar migraciones controladas.

- Introducir Flyway o Liquibase.
- Convertir scripts existentes en migraciones versionadas.
- Ejecutar migraciones automaticamente al iniciar.
- Documentar rollback o backups antes de cambios grandes.

Valor: despliegues mas predecibles y menos riesgo de romper Railway/PostgreSQL.

### 22. Importacion masiva de productos

Ya existe endpoint bulk, pero podria exponerse mejor en UI.

- Cargar CSV de productos iniciales.
- Validar SKU internos y SKU de proveedor antes de importar.
- Previsualizar errores y duplicados.
- Descargar plantilla CSV.

Valor: acelera cargar catalogos grandes.

### 23. Integracion con POS

Los reportes de ventas hoy se cargan por CSV. A futuro se puede integrar con el sistema de ventas.

- Importar automaticamente ventas desde API del POS si existe.
- Mapear SKU de POS contra productos internos.
- Programar carga diaria.
- Alertar ventas no aplicadas.

Valor: elimina carga manual y hace el dashboard mas confiable.

### 24. Integracion con proveedores

Si proveedores entregan listas de precios o facturas digitales, se puede automatizar mas.

- Importar listas de precios.
- Detectar cambios contra costos actuales.
- Leer facturas PDF o XML si el proveedor los envia.
- Crear pedidos sugeridos por proveedor.

Valor: reduce carga manual y mejora control de costos.

## Calidad tecnica

### 25. Separar logica de negocio en servicios

Los controladores contienen bastante logica de negocio. Conviene moverla gradualmente.

- `InvoiceService` para registrar facturas, crear productos faltantes y movimientos.
- `SalesReportService` para parsear, validar y aplicar ventas.
- `DashboardService` para metricas.
- `ProductService` para SKU, proveedores e imagenes.

Valor: facilita pruebas, cambios y mantenimiento.

### 26. DTOs explicitos y respuestas estables

Algunos endpoints devuelven entidades JPA directamente.

- Crear DTOs de lectura y escritura por modulo.
- Evitar exponer campos internos accidentalmente.
- Controlar mejor ciclos y datos anidados.
- Versionar contratos de API si la app crece.

Valor: frontend y backend quedan menos acoplados a la estructura interna de la base.

### 27. Manejo centralizado de errores

Actualmente se lanzan varios `ResponseStatusException`.

- Crear `@ControllerAdvice`.
- Responder errores con formato consistente.
- Incluir codigo de error, mensaje y campo afectado cuando aplique.
- Traducir errores de validacion Bean Validation a mensajes utiles.

Valor: mejores mensajes en UI y menos manejo repetido.

### 28. Pruebas end-to-end

Ya hay buenas pruebas de backend y algunas pruebas frontend. Falta cubrir journeys completos.

- Login.
- Crear proveedor.
- Crear producto.
- Cargar factura.
- Cargar y aplicar reporte de ventas.
- Ver cambios en dashboard.
- Probar modo oscuro y responsive.

Valor: protege los flujos principales antes de cada deploy.

### 29. Observabilidad

Agregar visibilidad para errores reales en produccion.

- Logs estructurados para cargas de factura, reportes y auth.
- Health checks mas detallados.
- Metricas basicas: reportes aplicados, facturas cargadas, errores OCR.
- Captura de errores frontend.

Valor: facilita diagnosticar fallos en Railway sin depender solo de reportes manuales.

### 30. Backups y recuperacion

PostgreSQL en Railway guarda los datos, pero conviene documentar y probar backups.

- Documentar estrategia de backup.
- Probar restore en una base temporal.
- Exportar snapshots CSV desde la app.
- Agregar pantalla de exportacion completa para admin.

Valor: protege el trabajo de carga de datos del bar.

## Ideas futuras

### 31. Forecast de demanda

Usar ventas historicas para predecir consumo.

- Estimar ventas por producto para la proxima semana.
- Detectar estacionalidad por dia de semana.
- Recomendar compras segun stock actual y lead time de proveedor.

### 32. Control de precios de venta

Ayudar a definir precios segun costo y margen objetivo.

- Margen objetivo por categoria.
- Sugerencia de precio de venta.
- Alerta cuando el costo sube y el margen cae.
- Historial de cambios de precio.

### 33. App instalable/PWA

Mejorar uso en telefono.

- Instalable en mobile.
- Cache de assets principales.
- Icono y manifest.
- Modo offline parcial para conteos, sincronizando luego.

### 34. Etiquetas y codigos QR

Generar etiquetas para productos o ubicaciones.

- QR a ficha de producto.
- QR para conteo rapido.
- Impresion de etiquetas con SKU interno y proveedor.

### 35. Multi-bar o multi-tenant

Si el proyecto se usa en mas de un bar, separar datos por negocio.

- Entidad `Organization` o `Venue`.
- Usuarios asignados a uno o varios bares.
- Productos y proveedores por bar.
- Dashboard consolidado para dueños.

## Sugerencia de orden de implementacion

1. Historial de movimientos global.
2. Anulacion/correccion de facturas.
3. Mermas separadas de ajustes.
4. Vista de reposicion.
5. Auditoria por usuario.
6. Migraciones versionadas.
7. Pruebas end-to-end.
8. Ordenes de compra.
9. Completar rentabilidad avanzada por periodo.
10. Conteo fisico de inventario.

Ese orden prioriza control operativo y seguridad de datos antes de agregar automatizaciones mas grandes.
