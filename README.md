# Actividad 4

Proyecto inicial de la Actividad 4, preparado para desarrollar una aplicación Spring Boot por etapas. En esta primera etapa se configura la base del proyecto; todavía no se implementan entidades, endpoints, CRUD ni autenticación JWT.

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

La prueba inicial comprueba la anotación de la clase principal sin levantar el contexto de Spring; así no necesita MySQL, usuarios ni claves JWT. No verifica todavía el arranque de la aplicación ni la conexión a la base de datos.

## Etapas futuras

1. Definir el modelo y la persistencia.
2. Implementar el CRUD y sus pruebas.
3. Configurar autenticación y autorización con JWT.
4. Preparar y validar el despliegue.
