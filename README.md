# Actividad 4

Proyecto de la Actividad 4, preparado para desarrollar una aplicación Spring Boot por etapas. Cuenta con entidades y repositorios para usuarios y materias; todavía no se implementan endpoints, CRUD ni autenticación JWT.

## Tecnologías

- Java 17 como versión objetivo y Maven Wrapper.
- Spring Boot 4.1.2: Spring Web MVC, Data JPA, Security, OAuth2 Resource Server y Bean Validation.
- MySQL Connector/J y herramientas de prueba de Spring Boot y Spring Security.

## Configuración local

Configura las variables de entorno `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` antes de iniciar la aplicación con una base de datos MySQL. Si no se define `DB_URL`, se utiliza `jdbc:mysql://localhost:3306/araj_act4_dev`. No guardes credenciales en el repositorio. Hibernate valida el esquema existente (`ddl-auto=validate`), pero no crea tablas.

El puerto predeterminado es 8088. Para desplegar en el VPS puede sobrescribirse, por ejemplo, con la variable de entorno `SERVER_PORT=8087`, sin modificar el código. Spring Security conserva su configuración predeterminada; la autenticación JWT se abordará posteriormente.

Para compilar y ejecutar las pruebas en Windows:

```powershell
.\mvnw.cmd verify
```

Las pruebas de persistencia utilizan H2 exclusivamente en memoria, ejecutan `database/schema.sql` en modo de compatibilidad MySQL y validan los mapeos con Hibernate. No acceden a la base MySQL local y no sustituyen una verificación en MySQL 8 real. La prueba inicial comprueba la anotación de la clase principal sin levantar el contexto.

## Esquema MySQL

En MySQL Workbench, conecta a la instancia local, selecciona la base `araj_act4_dev`, abre `database/schema.sql` y revisa y ejecuta el script sobre esa base. Si la base no existe, créala previamente desde Workbench. El script crea tablas y falla si ya existen: revisa cualquier tabla existente antes de ejecutarlo. Requiere MySQL 8.0.16 o posterior para que se aplique la restricción `CHECK` de semestre. No se ejecuta automáticamente al iniciar la aplicación.

## Etapas futuras

1. Implementar el CRUD y sus pruebas.
2. Configurar autenticación y autorización con JWT.
3. Preparar y validar el despliegue.
