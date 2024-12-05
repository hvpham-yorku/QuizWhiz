# AI API (APPLICATION PROGRAMMING INTERFACE)

Our web application allows users the ability to upload a PDF or Word document, to have the program automatically generate questions into the 
user's question bank. We've implemented Artificial Intelligence to automate the process of generating flashcard-style questions, by connecting 
to **OpenAI's** models using the SpringAI framework. In this document are specifications as to how the program is incorporating Artificial 
Intelligence.

## What is an AI API

The AI Application Programming Interface used in our design serves as a bridge between our programming
and the actual AI. Using a unique API key used to connect to the API, our program can send data such as prompts to the AI in order to receive responses. 

## SpringAI

SpringAI serves as a helper library to help connect our spring boot application to OpenAI's services. As opposed to implementing complex programming,
the library provides the tools to simplify the process of sending prompts and receiving responses from the AI. This way the programmer only needs
to worry about telling the AI what it needs, as the library's built-in methods handle HTTP requests and response processing behind the scenes.

## How SpringAI Communicates

  - The **chatClient** class which offers methods to send prompts and receive responses, uses an HTTP client to send a request to OpenAI

  - The prompt is then structured in the format required by the AI;

      - The AI is provided its purpose, being prompted the following:

        **"You are an assistant for generating quiz questions",** so that the AI model
         knows exactly how to interpret the data from the imported document
  
      - It is then given a very structured prompt to generate flashcard questions in exactly the way our program requires, which is to provide a question,
      and an answer that tests a statement from the document:

        **"Based on the following notes, generate 4 flashcard-style questions. Each question should have a concise question and a precise answer. 
         Use the format:\n Question: [Your question text]\nAnswer: [Your answer text]\n\nNotes:\n%s"**

  - Both of these messages are combined into a single prompt, converted to JSON, then sent to the AI

  - The library then parses the AI's JSON response to map it into objects that the question class can handle

## The Benefits

During our implementation, we faced significant issues in connecting directly to the OpenAI model. Our program would fail to 
make successful HTTP requests and found consistent issues with connecting to the AI. However, with the SpringAI library, we were able to simplify
the process of 

  - Setting up the endpoint URL
  - Building data into JSON and back into class objects
  - Sending the HTTP request
  - Parsing the response

and only have to focus on generating the prompt

## The Logic Flow of Our Note Upload Feature

  1). A user uploads a document, either a PDF or docx, of a certain size (As our current implementation doesn't handle scenarios where the 
  file is too large)

  2). Text from the document is extracted (Using the Apache PDFBox Library for PDFs and Apache POI library for word documents)

  3). The extracted text is added to the prompt 

  4). The chatClient.call() method sends the prompt to the AI model

  5). OpenAI responds

  6). The response text is split using \n, and the question and answer are extracted by looking for the lines starting with "Question"
  and "Answer"

  7). The question & answer is then instantiated as a Question object, which is then stored in the database













  
