# Mind Map Generator

A Spring Boot application that generates interactive mind maps from text or documents using OpenAI's API.

## Features

- Generate mind maps from text input
- Upload and process various file types (TXT, PDF, DOCX, images)
- Interactive mind map visualization with D3.js
- Zoom, pan, and download mind maps as SVG
- Multiple AI model support (GPT-3.5, GPT-4)
- Responsive web interface

**PLEASE NOTE THAT THE JAVA VERSION IS STILL IN EARLY DEVELOPMENT PERIOD, AND THE OPENAI API MIGHT NOT WORK DUE TO FIREWALL POLICIES?**

## Technology Stack

- **Backend**: Java 21, Spring Boot 3
- **Frontend**: HTML5, CSS3, JavaScript, D3.js
- **API Integration**: OpenAI GPT API
- **Build Tool**: Maven
- **Caching**: EhCache

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven
- OpenAI API Key

### Installation

1. Clone the repository

2. Create a `.env` file in the root directory

3. Edit the `.env` file with your OpenAI API key:

```
OPENAI_API_KEY=your_openai_api_key_here
```

4. Build the project:

```bash
mvn clean package
```

5. Run the application:

```bash
java -jar target/mindmap-generator-1.0.0.jar
```

6. Visit `http://localhost:8080` in your web browser to use the application.

**We are working on a domain currently**

## Usage

### Text Input

1. Navigate to the "Text Input" tab
2. Enter or paste your text content
3. Select the AI model to use for analysis
4. Click "Generate Mind Map"

### File Upload

1. Navigate to the "File Upload" tab
2. Drag and drop your file or click to select a file
3. Click "Upload & Generate"

### Mind Map Interaction

- Use the mouse wheel to zoom in and out
- Click and drag to pan the mind map
- Hover over nodes for tooltips with more information
- Use the control buttons to:
  - Zoom in
  - Zoom out
  - Reset view
  - Download as SVG image

## API Endpoints

- `GET /api/mindmap/health` - Health check endpoint
- `POST /api/mindmap/generate` - Generate mind map from text
- `POST /api/mindmap/upload` - Upload and process a file

## Configuration

The application can be configured through the `application.properties` file. Key configurations include:

- `openai.api.key` - Your OpenAI API key
- `openai.model` - Default model to use
- `openai.api.timeout` - API timeout in seconds
- `spring.servlet.multipart.max-file-size` - Maximum file upload size
- `server.port` - Server port

## Caching

The application uses EhCache for caching mind maps to improve performance and reduce API calls. Cache configuration can be modified in the `ehcache.xml` file.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- [OpenAI](https://openai.com/) for their powerful (but sometimes have connectivity problems) API
- [D3.js](https://d3js.org/) for visualization
- [Spring Boot](https://spring.io/projects/spring-boot) for the web framework
