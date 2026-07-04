# demo-auth — Spring Boot + Spring Security + AWS Cognito

Proyecto demo con **dos endpoints**:

| Método | Ruta           | Autenticación              |
|--------|----------------|----------------------------|
| `GET`  | `/api/public`  | ❌ No requiere token       |
| `GET`  | `/api/private` | ✅ Requiere JWT de Cognito |

La idea: obtener un **access token** de AWS Cognito desde **Postman** (flujo *Authorization Code* con la *Hosted UI*) y usar ese token como `Authorization: Bearer <token>` para entrar al endpoint protegido.

Stack: **Kotlin**, **Java 21 (JVM/toolchain)**, **Gradle (Kotlin DSL)**, **Spring Boot 3.5.x**, **application.yml**. Group/artifact: `com.pucetec` / `demo-auth` → paquete base `com.pucetec.demoauth`.

> ℹ️ Kotlin/Java no admiten guiones en los nombres de paquete, por eso `demo-auth` se convierte en `demoauth`. El artefacto y el `rootProject.name` sí conservan el guion.

---

## Marco teórico — qué vamos a hacer y por qué

Antes de tocar código conviene entender **qué problema resolvemos** y **qué piezas** entran en juego. La meta práctica es mínima: un endpoint al que solo se entra con un *token* válido. Pero detrás de ese token está un modelo de seguridad estándar de la industria —**OAuth 2.0** y **OpenID Connect (OIDC)**— que vale la pena comprender, porque es el mismo que usan Google, Microsoft, GitHub, etc.

### Autenticación vs. Autorización

Dos conceptos que se confunden constantemente:

| Concepto | Pregunta que responde | Ejemplo |
|----------|------------------------|---------|
| **Autenticación** (*AuthN*) | ¿**Quién** eres? | Iniciar sesión con usuario + contraseña (+ MFA) |
| **Autorización** (*AuthZ*) | ¿**Qué** puedes hacer? | "Este usuario puede leer `/api/private`" |

En este demo, **Cognito** se encarga de la **autenticación** (el login en su *Hosted UI*) y nuestra app hace una **autorización** mínima: *"si traes un token válido emitido por mi Cognito, puedes entrar a `/api/private`"*.

### El problema: identidad delegada

La forma "ingenua" sería que cada aplicación guarde usuarios y contraseñas y valide el login por su cuenta. Eso no escala y es peligroso: cada app se vuelve un lugar donde se pueden filtrar contraseñas, y el usuario termina con mil cuentas distintas.

La solución moderna es **delegar la identidad** a un servicio especializado (un *Identity Provider* / *Authorization Server*). La app **nunca ve la contraseña**: confía en un **token firmado** que ese servicio emite tras autenticar al usuario. Aquí ese servicio es **AWS Cognito**.

```
Antes (ingenuo):     Usuario → contraseña → [App valida contra su propia BD]
Ahora (delegado):    Usuario → contraseña → [Cognito] → token → [App solo valida el token]
```

### OAuth 2.0 y OpenID Connect en una frase

- **OAuth 2.0** es un protocolo de **autorización**: define cómo una aplicación obtiene un *access token* para acceder a recursos protegidos, sin manejar la contraseña del usuario.
- **OpenID Connect (OIDC)** es una capa de **autenticación** *encima* de OAuth 2.0: añade un *id_token* que dice **quién** es el usuario (su identidad). Cognito implementa ambos.

### Los cuatro roles de OAuth 2.0 (mapeados a este demo)

OAuth define cuatro actores. Identificarlos es la clave para no perderse:

| Rol OAuth | Qué es | En este demo |
|-----------|--------|--------------|
| **Resource Owner** | El dueño de la identidad/los datos (una persona) | El usuario que inicia sesión |
| **Client** | La app que quiere acceder en nombre del usuario | **Postman** (y el *App Client* de Cognito que lo representa) |
| **Authorization Server** | Quien autentica al usuario y **emite tokens** | **AWS Cognito** (el *User Pool*) |
| **Resource Server** | La API protegida que **consume y valida** el token | Nuestra **app Spring Boot** (`/api/private`) |

> 🧩 Spring lo llama *Resource Server* precisamente porque su único trabajo es **proteger recursos** validando tokens que **otro** (Cognito) emitió. No hace login, no guarda usuarios, no genera tokens.

### El flujo *Authorization Code* (el que usaremos)

¿Por qué tantos pasos y no simplemente "manda usuario y contraseña a la API"? Porque con *Authorization Code* **la contraseña solo la ve Cognito**: ni Postman ni nuestra API la tocan jamás. El navegador redirige al usuario al Authorization Server, este lo autentica y devuelve primero un **código de un solo uso** que luego se canjea por los tokens. Ese paso intermedio del código evita que los tokens viajen expuestos en la URL del navegador.

```
1. Postman abre el navegador →  Cognito Hosted UI  (Auth URL: /oauth2/authorize)
2. El usuario inicia sesión   →  Cognito verifica credenciales
3. Cognito redirige a la Callback URL con un  ?code=XXXX  (código de un solo uso)
4. Postman canjea el code     →  Cognito /oauth2/token   (Access Token URL)
5. Cognito responde con:  access_token  +  id_token  (JWT firmados)
6. Postman usa el access_token →  Authorization: Bearer <JWT>  hacia /api/private
```

> 🔐 **PKCE** (*Proof Key for Code Exchange*) es una extensión que blinda este flujo para *clients* públicos (sin secreto), evitando que alguien que intercepte el `code` pueda canjearlo. Por eso Postman ofrece la opción *Authorization Code (With PKCE)*.

### Qué es un JWT (y por qué se puede validar sin llamar a Cognito)

Un **JWT** (*JSON Web Token*) es una cadena con **tres partes separadas por puntos**: `header.payload.signature`, cada una en Base64URL.

```
eyJhbGciOiJSUzI1NiIsImtpZCI6Ii4uLiJ9 . eyJzdWIiOiJhMWIyIiwidG9rZW5fdXNlIjoiYWNjZXNzIn0 . <firma>
└──────────── header ───────────────┘ └─────────────── payload (claims) ───────────────┘ └ firma ┘
```

- **Header:** algoritmo de firma (`RS256`) y `kid` (qué clave se usó).
- **Payload:** los **claims** (afirmaciones) sobre el usuario y el token: `sub` (identificador), `iss` (emisor), `exp` (expiración), `token_use`, `scope`, etc.
- **Signature:** la firma criptográfica que garantiza que **nadie alteró** el contenido.

Lo potente: el token es **autocontenido**. Lleva toda la información necesaria, así que el Resource Server **no necesita consultar una base de datos ni llamar a Cognito en cada request** para saber quién es el usuario. Solo verifica la firma. Esto es lo que llamamos **stateless**.

### Firma asimétrica y JWKS — la pieza que hace que todo funcione

Cognito firma cada token con una **clave privada** que solo él conoce. Cualquiera puede **verificar** esa firma con la **clave pública** correspondiente. Esas claves públicas se publican en un endpoint estándar llamado **JWKS** (*JSON Web Key Set*).

```
Cognito  ──(firma con clave PRIVADA)──►   JWT
                                            │
Nuestra app ──(descarga claves PÚBLICAS del JWKS)──► verifica la firma del JWT
```

Por eso en `application.yml` solo configuramos un dato —el `issuer-uri`— y Spring se encarga del resto:

1. Toma el `issuer-uri` (la URL del User Pool).
2. Le agrega `/.well-known/openid-configuration` → **OIDC Discovery**: Cognito le dice dónde está su JWKS.
3. Descarga las claves públicas del JWKS (y las cachea).
4. Con esas claves valida en cada request: **firma** + **emisor (`iss`)** + **expiración (`exp`)**.

**No guardamos ninguna clave a mano.** Si Cognito rota sus claves, Spring vuelve a leer el JWKS automáticamente.

### `access_token` vs. `id_token` (no son lo mismo)

OIDC entrega dos tokens y cumplen propósitos distintos. Confundirlos es el error más común:

| Token | Para qué sirve | Quién lo consume |
|-------|----------------|------------------|
| **`access_token`** | **Autorizar el acceso a una API** (lleva `scope`, `token_use: access`) | El **Resource Server** (nuestra API) |
| **`id_token`** | Decir **quién es el usuario** (lleva `email`, `name`, `profile`…) | El **Client** (Postman / un frontend) |

> ⚠️ En este demo **siempre usamos el `access_token`** para llamar a `/api/private`. El `id_token` es para que el *cliente* sepa los datos del usuario, no para autorizar la API. (Nota Cognito-específica: su `access_token` **no** incluye `email`/`name`; esos perfiles viven en el `id_token`.)

### Por qué desactivamos sesiones y CSRF

Como cada request **trae su propio token** en el header `Authorization`, el servidor **no necesita memoria** (`SessionCreationPolicy.STATELESS`): no hay cookies de sesión ni estado entre peticiones. Y como no usamos cookies, el ataque **CSRF** —que abusa de cookies que el navegador adjunta solo— **no aplica**, por eso lo desactivamos. Esto es el patrón estándar de una **API REST stateless basada en tokens** (ver detalle en el `SecurityConfig` del paso 3.2).

### Resumen de la arquitectura

```
┌──────────┐   login    ┌────────────────────┐   emite JWT firmado
│  Usuario │──────────► │  Cognito (Auth      │──────────────────────┐
└──────────┘            │  Server / IdP)      │                      │
     ▲                  └────────────────────┘                       ▼
     │ navegador                                              ┌───────────────┐
     │ (Hosted UI)                                            │  access_token │
     ▼                                                        └───────┬───────┘
┌──────────┐   Bearer <access_token>                                 │
│ Postman  │──────────────────────────────────────────────────────►  │
│ (Client) │                                                          ▼
└──────────┘                                          ┌───────────────────────────────┐
                                                      │  Spring Boot (Resource Server) │
                                                      │  valida firma+iss+exp vía JWKS │
                                                      │  200 si OK · 401 si falla      │
                                                      └───────────────────────────────┘
```

Con este modelo en mente, el resto de la guía es **cablear** estas piezas: crear el proyecto (paso 1–3), configurar Cognito como Authorization Server (paso 4), levantar el Resource Server (paso 5) y obtener/usar el token desde el Client (paso 6–7).

---

## 0. Requisitos previos

- **JDK 21** (si usas otra versión, el *toolchain* de Gradle intentará descargar el 21 automáticamente).
- **Postman**.
- Un **User Pool de Cognito ya creado** con un **App Client** (asumido según el alcance de esta guía).
- El **dominio Hosted UI** del User Pool habilitado (lo configuramos en el paso 4).

Datos de Cognito que vas a necesitar a mano:

| Dato                 | Dónde se ve en la consola de Cognito                          | Ejemplo                              |
|----------------------|----------------------------------------------------------------|--------------------------------------|
| **Región**           | Esquina superior derecha de la consola AWS                     | `us-east-1`                          |
| **User Pool ID**     | User pool → *Overview*                                          | `us-east-1_AbCdEf123`                |
| **App client ID**    | User pool → *App integration* → *App clients*                  | `1h57kf5...`                         |
| **App client secret**| (Solo si el client es *confidential*) misma pantalla           | `abc123...`                          |
| **Dominio Hosted UI**| User pool → *App integration* → *Domain*                       | `demo-auth.auth.us-east-1.amazoncognito.com` |

> ✅ ¿No quieres crear tu propio pool? Usa el **User Pool de prueba ya configurado** de la sección 🔑 (justo abajo): trae todos estos datos listos.

---

## 🔑 User Pool de Cognito de prueba (úsalo directamente)

Para que **no tengas que crear tu propio User Pool**, puedes usar este **User Pool dummy ya configurado**. Es **solo para práctica** (no pongas datos reales; el `client_secret` es desechable).

| Dato | Valor |
|------|-------|
| Región | `us-east-1` |
| User Pool ID | `us-east-1_yzwNALI2A` |
| **`issuer-uri`** (para `application.yml`) | `https://cognito-idp.us-east-1.amazonaws.com/us-east-1_yzwNALI2A` |
| Dominio Hosted UI | `https://us-east-1yzwnali2a.auth.us-east-1.amazoncognito.com` |
| App client ID | `3gv2oqe4niko3s47srn1kitsk6` |
| App client secret *(client confidential)* | `14qdd388f1j6fge52el3l5r2ouvcg5sperlno3701t2jj1chgeiu` |
| Scopes habilitados | `email openid phone` |
| Redirect URI registrado | `https://d84l1y8p4kdic.cloudfront.net` *(managed login de Cognito)* |
| Authorize endpoint | `https://us-east-1yzwnali2a.auth.us-east-1.amazoncognito.com/oauth2/authorize` |
| Token endpoint | `https://us-east-1yzwnali2a.auth.us-east-1.amazoncognito.com/oauth2/token` |

### En tu `application.yml`

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://cognito-idp.us-east-1.amazonaws.com/us-east-1_yzwNALI2A
```

### Obtener un token manualmente (Authorization Code, paso a paso)

El `redirect_uri` registrado en este pool es la *managed login* de Cognito
(`https://d84l1y8p4kdic.cloudfront.net`), **no** el callback de Postman. Por eso el camino más
directo es **canjear el código a mano**:

**1) Inicia sesión en la Hosted UI** — abre esta URL en el navegador (si no tienes usuario, regístrate ahí mismo con *Sign up*):

```
https://us-east-1yzwnali2a.auth.us-east-1.amazoncognito.com/login?client_id=3gv2oqe4niko3s47srn1kitsk6&response_type=code&scope=email+openid+phone&redirect_uri=https%3A%2F%2Fd84l1y8p4kdic.cloudfront.net
```

**2) Copia el `code`** — tras el login, Cognito te redirige a:

```
https://d84l1y8p4kdic.cloudfront.net/?code=XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
```

Copia el valor de `code` de la barra de direcciones.
⚠️ **Es de un solo uso y dura pocos minutos.** Si caduca, vuelve a iniciar sesión para obtener uno nuevo.

**3) Canjea el `code` por los tokens** — pega tu code en `--data-urlencode 'code=...'`:

```bash
curl --location 'https://us-east-1yzwnali2a.auth.us-east-1.amazoncognito.com/oauth2/token' \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'grant_type=authorization_code' \
  --data-urlencode 'client_id=3gv2oqe4niko3s47srn1kitsk6' \
  --data-urlencode 'client_secret=14qdd388f1j6fge52el3l5r2ouvcg5sperlno3701t2jj1chgeiu' \
  --data-urlencode 'code=<PEGA_TU_CODE_AQUI>' \
  --data-urlencode 'redirect_uri=https://d84l1y8p4kdic.cloudfront.net'
```

La respuesta es un JSON con `access_token`, `id_token`, `refresh_token` y `expires_in`.
(El header `Cookie: XSRF-TOKEN=...` que aparece en algunos ejemplos **no es necesario** para este POST; puedes omitirlo.)

**4) Llama a tu API** con el `access_token`:

```bash
TOKEN="eyJ..."   # access_token de la respuesta anterior
curl -i http://localhost:8080/api/private -H "Authorization: Bearer $TOKEN"
```

> 💡 **En Postman:** pega ese `access_token` directamente en *Authorization → Bearer Token*. El flujo automático *OAuth 2.0* (paso 6) solo funciona si `https://oauth.pstmn.io/v1/callback` está entre los *callback URLs* del client; en este pool de prueba el callback registrado es el de la *managed login*, así que el **método manual de arriba es el camino seguro**.

---

## 1. Crear el proyecto en Spring Initializr

Entra a <https://start.spring.io> y configura:

| Campo             | Valor                |
|-------------------|----------------------|
| **Project**       | Gradle - Kotlin      |
| **Language**      | Kotlin               |
| **Spring Boot**   | 3.5.x (la última 3.5 estable) |
| **Group**         | `com.pucetec`        |
| **Artifact**      | `demo-auth`          |
| **Name**          | `demo-auth`          |
| **Package name**  | `com.pucetec.demoauth` |
| **Packaging**     | Jar                  |
| **Java**          | 21                   |

### Dependencias (botón *Add Dependencies*)

1. **Spring Web** — `spring-boot-starter-web`
   Sirve para exponer los endpoints REST y levantar el servidor embebido (Tomcat). Sin esto no hay controladores HTTP.

2. **OAuth2 Resource Server** — `spring-boot-starter-oauth2-resource-server`
   Convierte la app en un *Resource Server*: recibe un JWT en el header `Authorization`, **descarga el JWKS de Cognito** y valida la firma, el emisor (`iss`) y la expiración (`exp`) del token. Es la pieza que hace la autenticación con Cognito.

> ¿Y "Spring Security"? **No hace falta marcarlo aparte.** `OAuth2 Resource Server` ya depende de `spring-boot-starter-security` de forma transitiva, así que Spring Security entra al classpath automáticamente. (Si lo marcas igual, no pasa nada; queda redundante.)

> 🟣 Al elegir **Language: Kotlin**, Spring Initializr añade solo los plugins `kotlin("jvm")` y `kotlin("plugin.spring")` y las dependencias `jackson-module-kotlin` (serialización JSON de tipos Kotlin) y `kotlin-reflect` (requerida por Spring). No tienes que marcarlas a mano.

Pulsa **GENERATE**, descomprime el `.zip` y ábrelo en tu IDE. Esta misma guía ya trae el proyecto generado y configurado, así que puedes saltar al paso 2 si usas este repo.

---

## 2. Estructura del proyecto

```
demo-auth/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew  /  gradlew.bat
├── gradle/wrapper/
└── src/
    └── main/
        ├── kotlin/com/pucetec/demoauth/
        │   ├── DemoAuthApplication.kt          # arranque
        │   ├── config/SecurityConfig.kt        # reglas de seguridad
        │   └── controller/DemoController.kt    # los 2 endpoints
        └── resources/
            └── application.yml                 # issuer-uri de Cognito
```

---

## 3. Código

### 3.1 `build.gradle.kts`

```kotlin
plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.spring") version "2.1.21"          // hace `open` las clases @Component/@Bean
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.pucetec"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)   // JVM 21 (Kotlin compila contra esta)
    }
}

repositories { mavenCentral() }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions { freeCompilerArgs.addAll("-Xjsr305=strict") }
}

tasks.withType<Test> { useJUnitPlatform() }
```

> El plugin `kotlin("plugin.spring")` (alias de *all-open*) abre automáticamente las clases anotadas con `@Configuration`, `@Component`, etc., porque en Kotlin las clases son `final` por defecto y Spring necesita poder extenderlas (proxies).

### 3.2 `SecurityConfig.java` — el corazón de la autenticación

```kotlin
package com.pucetec.demoauth.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/public/**").permitAll()   // público
                    .anyRequest().authenticated()                    // todo lo demás: con token
            }
            .oauth2ResourceServer { oauth2 -> oauth2.jwt { } }       // valida JWT (defaults)
        return http.build()
    }
}
```

**Qué hace cada parte:**
- `csrf { it.disable() }`: **CSRF** (*Cross-Site Request Forgery*) es un ataque donde una web maliciosa hace que el navegador de la víctima envíe peticiones a un sitio donde ya está autenticada, abusando de credenciales que el navegador adjunta **solo** (cookies de sesión). La protección CSRF aplica cuando te autenticas con **cookies/sesión**; aquí no usamos cookies sino un **JWT en el header `Authorization`** (que el navegador no envía solo en peticiones cruzadas), así que el ataque no aplica y se desactiva — estándar en APIs REST stateless con token.
- `STATELESS`: el servidor no guarda sesión; cada request debe traer su propio token.
- `permitAll()` para `/api/public/**` y `authenticated()` para el resto.
- `oauth2ResourceServer { ... jwt { } }`: activa la validación de JWT. **De dónde valida** lo define el `issuer-uri` del `application.yml`.

### 3.3 `DemoController.java` — los dos endpoints

```kotlin
package com.pucetec.demoauth.controller

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class DemoController {

    @GetMapping("/public")
    fun publicEndpoint(): Map<String, Any?> = linkedMapOf(
        "message" to "Este endpoint es público, no requiere token.",
        "authenticated" to false,
    )

    @GetMapping("/private")
    fun privateEndpoint(@AuthenticationPrincipal jwt: Jwt): Map<String, Any?> = linkedMapOf(
        "message" to "Token válido. Acceso autorizado.",
        "authenticated" to true,
        "subject" to jwt.subject,                       // claim "sub"
        "tokenUse" to jwt.getClaimAsString("token_use"),
        "scopes" to jwt.getClaimAsString("scope"),
        "claims" to jwt.claims,
    )
}
```

En `/api/private`, Spring ya validó el token **antes** de entrar al método e inyecta el `Jwt` con `@AuthenticationPrincipal`. Devolvemos algunos claims para comprobar que la validación funcionó.

### 3.4 `application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: demo-auth
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://cognito-idp.${AWS_REGION:us-east-1}.amazonaws.com/${COGNITO_USER_POOL_ID:us-east-1_xxxxxxxxx}
```

El `issuer-uri` es el **emisor del User Pool**:
```
https://cognito-idp.<REGION>.amazonaws.com/<USER_POOL_ID>
```
Spring le agrega `/.well-known/openid-configuration`, descubre la URL del JWKS y descarga las claves públicas con las que valida la firma de cada token. **No necesitas guardar ninguna clave manualmente.**

Puedes dejar tus valores fijos en el YAML, o exportarlos como variables de entorno (ver paso 5).

> 🔎 Verifica tu issuer abriendo en el navegador:
> `https://cognito-idp.<REGION>.amazonaws.com/<USER_POOL_ID>/.well-known/openid-configuration`
> Debe devolver un JSON con `issuer`, `jwks_uri`, etc.

---

## 4. Configurar el App Client de Cognito para Postman

En la consola de **Cognito → tu User Pool → App integration**:

### 4.1 Dominio (Hosted UI)
En **Domain**, asegúrate de tener un dominio (Cognito prefix o propio). Ejemplo:
```
https://demo-auth.auth.us-east-1.amazoncognito.com
```

### 4.2 App client → *Hosted UI / OAuth settings* (editar)
Configura exactamente:

- **Allowed callback URLs:** agrega la de Postman →
  ```
  https://oauth.pstmn.io/v1/callback
  ```
- **OAuth 2.0 grant types:** marca **Authorization code grant**.
- **OpenID Connect scopes:** marca **openid**, **email**, **profile**.
- Guarda los cambios.

### 4.3 ¿El App client tiene *client secret*?
- Si tu app client es **confidential** (con secret), tendrás que ingresar el **Client Secret** en Postman.
- Si es **public** (sin secret), lo dejas vacío.

> 💡 Anota: **App client ID**, **App client secret** (si aplica) y el **dominio**. Los usarás en Postman.

---

## 5. Ejecutar la aplicación

Desde la raíz del proyecto, pasando tus datos de Cognito por variables de entorno:

```bash
export AWS_REGION="us-east-1"
export COGNITO_USER_POOL_ID="us-east-1_AbCdEf123"

./gradlew bootRun
```

(En Windows PowerShell: `$env:AWS_REGION="us-east-1"` etc.)

La app queda en `http://localhost:8080`.

**Prueba rápida del endpoint público (sin token):**
```bash
curl -i http://localhost:8080/api/public
# 200 OK -> {"message":"...","authenticated":false}
```

**El endpoint protegido sin token debe dar 401:**
```bash
curl -i http://localhost:8080/api/private
# 401 Unauthorized
```

---

## 6. Obtener el token en Postman (Authorization Code + Hosted UI)

> 🔑 Si estás usando el **User Pool de prueba** (sección 🔑), su callback es la *managed login*
> y **no** `oauth.pstmn.io`, por lo que el flujo automático de abajo no aplicará: usa el
> **método manual con `curl`** de esa sección y pega el `access_token` en *Authorization → Bearer Token*.
> Los pasos siguientes son para cuando configuras **tu propio** App Client con el callback de Postman.

1. Abre Postman y crea un request `GET http://localhost:8080/api/private`.
2. Ve a la pestaña **Authorization** → **Type: OAuth 2.0**.
3. En **Configure New Token**, completa:

   | Campo                     | Valor                                                                 |
      |---------------------------|-----------------------------------------------------------------------|
   | **Token Name**            | `cognito` (cualquiera)                                                 |
   | **Grant Type**            | `Authorization Code` (o *Authorization Code (With PKCE)*)             |
   | **Callback URL**          | `https://oauth.pstmn.io/v1/callback` (debe coincidir con Cognito)     |
   | **Auth URL**              | `https://<DOMINIO>/oauth2/authorize`                                   |
   | **Access Token URL**      | `https://<DOMINIO>/oauth2/token`                                       |
   | **Client ID**             | tu **App client ID**                                                   |
   | **Client Secret**         | tu **App client secret** (vacío si el client no tiene secret)         |
   | **Scope**                 | `openid email profile`                                                 |
   | **Client Authentication** | `Send as Basic Auth header`                                            |

   Donde `<DOMINIO>` es, por ejemplo, `demo-auth.auth.us-east-1.amazoncognito.com`.

4. Pulsa **Get New Access Token**.
5. Se abre el navegador con la **Hosted UI de Cognito** → inicia sesión (o regístrate) con un usuario del pool.
6. Cognito redirige a Postman, que captura los tokens. Verás **`access_token`** e **`id_token`**.
7. Pulsa **Use Token**. Postman tomará el **access_token** y lo pondrá como `Authorization: Bearer <token>`.

> ⚠️ **Usa el `access_token`** (no el `id_token`) para llamar a la API. El backend valida el access token de Cognito (`token_use = access`).

---

## 7. Probar los endpoints

Con el token ya cargado en Postman, envía el request:

```
GET http://localhost:8080/api/private
Authorization: Bearer <access_token>
```

Respuesta esperada (`200 OK`):
```json
{
  "message": "Token válido. Acceso autorizado.",
  "authenticated": true,
  "subject": "a1b2c3d4-....",
  "tokenUse": "access",
  "scopes": "openid email profile",
  "claims": { "...": "..." }
}
```

Equivalente con `curl` (pega tu token):
```bash
TOKEN="eyJ..."     # access_token copiado de Postman
curl -i http://localhost:8080/api/private -H "Authorization: Bearer $TOKEN"
```

Y el público sigue funcionando sin token:
```bash
curl -i http://localhost:8080/api/public
```

---

## 8. ¿Cómo funciona la validación? (resumen)

```
Postman → Hosted UI Cognito (login) → access_token (JWT firmado por Cognito)
   │
   └── GET /api/private  con  Authorization: Bearer <JWT>
            │
            ▼
   Spring Security (Resource Server)
     1. Lee el issuer-uri del application.yml
     2. Descarga el JWKS de Cognito (claves públicas)
     3. Valida firma + iss + exp del JWT
     4. Si todo OK → 200 y entra al controller
        Si falla   → 401 Unauthorized
```

---

## 9. Troubleshooting

| Síntoma | Causa probable / solución |
|--------|----------------------------|
| `401` en `/api/private` con header `WWW-Authenticate: Bearer error="invalid_token"` | Token expirado (los access token de Cognito duran ~1h, pide uno nuevo), o `issuer-uri` mal configurado (región / pool id). |
| App no arranca / error al resolver el issuer | Revisa que `AWS_REGION` y `COGNITO_USER_POOL_ID` sean correctos y que el `.../.well-known/openid-configuration` responda en el navegador. |
| Postman: `redirect_mismatch` | La **Callback URL** de Postman (`https://oauth.pstmn.io/v1/callback`) no está en *Allowed callback URLs* del App client. |
| Postman: `invalid_client` al pedir el token | Falta el **Client Secret** (client confidential) o *Client Authentication* mal puesto. Usa `Send as Basic Auth header`. |
| Postman: `invalid_scope` o no aparece el login | Los scopes (`openid email profile`) no están habilitados en el App client, o falta el grant *Authorization code*. |
| Entra con `id_token` "por error" | Usa siempre el **access_token**. Aunque el id_token está firmado por el mismo pool, lo correcto para autorizar la API es el access token. |

---

## Comandos útiles

```bash
./gradlew bootRun     # ejecutar
./gradlew build       # compilar + tests + jar
./gradlew test        # solo tests
```
