# C4 Model Documentation - TC Agencies Authentication Service

## Overview

This document describes the architecture of the TC Agencies Authentication Service using the C4 Model approach. The system serves as an authentication gateway and integration service for travel agencies, connecting multiple systems in the travel and tourism domain.

---

## Level 1: System Context Diagram

### System Purpose

The **TC Agencies Authentication Service** acts as a central authentication gateway that enables seamless integration between travel agency management systems and booking platforms.

### External Systems & Users

#### **Users**

- **Travel Agents**: End users who access the system through travel agency portals
- **System Administrators**: Manage system configuration and monitoring

#### **External Systems**

- **Cockpit System**: Travel agency management system that issues JWT tokens and stores agency data
- **TravelCompositor**: Booking and reservation management system
- **Microsite Portals**: Various travel agency websites (cockpit, suntrips, flugbuchung, etc.)
- **MySQL Database**: Stores microsite mapping configurations

### System Interactions

```
[Travel Agent] --uses--> [TC Agencies Auth Service] --authenticates with--> [Cockpit System]
[TC Agencies Auth Service] --synchronizes data--> [TravelCompositor]
[TC Agencies Auth Service] --redirects to--> [Microsite Portals]
[TC Agencies Auth Service] --reads config from--> [MySQL Database]
```

---

## Level 2: Container Diagram

### Containers Overview

#### **Web Application Container**

- **Technology**: Spring Boot 3.5.4, Java 21
- **Port**: 8080
- **Responsibilities**:
  - Handle HTTP requests
  - JWT token validation
  - Authentication orchestration
  - API integration management

#### **Database Container**

- **Technology**: MySQL 8.0
- **Host**: chsprod.vna.de:3306
- **Database**: lmxdb
- **Responsibilities**:
  - Store microsite mapping configurations
  - Company code to microsite URL mappings

### Container Interactions

```
[Web Browser] --HTTPS--> [Spring Boot App:8080]
[Spring Boot App] --JDBC--> [MySQL Database]
[Spring Boot App] --HTTPS--> [Cockpit API]
[Spring Boot App] --HTTPS--> [TravelCompositor API]
[Spring Boot App] --HTTP Redirect--> [Microsite Portals]
```

---

## Level 3: Component Diagram

### Core Components

#### **Presentation Layer**

- **UserController**
  - Endpoints: `/aerwebservice/user/authenticate`, `/aerwebservice/user/authenticatetest`
  - Handles authentication requests
  - Returns HTML redirects or JSON responses

#### **Service Layer**

##### **AuthenticationService** (Domain Service)

- **Responsibilities**:
  - Orchestrate authentication flow
  - Validate JWT tokens
  - Resolve microsite configurations
  - Ensure agency/user synchronization
  - Build authentication responses

##### **JwtService**

- **Responsibilities**:
  - Parse and validate ES256 JWT tokens
  - Fetch public keys from Cockpit
  - Token expiration and signature validation
  - Cache public keys for performance

##### **CockpitService**

- **Responsibilities**:
  - Fetch agency data from Cockpit API
  - Convert Cockpit data to TravelCompositor format
  - Handle Cockpit API authentication

##### **TravelCompositorService**

- **Responsibilities**:
  - Check agency status in TravelCompositor
  - Create/update agencies and users
  - Manage TravelCompositor API calls

##### **TravelcAuthManager**

- **Responsibilities**:
  - Manage authentication tokens for TravelCompositor
  - Cache tokens with expiration (30 minutes)
  - Handle multiple microsite credentials

##### **PasswordService**

- **Responsibilities**:
  - Generate encrypted passwords for users
  - Handle password encryption algorithms

##### **MicrositeMappingService**

- **Responsibilities**:
  - Resolve microsite configurations by company code
  - Database operations for microsite mappings

##### **HttpClientService**

- **Responsibilities**:
  - HTTP client operations
  - Public key fetching from Cockpit

#### **Data Layer**

- **MicrositeMappingRepository**: JPA repository for microsite configurations
- **MicrositeMapping Entity**: Database entity for microsite mappings

#### **Configuration Layer**

- **SecurityConfig**: Spring Security configuration
- **JwtConfig**: JWT validation configuration
- **MicrositeConfig**: Microsite fallback configuration
- **RestTemplateConfig**: HTTP client configuration

### Component Interactions Flow

```
1. [UserController] receives authentication request
2. [UserController] calls [AuthenticationService]
3. [AuthenticationService] validates JWT via [JwtService]
4. [JwtService] fetches public keys via [HttpClientService]
5. [AuthenticationService] resolves microsite via [MicrositeMappingService]
6. [AuthenticationService] checks agency status via [TravelCompositorService]
7. [TravelCompositorService] authenticates via [TravelcAuthManager]
8. [AuthenticationService] syncs data via [CockpitService] if needed
9. [AuthenticationService] generates password via [PasswordService]
10. [UserController] returns redirect response
```

---

## Level 4: Code Diagram

### Key Classes and Methods

#### **AuthenticationService**

```java
public class AuthenticationService {
    // Main authentication flow
    public AuthenticationResponse authenticateUser(String jwtToken, String language, String type)

    // Private helper methods
    private JwtClaims parseAndValidateJwtToken(String jwtToken)
    private MicrositeInfo resolveMicrositeInfo(String companyCode)
    private String generateUserPassword(JwtClaims claims)
    private AgencyStatus ensureAgencyActive(String agencyNumber, MicrositeInfo micrositeInfo)
    private boolean ensureUserExists(JwtClaims claims, String microsite)
    private boolean syncAgencyFromCockpit(String agencyNumber, String microsite, AgencyStatus currentStatus)
    private String buildReturnUrl(String baseMicrositeUrl, String language, String type, String user, String encryptedPassword, String agency)
}
```

#### **JwtService**

```java
public class JwtService {
    // JWT parsing and validation
    public JwtClaims parseJwtToken(String token)
    public boolean isTokenValid(JwtClaims claims)
    public boolean isTokenExpired(JwtClaims claims)

    // Private helper methods
    private boolean validateES256JWT(String jwtToken, String publicKeyPEM)
    private String determinePublicKeyUrl(String issuer, String kid)
    private String getPublicKey(String url)
}
```

#### **TravelCompositorService**

```java
public class TravelCompositorService {
    // Agency management
    public AgencyStatus checkAgencyStatus(String microsite, String agencyNumber)
    public TCAgencydata getAgencyData(String microsite, String agencyNumber)
    public Boolean createAgency(TravelcAgencyRequest agency, String microsite)
    public Boolean updateAgency(TravelcAgencyRequest agency, String microsite)

    // User management
    public Boolean getUser(String microsite, String agencyNumber, String userId)
    public Boolean createUser(TravelcUserRequest user, String microsite)
    public Boolean updateUser(TravelcUserRequest user, String microsite)
}
```

#### **TravelcAuthManager**

```java
public class TravelcAuthManager {
    // Token management
    public static String getToken(String microsite)
    public String refreshToken(String microsite)
    public boolean isTokenValid(String token)

    // Private helper methods
    private static String fetchTokenFromApi(String microsite, String username, String password)

    // Inner classes
    private static class TokenInfo { /* Token with expiration */ }
    private static class Credentials { /* Username/password pair */ }
}
```

---

## Data Flow Architecture

### Authentication Flow Sequence

1. **Request Reception**: UserController receives authentication request with JWT token
2. **JWT Validation**: JwtService validates token signature using public keys from Cockpit
3. **Claims Extraction**: Extract user and agency information from JWT claims
4. **Microsite Resolution**: Determine target microsite based on company code
5. **Agency Verification**: Check if agency exists and is active in TravelCompositor
6. **Data Synchronization**: Sync agency data from Cockpit if needed
7. **User Verification**: Check if user exists in TravelCompositor
8. **User Creation**: Create user in TravelCompositor if needed
9. **Password Generation**: Generate encrypted password for user
10. **URL Building**: Construct redirect URL with authentication parameters
11. **Response**: Return redirect response to microsite

### Error Handling Flow

- JWT validation failures return authentication errors
- Missing agencies trigger Cockpit synchronization
- Missing users trigger user creation from Cockpit data
- API failures are logged and handled gracefully
- Fallback microsite configuration used when mapping not found

---

## Security Architecture

### Authentication Mechanisms

- **JWT Token Validation**: ES256 algorithm with public key verification
- **Token Expiration**: Configurable token expiration checking (currently disabled)
- **Public Key Caching**: In-memory cache for public keys to improve performance
- **API Authentication**: Bearer token authentication for TravelCompositor API

### Security Configurations

- **CSRF Protection**: Disabled for API endpoints
- **CORS**: Configured for cross-origin requests
- **Endpoint Security**: `/aerwebservice/user/**` endpoints are publicly accessible
- **Database Security**: Encrypted connection to MySQL database

---

## Integration Points

### External API Integrations

#### **Cockpit API**

- **Base URL**: `https://cockpit.aerticket.de/api/chs/agencies/`
- **Authentication**: Bearer token
- **Purpose**: Fetch agency data for synchronization
- **Data Format**: JSON

#### **TravelCompositor API**

- **Base URL**: `https://kombireisen.suntrips.de/resources`
- **Authentication**: Custom auth-token header
- **Purpose**: Manage agencies and users
- **Operations**: GET, POST, PUT for agencies and users

#### **Public Key URLs**

- **QA**: `https://qa-cockpit-aer-de.aerticket.org/common/keys/{kid}.pub`
- **Staging**: `https://stg-cockpit-aer-de.aerticket.org/common/keys/{kid}.pub`
- **Production**: `https://cockpit.aerticket.de/common/keys/{kid}.pub`

### Database Schema

```sql
-- Microsite mapping table
CREATE TABLE aer_cockpit_mapping_microsite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_code VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    microsite VARCHAR(255),
    microsite_api VARCHAR(255),
    microsite_url VARCHAR(255)
);
```

---

## Deployment Architecture

### Environment Configuration

- **Development**: Local development with embedded configurations
- **Staging**: Staging environment with staging Cockpit integration
- **Production**: Production environment with production Cockpit integration

### Configuration Management

- **Application Properties**: Environment-specific configurations
- **JWT Configuration**: Public key URLs and timeouts
- **Database Configuration**: Connection strings and credentials
- **Microsite Configuration**: Fallback URLs and default settings

### Monitoring and Logging

- **Logging Framework**: SLF4J with Logback
- **Log Levels**: DEBUG level for detailed authentication flow
- **Error Tracking**: Comprehensive error logging for troubleshooting
- **Performance Monitoring**: Token caching and API response times

---

## Technology Stack Summary

### Core Technologies

- **Java 21**: Programming language
- **Spring Boot 3.5.4**: Application framework
- **Spring Security**: Security framework
- **Spring Data JPA**: Data access layer
- **MySQL 8.0**: Database
- **Maven**: Build tool

### Libraries and Dependencies

- **Lombok**: Code generation
- **JJWT**: JWT processing
- **Nimbus JOSE JWT**: JWT validation
- **Jackson**: JSON processing
- **Apache Commons Codec**: Encoding utilities

### External Integrations

- **REST APIs**: HTTP client integrations
- **JWT Tokens**: ES256 signature validation
- **Database**: JPA/Hibernate ORM
- **Caching**: In-memory token and key caching

---

## Quality Attributes

### Performance

- **Token Caching**: 30-minute cache for TravelCompositor tokens
- **Public Key Caching**: In-memory cache for JWT public keys
- **Connection Pooling**: Database connection pooling
- **Async Processing**: Non-blocking API calls where possible

### Reliability

- **Error Handling**: Comprehensive exception handling
- **Fallback Mechanisms**: Default microsite configuration
- **Retry Logic**: Implicit retry through token refresh
- **Data Consistency**: Synchronization between systems

### Security

- **Token Validation**: Strong JWT signature verification
- **Encrypted Passwords**: Secure password generation
- **API Authentication**: Secure API communications
- **Input Validation**: Parameter sanitization and validation

### Maintainability

- **Clean Architecture**: Separation of concerns
- **Configuration Management**: Externalized configuration
- **Logging**: Comprehensive logging for debugging
- **Documentation**: Inline code documentation

---

This C4 Model documentation provides a comprehensive view of the TC Agencies Authentication Service architecture, from high-level system context down to detailed code structure, enabling better understanding and maintenance of the system.
