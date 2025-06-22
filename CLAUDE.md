# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Maven Commands
- **Build**: `mvn clean package -DskipTests` (fast build, used by Docker scripts)
- **Test**: `mvn test` (run all tests)
- **Clean build**: `mvn clean package` (full build with tests)
- **Local run**: `mvn spring-boot:run` (development mode)

### Docker Commands (Recommended)
- **Start services**: `./bin/start.sh` (Mac/Linux) or `bin\start.bat` (Windows)
- **Start with database reset**: `./bin/start.sh --reset-db`
- **Force rebuild**: `./bin/start.sh --clean-build`
- **Stop services**: `./bin/stop.sh` or `bin\stop.bat`
- **View logs**: `./bin/logs.sh -f` or `bin\logs.bat -f`

### Database Management
- **Start database only**: `docker-compose up -d postgres`
- **Database connection**: `localhost:5433`, database: `api_gateway`, user: `gateway_user`, password: `gateway_pass`
- **Reset database**: Use `--reset-db` flag with start scripts

## Architecture Overview

This is a **Domain-Driven Design (DDD)** Spring Boot application for API gateway management with intelligent load balancing and high availability.

### DDD Layer Structure
```
src/main/java/org/xhy/gateway/
├── interfaces/          # Controllers and REST endpoints
├── application/         # Application services (orchestration, transactions)
│   ├── service/        # App services (XxxAppService)
│   ├── dto/            # Data transfer objects  
│   └── assembler/      # Entity ↔ DTO conversion
├── domain/             # Core business logic (3 bounded contexts)
│   ├── apiinstance/    # API instance selection and management
│   ├── apikey/         # API key management
│   └── metrics/        # Performance metrics collection
└── infrastructure/     # Technical concerns (config, persistence)
```

### Key Domain Concepts
- **Project**: Multi-tenant organization unit for API resources
- **API Instance**: Registered backend API endpoints with metadata (priority, cost, health)
- **API Selection**: Smart routing based on performance, affinity, and load balancing strategies
- **Metrics**: Real-time collection of API call results for decision making

### Bounded Contexts
1. **API Instance Domain**: Registration, selection algorithms, load balancing strategies
2. **Metrics Domain**: Performance tracking, health monitoring, success rate calculation  
3. **Project/API Key Domain**: Multi-tenant management and authentication

## DDD Development Rules

### Code Organization
- **No Lombok**: Use explicit getters/setters and constructors
- **MyBatis Plus**: Use repository interfaces extending `MyBatisPlusExtRepository`, no manual SQL
- **Constructor injection**: For dependency injection (no `@Autowired` on fields)
- **Transactions**: Only in application layer (`@Transactional` on app services for updates)

### Naming Conventions
- **Entities**: `XxxEntity` (e.g., `ProjectEntity`, `ApiInstanceEntity`)
- **Requests**: `XxxCreateRequest`, `XxxUpdateRequest` 
- **App Services**: `XxxAppService`
- **Domain Services**: `XxxDomainService`
- **Assemblers**: `XxxAssembler` (for DTO ↔ Entity conversion)

### Data Flow
```
HTTP Request → Controller → AppService → DomainService → Repository
                     ↓         ↓           ↓
               Request → Entity → Entity → Database
                     ↑         ↑
               DTO ← Assembler ← Entity
```

### Method Conventions
- **get()**: Must return value or throw exception (never null)
- **find()**: Can return null
- **No Optional**: Use null returns where appropriate

## Core Business Logic

### API Selection Algorithm
The gateway implements intelligent API instance selection through:
- **Load balancing strategies**: Round-robin, latency-first, success-rate-first, smart strategy
- **Affinity-aware routing**: Caches user/API bindings for consistency
- **Circuit breaker pattern**: Automatic fallback and degradation
- **Real-time metrics**: Success rates, latency, and health scoring

### Key Services
- `ApiInstanceSelectionDomainService`: Core selection algorithm with strategy pattern
- `AffinityAwareStrategyDecorator`: Adds user affinity to selection strategies  
- `MetricsCollectionDomainService`: Aggregates performance data for decision making

## Testing

### Test Structure
- **Base class**: `BaseIntegrationTest` for Spring Boot integration tests
- **Test data**: `TestDataGenerator` for creating test entities
- **Active profile**: `@ActiveProfiles("test")` 
- **Database**: Uses same PostgreSQL setup as main app

### Key Test Classes
- `ApiInstanceSelectionDomainServiceTest`: Tests core selection algorithms
- `MetricsCollectionDomainServiceTest`: Tests metrics aggregation
- Integration tests for application services

## Service Endpoints

### External API (Gateway Functions)
- `POST /api/selection/instance` - Select best API instance
- `POST /api/selection/report` - Report API call results
- `GET /api/project/{projectName}` - Get project info

### Admin API  
- `/api/admin/projects/**` - Project management
- `/api/admin/api-instances/**` - API instance management
- `/api/admin/monitoring/**` - Metrics and monitoring

## Development Notes

- **PostgreSQL**: Required for JSONB support (provider info storage)
- **Caffeine Cache**: Used for affinity bindings and metrics caching
- **Spring Boot 3.2**: Uses Java 17 features
- **Port 8081**: Default application port (8080 in development mode)
- **Health check**: `/api/health` endpoint for monitoring