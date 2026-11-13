# Actividad 4

API académica con registro, autenticación JWT y CRUD REST de materias por propietario. El VPS todavía no contiene esta versión; no se ha desplegado desde este proyecto.

## Tecnologías

- Java 17 como versión objetivo y Maven Wrapper.
- Spring Boot 4.1.2: Spring Web MVC, Data JPA, Security, OAuth2 Resource Server y Bean Validation.
- MySQL Connector/J y herramientas de prueba de Spring Boot y Spring Security.

## Configuración local

Configura las variables de entorno `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` antes de iniciar la aplicación con una base de datos MySQL. Si no se define `DB_URL`, se utiliza `jdbc:mysql://localhost:3306/araj_act4_dev`. No guardes credenciales en el repositorio. Hibernate valida el esquema existente (`ddl-auto=validate`), pero no crea tablas.

El puerto predeterminado es 8088. Para desplegar en el VPS puede sobrescribirse, por ejemplo, con la variable de entorno `SERVER_PORT=8087`, sin modificar el código.

Para compilar y ejecutar las pruebas en Windows:

```powershell
.\mvnw.cmd verify
```

Las pruebas de persistencia y del controlador utilizan H2 exclusivamente en memoria, ejecutan `database/schema.sql` en modo de compatibilidad MySQL y validan los mapeos con Hibernate. Las pruebas HTTP usan MockMvc; las nuevas pruebas JWT verifican firmas reales con claves efímeras de prueba. No acceden a la base MySQL local y no sustituyen una verificación en MySQL 8 real.

### Clave RSA y ejecución local

La aplicación necesita una clave RSA privada **PKCS#8 PEM sin cifrar**, de al menos 2048 bits. La clave pública se deriva de ella. `JWT_PRIVATE_KEY` contiene la ruta al archivo (no su contenido); `JWT_ISSUER` identifica al emisor y por defecto es `act4-api`. Si falta la clave o es inválida, el arranque falla: no se genera una clave alternativa.

Con OpenSSL instalado, un ejemplo en PowerShell 7 para desarrollo local es el siguiente. Si `openssl` no está en `PATH` y tienes Git para Windows, comprueba si existe `C:\Program Files\Git\usr\bin\openssl.exe` y utiliza esa ruta completa con `&` en vez de `openssl`. No sobrescribas una clave ya existente: al cambiarla dejan de validarse los tokens emitidos con la anterior.

```powershell
New-Item -ItemType Directory -Force "$HOME\act4-claves"
if (Test-Path "$HOME\act4-claves\jwt-private.pem") { throw "La clave ya existe; no se sobrescribirá" }
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$HOME\act4-claves\jwt-private.pem"
$env:JWT_PRIVATE_KEY = "$HOME\act4-claves\jwt-private.pem"
$env:JWT_ISSUER = "act4-local"
$env:DB_URL = "jdbc:mysql://localhost:3306/araj_act4_dev"
$env:DB_USERNAME = Read-Host "Usuario MySQL"
$env:DB_PASSWORD = Read-Host "Contraseña MySQL" -AsSecureString | ConvertFrom-SecureString -AsPlainText
.\mvnw.cmd spring-boot:run
```

La opción `ConvertFrom-SecureString -AsPlainText` requiere PowerShell 7. En Windows PowerShell 5.1, establece `DB_PASSWORD` por otro mecanismo seguro del entorno. Restringe los permisos del archivo de clave al usuario que ejecuta la aplicación y almacénalo fuera del repositorio; el `.gitignore` también excluye `*.pem` y `*.key` como protección adicional, no como sustituto de los permisos. No compartas la clave ni la subas a Git. Para el VPS deberán generarse y protegerse claves independientes y configurarse su propio emisor; no se ha accedido al VPS. El transporte en entornos reales debe usar HTTPS.

## Registro y credenciales

`POST /api/auth/register` es público y recibe JSON, por ejemplo:

```json
{"nombre":"Ana", "email":" Ana@example.test ", "password":"<contraseña-elegida>"}
```

El nombre es obligatorio, no puede estar en blanco y admite hasta 255 caracteres. El correo es obligatorio, debe cumplir `@Email`, admite hasta 255 caracteres y se normaliza quitando espacios exteriores y convirtiéndolo a minúsculas antes de validar y guardar. Debe ser único; la restricción UNIQUE de la base también protege frente a registros concurrentes. La contraseña es obligatoria, tiene al menos 8 caracteres y como máximo 72 bytes UTF-8, límite de BCrypt; este límite en bytes puede representar menos caracteres para texto no ASCII.

Se guarda exclusivamente un hash generado por `BCryptPasswordEncoder`, con su coste predeterminado de 10 y una sal aleatoria. La respuesta `201 Created` contiene solo información pública, por ejemplo `{"id":1,"nombre":"Ana","email":"ana@example.test"}`; no contiene contraseña ni hash. Los datos inválidos producen `400` y un correo duplicado `409`. Los errores tratados usan JSON con `estado`, `mensaje` y `campos`.

### Login y JWT

`POST /api/auth/login` es público. Ejemplo de cuerpo JSON (sustituye el marcador por la contraseña del registro):

```json
{"email":"ana@example.test", "password":"<contraseña-elegida>"}
```

Se normaliza y valida el correo igual que en el registro. El servicio existente verifica la contraseña con `PasswordEncoder.matches`; contraseña incorrecta y usuario inexistente producen el mismo error genérico `401`. Un cuerpo inválido produce `400`. Si las credenciales son correctas, se devuelve `200` y `Cache-Control: no-store`:

```json
{"accessToken":"<JWT-firmado>", "tokenType":"Bearer", "expiresIn":3600}
```

Un JWT es un documento compacto con claims firmados; su contenido no está cifrado. El token contiene `sub` (ID inmutable del usuario), `iss` (emisor configurado), `iat` (emisión) y `exp` (expiración). Dura 3600 segundos. Spring Security/Nimbus lo firma con RS256 y valida la firma usando la clave pública, el algoritmo permitido, el emisor, los tiempos y la estructura del ID. Después consulta que el usuario siga existiendo y crea un principal `IdentidadUsuario`. No se guardan tokens ni identidades en sesiones.

En cada petición protegida envía la cabecera:

```http
Authorization: Bearer <JWT-firmado>
```

Un token ausente, malformado, vencido, firmado con otra clave o con claims inválidos produce `401`; una ruta no autorizada para una identidad autenticada produce `403`. Las respuestas de error usan `estado`, `mensaje` y `campos`, sin credenciales. Protege el token como una credencial: quien lo posea puede utilizarlo hasta su vencimiento. No hay renovación ni revocación individual de tokens en esta etapa; eliminar al usuario impide su uso y cambiar la clave invalida los tokens anteriores.

## CRUD de materias

| Método y ruta | Resultado exitoso |
| --- | --- |
| `GET /api/materias?page=0&size=20` | 200, página de materias propias |
| `GET /api/materias/{id}` | 200, materia propia |
| `POST /api/materias` | 201, materia creada y cabecera `Location` |
| `PUT /api/materias/{id}` | 200, materia actualizada |
| `DELETE /api/materias/{id}` | 204, sin contenido |

El DTO de entrada para creación y actualización admite `nombre`, `docente` y `semestre`:

```json
{"nombre":"Álgebra", "docente":"Ana Pérez", "semestre":1}
```

`nombre` y `docente` son obligatorios, no pueden estar en blanco y admiten hasta 255 caracteres. `semestre` es obligatorio, entero y debe estar entre 1 y 12. El DTO de respuesta incluye únicamente información pública:

```json
{"id":1, "nombre":"Álgebra", "docente":"Ana Pérez", "semestre":1}
```

La página empieza en 0 y el tamaño predeterminado es 20; `size` debe estar entre 1 y 100. El listado se ordena por ID ascendente y devuelve `contenido`, `pagina`, `tamano`, `totalElementos` y `totalPaginas`. Los datos o parámetros inválidos producen 400; un ID inexistente o de otra persona produce 404. El manejador de errores del controlador devuelve JSON con `estado`, `mensaje` y `campos` para errores de validación y de negocio tratados.

El propietario se obtiene exclusivamente de una identidad autenticada confiable de Spring Security, no del JSON ni de parámetros. El servicio verifica que el usuario exista y consulta cada materia por ID y propietario. Los campos adicionales del JSON no pueden asignar un propietario. No se devuelve el usuario ni su `passwordHash`.

### Estado de seguridad

Spring Security permite anónimamente solo `POST /api/auth/register` y `POST /api/auth/login`; exige autenticación para materias y deniega por defecto otras rutas. La configuración es stateless y desactiva login por formulario y HTTP Basic. CSRF está deshabilitado porque se utilizan tokens Bearer en cabeceras y no cookies ni sesión; esta decisión deberá revisarse si se incorpora autenticación mediante cookies.

## Esquema MySQL

En MySQL Workbench, conecta a la instancia local y comprueba `SELECT VERSION();`, `SHOW DATABASES LIKE 'araj_act4_dev';` y, si existe, `USE araj_act4_dev; SHOW TABLES;`. Si faltan ambas tablas y tienes autorización para crearlas, selecciona `araj_act4_dev` como esquema predeterminado, abre `D:\Spring\act4\database\schema.sql` con **File > Open SQL Script**, comprueba el esquema seleccionado y ejecuta el script completo con el botón del rayo. Si falta la base, solicita autorización antes de crearla. Si existe alguna tabla, no vuelvas a ejecutar el script completo: revisa primero el esquema existente para evitar cambios no deseados. Requiere MySQL 8.0.16 o posterior para que se aplique la restricción `CHECK` de semestre. No se ejecuta automáticamente al iniciar la aplicación. Usa una cuenta dedicada para la aplicación, no `root` como usuario permanente.

## Pruebas de la API con Bruno

1. Instala Bruno Desktop desde [usebruno.com/downloads](https://www.usebruno.com/downloads) y abre la carpeta `D:\Spring\act4\bruno` mediante **Open Collection**. La colección usa el formato `.bru` y se ha ejecutado con Bruno CLI 4.3.0.
2. Arranca la API local con MySQL y selecciona el entorno **Local**. Comprueba que `baseUrl` sea `http://localhost:8088`.
3. En la configuración de ese entorno abre la pestaña **Secrets** e introduce localmente `passwordPrueba`: al menos 8 caracteres, hasta 72 bytes UTF-8. Bruno guarda el valor del secreto en su almacén local, no en el `.bru`. No escribas contraseñas ni tokens en variables ordinarias, capturas o reportes compartidos. El secreto del entorno **DigitalOcean** es independiente.
4. Ejecuta **01 - Registrar usuario** y luego **02 - Iniciar sesión**. El registro genera un correo único `@example.test` y lo guarda en la variable de ejecución `emailPrueba`; el login guarda el JWT en memoria como `accessToken`. No se llama a `setEnvVar`, por lo que el token no se escribe en el archivo de entorno. El token vence a los 3600 segundos.
5. Ejecuta las peticiones de **02 - Materias** en orden: listar, crear, consultar, actualizar, paginar y eliminar. La creación guarda el ID de la respuesta en `materiaId`. Actualizar y eliminar comprueban que ese ID fue creado por el flujo con el mismo entorno y token. Finalmente ejecuta **03 - Seguridad y validaciones**, incluida la consulta de la materia eliminada.

Para ejecutar todo, usa el **Runner** de la colección, selecciona **Local**, incluye las tres carpetas y ejecuta en orden secuencial, sin paralelismo. Cada ejecución registra un usuario nuevo y crea y elimina una materia de prueba; la API no proporciona eliminación de usuarios, por lo que el usuario queda en la base. Si se interrumpe antes del DELETE puede quedar también la materia de prueba. El registro, la creación, la actualización y la eliminación modifican la base; los registros inválido y duplicado y el POST de semestre inválido esperan ser rechazados sin crear datos. Login y GET son de solo lectura para los datos de la API.

Las peticiones pueden ejecutarse individualmente en la misma sesión, respetando sus dependencias: registro antes de login; login antes de las peticiones protegidas; crear antes de consultar, actualizar o eliminar; eliminar antes de verificar el 404. Las variables de ejecución se pierden al cerrar Bruno; no sustituyas manualmente `materiaId` para operar sobre datos ajenos. Si renuevas el login, crea otra materia antes de actualizar o borrar, porque la protección vincula el ID al token de la creación. No cambies de entorno durante el flujo. El ID máximo usado para la prueba de inexistencia solo se consulta; si alguna vez existe y pertenece al usuario de prueba, esa comprobación no será válida.

La carpeta también incluye el entorno **DigitalOcean**, con `http://204.48.17.197:8087`, reservado para más adelante. La colección no contacta ese destino mientras se seleccione Local. La URL es HTTP, no HTTPS: no envíes contraseñas ni JWT por redes no confiables; configura transporte seguro antes de utilizarla. El VPS todavía no contiene necesariamente esta versión.

Como alternativa, Bruno CLI puede leer la contraseña de prueba de la variable de proceso `BRUNO_PASSWORD`, sin guardar su valor en la colección. Desde `bruno/`, después de configurarla en el entorno del proceso, ejecuta `bru run -r --env Local --bail`. No uses `--verbose` ni reportes con cuerpos o cabeceras si pueden contener credenciales. Los resultados anteriores de CLI no sustituyen revisar las pruebas en Bruno Desktop cuando se desee verificar la interfaz.

## Etapas futuras

1. Revisar gestión y rotación de claves, limitación de intentos, HTTPS y demás medidas antes de producción.
2. Preparar y validar el despliegue por separado.
