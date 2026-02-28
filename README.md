# Semantic Search on Spring AI

An information retrieval fullstack web application for semantic search across FB2 documents using vector embeddings and Spring AI.

When a document is uploaded, its text is split into chunks via `TokenTextSplitter` and stored in a vector database as embeddings. On search, the query is also converted to an embedding and compared against stored vectors using cosine similarity — returning the most semantically relevant fragments, not just keyword matches.

**Backend:**
- Java 23, Spring Boot 3
- Spring AI milestone-release M5: embeddings, vector store, token splitter, Hierarchical Navigable Small World (HNSW), approximate nearest neighbor (ANN) algorithm for searching
- PostgreSQL, pgvector (relational and vector storage)
- Spring Data JPA, Swagger, OpenAPI

**Frontend:**
- HTML, CSS, JS
- communicates with backend via REST API

**API endpoints**

| Method | Endpoint | Summary |
|---|---|---|
| `GET` | `/api/docs` | Get all docs |
| `GET` | `/api/docs/{id}` | Search doc by its id |
| `POST` | `/api/docs` | Upload fb2 document |
| `DELETE` | `/api/docs/{id}` | Delete doc by its id |
| `POST` | `/api/docs/search` | Semantic search |


Backend available at `http://localhost:8088/api/`

Swagger UI available at `http://localhost:8088/swagger-ui.html`
