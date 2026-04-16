# SpringRag

A Spring Boot application implementing a Retrieval-Augmented Generation (RAG) system using Mistral AI for embeddings and chat completion.

## Overview

SpringRag is a lightweight RAG implementation that allows you to:
- Ingest text documents and store them as vector embeddings
- Ask questions and get answers based on the ingested content
- Use cosine similarity for semantic search
- Leverage Mistral AI for both embeddings and chat completion

## Tech Stack

- **Java 17**
- **Spring Boot 3.5.5**
- **Mistral AI API** (embeddings and chat)
- **In-memory vector store** with cosine similarity
- **H2 Database** (for persistence)
- **Spring WebFlux** (for reactive programming)

## Features

- **Text Ingestion**: POST `/rag/ingest` - Add documents to the knowledge base
- **Question Answering**: POST `/rag/ask` - Ask questions based on ingested content
- **Vector Search**: Cosine similarity-based semantic search
- **Source Attribution**: Returns source references with answers
- **Error Handling**: Comprehensive error handling with French error messages
- **Retry Logic**: Automatic retries with exponential backoff for API failures

## Configuration

### Environment Variables

Set your Mistral API key:
```bash
export MISTRAL_KEY=your_mistral_api_key_here
```

Or configure in `application.properties`:
```properties
mistral.api.key=your_mistral_api_key_here
```

### Application Properties

The application uses these default models (configurable):
- Chat model: `mistral-small-latest`
- Embedding model: `mistral-embed`

## API Endpoints

### Ingest Text
```http
POST /rag/ingest
Content-Type: application/json

{
  "text": "Your document text here",
  "source": "document_name_or_url"
}
```

### Ask Question
```http
POST /rag/ask
Content-Type: application/json

{
  "question": "Your question here",
  "topK": 4
}
```

Response:
```json
{
  "answer": "The answer based on ingested content",
  "sources": ["document_name_or_url"]
}
```

## Architecture

### Components

1. **RagController**: REST API endpoints for ingestion and querying
2. **RagService**: Business logic for RAG operations
3. **MistralClient**: HTTP client for Mistral AI API with retry logic
4. **InMemoryVectorStore**: Vector storage with cosine similarity search

### Flow

1. **Ingestion**: Text is embedded using Mistral's embedding API and stored with metadata
2. **Querying**: Question is embedded, similar documents are retrieved, and context is passed to Mistral's chat API
3. **Response**: Answer is returned with source attribution

## Running the Application

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Mistral AI API key

### Build and Run

```bash
# Build the application
mvn clean package

# Run the application
java -jar target/SpringRag-0.0.1-SNAPSHOT.jar
```

Or using Maven:
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Usage Example

1. **Ingest some text:**
```bash
curl -X POST http://localhost:8080/rag/ingest \
  -H "Content-Type: application/json" \
  -d '{"text": "Spring Boot is a framework for building production-ready applications with minimal configuration.", "source": "spring-docs"}'
```

2. **Ask a question:**
```bash
curl -X POST http://localhost:8080/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is Spring Boot?", "topK": 2}'
```

## Error Handling

The application provides comprehensive error handling:
- **401 errors**: Missing or invalid Mistral API key
- **429 errors**: Rate limiting with retry logic
- **Validation errors**: Missing required fields
- **General errors**: Detailed error messages with context

## Development

### Project Structure

```
src/main/java/com/example/springrag/
|-- Rag/
|   |-- RagController.java      # REST endpoints
|   |-- RagService.java         # Business logic
|   |-- MistralClient.java      # Mistral AI integration
|   |-- InMemoryVectorStore.java # Vector storage
```

### Dependencies

Key dependencies include:
- Spring Boot Starter Web/WebFlux
- Spring Boot Starter Data JPA
- H2 Database
- Jackson (JSON processing)

## Limitations

- **In-memory storage**: Vector embeddings are stored in memory only
- **No persistence**: Data is lost on application restart
- **Limited scalability**: Suitable for small to medium datasets

## Future Enhancements

- Persistent vector database (Pinecone, Weaviate, etc.)
- Multi-language support
- Document chunking for large texts
- Web interface for easier interaction
- Batch ingestion capabilities
- Vector indexing optimizations
