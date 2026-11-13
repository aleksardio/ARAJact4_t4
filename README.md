# Actividad 4

Proyecto de la Actividad 4, preparado para desarrollar una aplicación Spring Boot por etapas. Cuenta con entidades, repositorios y CRUD REST de materias. La autenticación JWT aún no está implementada.

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

Las pruebas de persistencia y del controlador utilizan H2 exclusivamente en memoria, ejecutan `database/schema.sql` en modo de compatibilidad MySQL y validan los mapeos con Hibernate. Las pruebas HTTP usan MockMvc y una identidad de prueba en Spring Security. No acceden a la base MySQL local y no sustituyen una verificación en MySQL 8 real.

## Registro y credenciales

`POST /api/auth/register` es público y recibe JSON, por ejemplo:

```json
{"nombre":"Ana", "email":" Ana@example.test ", "password":"una-clave-segura"}
```

El nombre es obligatorio, no puede estar en blanco y admite hasta 255 caracteres. El correo es obligatorio, debe cumplir `@Email`, admite hasta 255 caracteres y se normaliza quitando espacios exteriores y convirtiéndolo a minúsculas antes de validar y guardar. Debe ser único; la restricción UNIQUE de la base también protege frente a registros concurrentes. La contraseña es obligatoria, tiene al menos 8 caracteres y como máximo 72 bytes UTF-8, límite de BCrypt; este límite en bytes puede representar menos caracteres para texto no ASCII.

Se guarda exclusivamente un hash generado por `BCryptPasswordEncoder`, con su coste predeterminado de 10 y una sal aleatoria. La respuesta `201 Created` contiene solo información pública, por ejemplo `{"id":1,"nombre":"Ana","email":"ana@example.test"}`; no contiene contraseña ni hash. Los datos inválidos producen `400` y un correo duplicado `409`. Los errores tratados usan JSON con `estado`, `mensaje` y `campos`.

El servicio interno `AutenticacionService.verificarCredenciales` normaliza el correo mediante `LoginSolicitud` y comprueba la contraseña con `PasswordEncoder.matches`. Si el correo no existe o la contraseña es incorrecta, rechaza ambos casos con el mismo error genérico. Comprobar credenciales **no emite ni valida JWT**, ni inicia una sesión. No existe todavía un endpoint público de login; `POST /api/auth/login` no está implementado.

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

Spring Security permite únicamente el registro anónimo y protege el resto de rutas. La configuración es stateless, desactiva el login por formulario y HTTP Basic y no utiliza sesiones para autenticar. Se desactiva CSRF porque no hay autenticación mediante cookies ni sesión; esta decisión deberá revisarse si se incorpora alguna. Las peticiones anónimas protegidas reciben 401 JSON.

El CRUD exige un principal que implemente `IdentidadUsuario` con el ID de un usuario existente. Ningún mecanismo real lo proporciona todavía; las pruebas crean el principal solo en el contexto de prueba. Por ahora no se puede utilizar el CRUD mediante JWT y la aplicación no está lista para producción.

## Esquema MySQL

En MySQL Workbench, conecta a la instancia local, selecciona la base `araj_act4_dev`, abre `database/schema.sql` y revisa y ejecuta el script sobre esa base. Si la base no existe, créala previamente desde Workbench. El script crea tablas y falla si ya existen: revisa cualquier tabla existente antes de ejecutarlo. Requiere MySQL 8.0.16 o posterior para que se aplique la restricción `CHECK` de semestre. No se ejecuta automáticamente al iniciar la aplicación.

## Etapas futuras

1. Etapa 5: implementar el endpoint de login y la emisión y validación de JWT; conectar la identidad validada a `IdentidadUsuario` sin confiar en datos enviados por el cliente.
2. Revisar gestión de claves, expiración y medidas de protección antes de producción.
3. Preparar y validar el despliegue.
