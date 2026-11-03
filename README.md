# DonaTrack - API Gateway

Punto de entrada único al sistema (entrega 6). Recibe todas las requests de los clientes (Postman, MCP Server,
Bot de Telegram) y las reenvía al módulo que corresponde según el prefijo del path. No tiene lógica de negocio
ni base de datos: solo enruta.

Implementado con Spring Cloud Gateway (WebFlux). Las rutas están en `src/main/resources/application.yml`.

Despliegue: https://api-gateway-bti9.onrender.com

## Rutas

Cada módulo vive bajo `/api/<modulo>`. El gateway quita ese prefijo antes de reenviar, así que del lado del
módulo los endpoints no cambian.

| Prefijo en el gateway | Servicio | Ejemplo |
|---|---|---|
| `/api/donaciones/**` | Donaciones | `GET /api/donaciones/donaciones/5` → `GET /donaciones/5` |
| `/api/logistica/**` | Logística | `GET /api/logistica/depositos` → `GET /depositos` |
| `/api/donadores-entidades/**` | Donadores y Entidades | `GET /api/donadores-entidades/necesidades` → `GET /necesidades` |
| `/api/incentivos/**` | Incentivos | `GET /api/incentivos/misiones` → `GET /misiones` |
| `/api/consultas/**` | Servicio de Consultas | `GET /api/consultas/donadores/12/ficha` → `GET /donadores/12/ficha` |

El prefijo por módulo es necesario porque hay paths que existen en más de un módulo (por ejemplo `/donadores`
está en Donadores y Entidades y en Incentivos, y `/admin` en casi todos).

Un path sin prefijo de módulo devuelve 404.

La comunicación entre módulos sigue siendo directa: el gateway es solo para los clientes.

## Endpoints propios

| Endpoint | Para qué |
|---|---|
| `GET /actuator/health` | Estado del gateway (para UptimeRobot) |
| `GET /actuator/gateway/routes` | Rutas configuradas y a qué URL apunta cada una |

## Variables de entorno

| Variable | Valor por defecto |
|---|---|
| `DONACIONES_URL` | `https://marco-maggio-springboot-api-entrega2-dsi.onrender.com` |
| `LOGISTICA_URL` | `https://logistica-v3xw.onrender.com` |
| `DONADORES_ENTIDADES_URL` | `https://donadores-y-entidades-0uv4.onrender.com` |
| `INCENTIVOS_URL` | `https://incentivos-nhyc.onrender.com` |
| `CONSULTAS_URL` | `http://localhost:8081` (hay que configurarla con la URL del Servicio de Consultas) |
| `PORT` | `8080` |

## Deploy en Render

1. New → Web Service → este repositorio. Runtime: Docker (usa el `Dockerfile`). Plan: Free.
2. Cargar al menos `CONSULTAS_URL` y las URLs que cambien respecto de los valores por defecto.
3. Health check path: `/actuator/health`.

Los servicios free de Render se duermen: la primera request a un módulo dormido puede tardar cerca de un minuto.
El gateway espera hasta 120 segundos antes de cortar.

## Tests

`mvn test` levanta un servidor falso por módulo y verifica que cada prefijo llegue al servicio correcto con el
path recortado.
