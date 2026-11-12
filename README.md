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

Spring Security conserva su configuración predeterminada y el CRUD exige un principal que implemente `IdentidadUsuario` con el ID de un usuario existente. Ningún mecanismo de autenticación real lo proporciona todavía: el usuario predeterminado de Spring no permite utilizar este CRUD. Las pruebas crean el principal solo en el contexto de prueba. Falta implementar registro, login y JWT, y conectar la identidad validada al principal; por ahora no se puede usar el CRUD con JWT ni se debe considerar listo para producción.

Las solicitudes anónimas probadas reciben 401; para escrituras, la protección CSRF predeterminada puede responder 403 antes de la autenticación si falta el token CSRF. Las respuestas generadas por los filtros de seguridad, incluidas esas respuestas 401/403, no utilizan necesariamente el formato JSON del manejador del controlador. Las pruebas de escritura incluyen un token CSRF de prueba; no se desactivó la protección.

## Esquema MySQL

En MySQL Workbench, conecta a la instancia local, selecciona la base `araj_act4_dev`, abre `database/schema.sql` y revisa y ejecuta el script sobre esa base. Si la base no existe, créala previamente desde Workbench. El script crea tablas y falla si ya existen: revisa cualquier tabla existente antes de ejecutarlo. Requiere MySQL 8.0.16 o posterior para que se aplique la restricción `CHECK` de semestre. No se ejecuta automáticamente al iniciar la aplicación.

## Etapas futuras

1. Implementar registro y login seguros.
2. Configurar autenticación y autorización con JWT y conectar el principal.
3. Preparar y validar el despliegue.
