# 🧠 AI Interview Agent

An AI-powered interview practice backend built with Spring Boot.  
It helps candidates prepare for real-world interviews by:

- Conducting mock interviews for different roles (SDE, Sales, Retail, etc.)
- Asking intelligent follow-up questions like a human interviewer
- Providing structured feedback on each answer (communication, technical depth, clarity, etc.)
- Supporting both **voice** and **chat**–based interaction (depending on the frontend / client)

---

## ✨ Features

- **Role-based mock interviews**
  - Start an interview for a specific role (e.g., *“Java Backend Developer”*, *“Sales Executive”*).
  - Question set dynamically generated based on role & experience level.

- **Conversational flow**
  - Asks next question automatically.
  - Can ask follow-up questions if the previous answer was incomplete or unclear.
  - Maintains interview context per session.

- **AI-driven feedback**
  - Analyses each answer on:
    - Communication & structure
    - Technical correctness
    - Depth of knowledge
    - Confidence & clarity
  - Shares improvement tips at the end of the interview.

- **Session management**
  - Create / resume / end interview sessions.
  - Stores current question index and conversation history in memory (or DB if extended).

- **LLM integration ready**
  - Backend designed so you can plug in any LLM provider (OpenAI, etc.).
  - Single service layer to generate:
    - Questions
    - Follow-ups
    - Feedback summaries

---

## 🏗️ Tech Stack

- **Backend Framework:** Spring Boot (Java 17)
- **Build Tool:** Maven
- **Architecture:** RESTful API, layered (Controller → Service → Model)
- **Others (typical for this project):**
  - Spring Web
  - Lombok (optional, for boilerplate reduction)
  - Jackson (JSON serialization)

> ℹ️ If you’ve added OpenAI or any other LLM SDK to `pom.xml`, this README already fits that use case (just adjust the config section below).

---

## 📂 Project Structure

Typical Maven + Spring Boot structure used by this project:

```text
AiInterviewAgent/
│
├── pom.xml
├── README.md
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com/example/interview/
│   │   │       ├── AiInterviewAgentApplication.java
│   │   │       │
│   │   │       ├── controller/
│   │   │       │   ├── AuthController.java
│   │   │       │   └── InterviewController.java
│   │   │       │
│   │   │       ├── service/
│   │   │       │   ├── InterviewService.java
│   │   │       │   └── UserService.java
│   │   │       │
│   │   │       ├── llm/
│   │   │       │   ├── LlmClient.java
│   │   │       │   └── dto/ChatMessage.java
│   │   │       │
│   │   │       ├── model/
│   │   │       │   ├── User.java
│   │   │       │   ├── UserRole.java
│   │   │       │   ├── InterviewConfig.java
│   │   │       │   ├── InterviewSession.java
│   │   │       │   ├── Answer.java
│   │   │       │   └── InterviewFeedback.java
│   │   │       │
│   │   │       ├── repository/
│   │   │       │   └── UserRepository.java
│   │   │       │
│   │   │       └── security/
│   │   │           ├── CustomUserDetailsService.java
│   │   │           ├── JwtAuthenticationFilter.java
│   │   │           └── SecurityConfig.java
│   │   │
│   │   └── resources
│   │       ├── application.properties
│   │       └── static/index.html
│   │
└── target/
```
## 🚀 Getting Started
1. Prerequisites

Java 17+

Maven 3.8+

A modern IDE (IntelliJ IDEA, VS Code, Eclipse, etc.)

2. Clone the repository
```
git clone https://github.com/MrRajKumar07/Ai_InterviewAgent.git
cd Ai_InterviewAgent

```
## ⚙️ Setup & Configuration

### 1️⃣ Database Setup (MariaDB)

Create database:
```
CREATE DATABASE interview_db;
```

Update your application.properties:
```
spring.datasource.url=jdbc:mariadb://localhost/interview_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### 2️⃣ LLM (Gemini) Configuration

Add these to application.properties:
```
llm.provider=GEMINI
llm.model=gemini-2.0-flash
llm.api-key=YOUR_GEMINI_API_KEY
llm.base-url=https://generativelanguage.googleapis.com/v1beta/openai/chat/completions
```

### ⚠️ 403 Forbidden Fix

- Enable "Gemini API" in Google AI Studio
- Ensure API key is valid
- Ensure billing is on
- Ensure model name is correct

4. Run the application

Using Maven wrapper:
```
./mvnw spring-boot:run      # Linux / macOS
mvnw.cmd spring-boot:run    # Windows
```

Or package and run:
```
./mvnw clean package
java -jar target/AiInterviewAgent-0.0.1-SNAPSHOT.jar

```
The application will typically start on:
```
http://localhost:3035
```

(Check your application.properties if you changed the port.)

## 🔥 API Endpoints

### 🔐 Authentication

POST /api/auth/signup
```
{ "fullName": "Raj Kumar", "email": "raj@gmail.com", "password": "123" }
```

POST /api/auth/login
```
{ "email": "raj@gmail.com", "password": "123" }
```

Returns:

- JWT Token
- User details

🎤 Interview API
POST /api/interview/start
```
Body:

{
  "role": "Java Backend Developer",
  "experience": "Fresher",
  "interviewType": "Technical"
}
```

Returns:

- sessionId
- first AI question

POST /api/interview/answer/{sessionId}
```
{ "answer": "Your response here..." }
```
POST /api/interview/finish/{sessionId}
Returns:

- Summary
- Scores
- Strengths
- Areas to improve
- Improved sample answers
  
## 🧱 Architecture & Design

### 🔹 Layered Architecture

This project follows a clean, modular **Layered Architecture** to ensure scalability, clarity, and easy maintenance.

#### **1. Controller Layer**
- Exposes REST APIs.
- Handles incoming HTTP requests.
- Performs request validation and maps DTOs.
- Delegates business logic to the Service layer.

#### **2. Service Layer**
- Contains the core business logic of the interview system.
- Manages the entire interview flow:
  - Starting a session
  - Generating questions
  - Receiving and evaluating answers
  - Providing instant and final feedback
- Interacts with the LLM Client layer for AI-driven tasks.

#### **3. LLM Client Layer**
- Encapsulates all AI provider interactions (OpenAI, etc.).
- Sends prompts to LLM models and returns generated responses.
- Makes the system provider-agnostic—easy to switch between models without affecting controllers/services.

---

### 🔹 Session-Centric Design

Each interview operates as an independent session.  
A session stores:

- 🎯 **Role** (e.g., Java Developer, Sales Executive)
- 🧩 **Experience Level** (Fresher, Mid, Senior)
- ❓ **List of questions asked so far**
- 🗣️ **User answers**
- 📌 **Current question index / interview progress**

This modular design allows:

- In-memory session storage for fast prototyping
- Easy future extension to persistence layers like:
  - MySQL / PostgreSQL
  - MongoDB
  - Redis (for fast session tracking)
  - Cloud DBs (AWS DynamoDB, Firestore)

---

### 🔹 Extensible LLM Integration

All AI-related logic is abstracted behind service interfaces, such as:

- **`LLMClient`**
- **`QuestionGeneratorService`**
- **`FeedbackService`**

Benefits:

- Swap AI providers without touching controller/service code.
- Add new LLMs (OpenAI, Groq, Gemini, Claude) easily.
- Centralized place to modify:
  - prompt templates  
  - models  
  - temperature & tuning parameters  

This ensures the entire system stays modular, clean, and future-proof.

---

### ⚙️ Why This Architecture?

- ✔️ Easy to maintain  
- ✔️ Highly scalable  
- ✔️ Clear separation of concerns  
- ✔️ Plug-and-play AI model support  
- ✔️ Ideal for large interview workflows and session-heavy operations  

### ✅ Running Tests

If you have tests configured, run:
```
./mvnw test

```
Add unit tests for:

- InterviewService – flow logic
- FeedbackService – scoring & feedback generation
- Any LLM adapter (mocking external calls)

### 🛣️ Roadmap / Possible Improvements

- Persist interview sessions in a database
- Add authentication & user profiles
- Export interview reports as PDF
- Add more role-specific question templates
- Integrate real-time voice (WebRTC / WebSockets + STT/TTS pipeline)

### 📄 License

This project currently has no explicit license.
If you want others to use it, consider adding a license (MIT, Apache-2.0, etc.) in a LICENSE file.

### 👤 Author

Raj Kumar
B.Tech (Software Engineering), Jain University, Bengaluru

If someone is reviewing this repository (e.g., for a hiring challenge), they can:

- Clone the project
- Configure the LLM API key
- Run the Spring Boot application
- Connect any frontend or API client (Postman) to test the interview flow
