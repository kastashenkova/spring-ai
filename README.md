# Semantic Search on Spring AI

An information retrieval fullstack web application for semantic search across FB2 documents using vector embeddings and Spring AI.

When a document is uploaded, its text is split into chunks via `TokenTextSplitter` and stored in a vector database as embeddings. On search, the query is also converted to an embedding and compared against stored vectors using cosine similarity — returning the most semantically relevant fragments, not just keyword matches.

**Backend:**
- Java 23, Spring Boot 3.4.2
- Spring AI milestone-release M5: embeddings, vector store, token splitter, Hierarchical Navigable Small World (HNSW), approximate nearest neighbor (ANN) and cosine distance algorithms for searching
- PostgreSQL, pgvector (relational and vector storage)
- Spring Data JPA, Swagger, OpenAPI

**Frontend:**
- HTML, CSS, JS
- communicates with backend via REST API

**API endpoints**

| Method | Endpoint | Summary |
|---|---|---|
| `GET` | `/docs` | Get all docs |
| `GET` | `/docs/{id}` | Search doc by its id |
| `POST` | `/docs` | Upload fb2 document |
| `DELETE` | `/docs/{id}` | Delete doc by its id |
| `POST` | `/docs/search` | Semantic search |


Backend available at `http://localhost:8088/api/`

JSON specification available at `http://localhost:8088/api/v3/api-docs`

Swagger UI available at `http://localhost:8088/api/swagger-ui.html`
