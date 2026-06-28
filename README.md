# ZNSK Shortener URL

Serviço REST de encurtamento de links desenvolvido com Spring Boot. Gera URLs curtas no domínio `https://znsk.uk/` e redireciona o usuário para a URL original, com suporte a expiração configurável.

## Sumário

- [Visão geral](#visão-geral)
- [Stack tecnológica](#stack-tecnológica)
- [Arquitetura](#arquitetura)
- [API](#api)
- [Modelo de dados](#modelo-de-dados)
- [Regras de negócio](#regras-de-negócio)
- [Configuração](#configuração)
- [Como executar](#como-executar)
- [Docker](#docker)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Testes](#testes)

## Visão geral

O serviço expõe dois endpoints principais:

1. **Encurtar** — recebe uma URL longa e retorna uma URL curta.
2. **Redirecionar** — recebe o código curto e redireciona (HTTP 302) para a URL original.

Links podem ter prazo de validade (`1h`, `1d`, `7d` ou `30d`). Links expirados retornam HTTP 410 (Gone) no acesso e são removidos do banco. Um job agendado também limpa links expirados diariamente à meia-noite.

## Stack tecnológica

| Tecnologia | Versão / uso |
|---|---|
| Java | 21 |
| Spring Boot | 3.4.5 |
| Spring Web | REST API |
| Spring Data JPA | persistência |
| PostgreSQL | banco de produção |
| H2 | dependência presente (config local legada no `application.properties`) |
| Lombok | redução de boilerplate nas entidades |
| Maven | build e dependências |

## Arquitetura

```
Cliente (https://znsk.uk)
        │
        ▼
┌───────────────────┐
│   UrlController   │  POST /shorten  ·  GET /{code}
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│    UrlService     │  geração de código · expiração · limpeza
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│ UrlMappingRepository │  JPA
└─────────┬─────────┘
          │
          ▼
     PostgreSQL
```

Camadas:

- **Controller** — recebe requisições HTTP e delega ao serviço.
- **Service** — lógica de encurtamento, verificação de expiração e limpeza.
- **Repository** — acesso ao banco via Spring Data JPA.
- **Domain** — entidade `UrlMapping`.

## API

Base URL de produção: `https://znsk.uk`

CORS habilitado para origem `https://znsk.uk/` nos métodos `GET` e `POST`.

### POST `/shorten`

Cria um link encurtado.

**Request body (JSON):**

```json
{
  "originalUrl": "https://exemplo.com/pagina-longa",
  "expires": "7d"
}
```

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `originalUrl` | string | sim | URL original a ser encurtada |
| `expires` | string | sim | Prazo de validade: `1h`, `1d`, `7d` ou `30d` |

**Resposta — 200 OK:**

```
https://znsk.uk/aBc12X
```

Retorna a URL curta completa como texto plano (não JSON).

### GET `/{code}`

Redireciona para a URL original associada ao código.

| Código HTTP | Situação |
|---|---|
| `302 Found` | Link válido; redireciona para `originalUrl` |
| `404 Not Found` | Código inexistente |
| `410 Gone` | Link expirado; registro removido do banco |

**Exemplo:**

```
GET https://znsk.uk/aBc12X
→ 302 Location: https://exemplo.com/pagina-longa
```

## Modelo de dados

Entidade `UrlMapping` (tabela gerenciada pelo Hibernate com `ddl-auto=update`):

| Campo | Tipo | Descrição |
|---|---|---|
| `code` | `String` (PK) | Código curto de 6 caracteres (Base62) |
| `originalUrl` | `String` | URL de destino |
| `createdAt` | `LocalDateTime` | Data/hora de criação |
| `expiresAt` | `LocalDateTime` | Data/hora de expiração (nullable) |

## Regras de negócio

### Geração de código

- Códigos têm **6 caracteres** usando alfabeto Base62: `a-z`, `A-Z`, `0-9`.
- Colisões são evitadas com loop até encontrar um código único no banco.

### Expiração

| Valor `expires` | Duração |
|---|---|
| `1h` | 1 hora |
| `1d` | 24 horas |
| `7d` | 168 horas (7 dias) |
| `30d` | 720 horas (30 dias) |

Se `expires` não corresponder a nenhum valor acima, `expiresAt` permanece `null` e o link **não expira**.

### Limpeza de links expirados

1. **No acesso** — ao acessar um link expirado, o serviço retorna `410 Gone` e remove o registro.
2. **Job agendado** — diariamente à meia-noite (`cron: 0 0 0 * * *`), remove todos os links com `expiresAt` anterior ao momento atual.

## Configuração

Variáveis de ambiente necessárias (definidas em `application.properties`):

| Variável | Descrição |
|---|---|
| `DATABASE_URL` | JDBC URL do PostgreSQL |
| `DB_USERNAME` | Usuário do banco |
| `DB_PASSWORD` | Senha do banco |

Propriedades JPA relevantes:

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

> **Nota:** o arquivo `application.properties` contém entradas legadas para H2 no início do arquivo, mas a configuração efetiva usa PostgreSQL via variáveis de ambiente.

## Como executar

### Pré-requisitos

- JDK 21
- Maven (ou use o wrapper `./mvnw`)
- PostgreSQL acessível com as variáveis de ambiente configuradas

### Desenvolvimento local

```bash
# Linux / macOS
export DATABASE_URL=jdbc:postgresql://localhost:5432/znsk
export DB_USERNAME=postgres
export DB_PASSWORD=senha

./mvnw spring-boot:run
```

```powershell
# Windows (PowerShell)
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/znsk"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "senha"

.\mvnw.cmd spring-boot:run
```

A aplicação sobe na porta padrão **8080** do Spring Boot.

### Build

```bash
./mvnw clean package
java -jar target/znsk-shortener-url-0.0.1-SNAPSHOT.jar
```

## Docker

```bash
docker build -t znsk-shortener-url .
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host:5432/znsk \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=senha \
  znsk-shortener-url
```

O `Dockerfile` usa `eclipse-temurin:21-jdk-alpine`, compila com `./mvnw clean package -DskipTests` e executa o JAR gerado.

## Estrutura do projeto

```
znsk-shortener-url/
├── Dockerfile
├── pom.xml
├── mvnw / mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/example/znsk_shortener_url/
    │   │   ├── ZnskShortenerUrlApplication.java   # entry point + @EnableScheduling
    │   │   ├── controller/
    │   │   │   └── UrlController.java             # endpoints REST
    │   │   ├── service/
    │   │   │   └── UrlService.java                # lógica de negócio
    │   │   ├── repository/
    │   │   │   └── UrlMappingRepository.java      # Spring Data JPA
    │   │   ├── domain/url/
    │   │   │   └── UrlMapping.java                # entidade JPA
    │   │   └── dtos/
    │   │       └── RequestDto.java                # DTO de entrada
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/.../ZnskShortenerUrlApplicationTests.java
```

## Testes

```bash
./mvnw test
```

Atualmente há um teste de contexto Spring Boot (`contextLoads`) que verifica se a aplicação inicializa corretamente. Não há testes de integração dos endpoints ou da lógica de expiração.

## Exemplos de uso

### Encurtar URL

```bash
curl -X POST https://znsk.uk/shorten \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://github.com/exemplo", "expires": "30d"}'
```

Resposta:

```
https://znsk.uk/xY9kLm
```

### Acessar link encurtado

```bash
curl -I https://znsk.uk/xY9kLm
```

Resposta esperada para link válido:

```
HTTP/1.1 302 Found
Location: https://github.com/exemplo
```
