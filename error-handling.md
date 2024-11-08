**Editor’s note: This article was updated on September 5, 2022, by our editorial team. It has been modified to include recent sources and to align with our current editorial standards.**

The ability to handle errors correctly in APIs while providing meaningful error messages is a desirable feature, as it can help the API client respond to issues. The default behavior returns stack traces that are hard to understand and ultimately useless for the API client. Partitioning the error information into fields enables the API client to parse it and provide better error messages to the user. In this article, we cover how to implement proper [Spring Boot](https://www.toptal.com/spring) exception handling when building a REST API .

![Person confused about a cryptic and long error message](https://assets.toptal.io/images?url=https%3A%2F%2Fbs-uploads.toptal.io%2Fblackfish-uploads%2Fpublic-files%2Fspring_boot_exception_handling-b90b778cb2d9fe923a6fd227795b668a.webp)

Building REST APIs with Spring became the standard approach for Java developers. Using Spring Boot helps substantially, as it removes a lot of boilerplate code and enables auto-configuration of various components. We assume that you’re familiar with the basics of API development with those technologies. If you are unsure about how to develop a basic REST API, you should start with this article about [Spring MVC](https://www.toptal.com/spring/beginners-guide-to-mvc-with-spring-framework) or this article about building a [Spring REST Service](https://spring.io/guides/gs/rest-service/).

## Making Error Responses Clearer

We’ll use the [source code hosted on GitHub](https://github.com/brunocleite/spring-boot-exception-handling) as an example application that implements a REST API for retrieving objects that represent birds. It has the features described in this article and a few more examples of error handling scenarios. Here’s a summary of endpoints implemented in that application:

<table><thead></thead><tbody><tr><td><code>GET /birds/{birdId}</code></td><td>Gets information about a bird and throws an exception if not found.</td></tr><tr><td><code>GET /birds/noexception/{birdId}</code></td><td>This call also gets information about a bird, except it doesn't throw an exception when a bird doesn't exist with that ID.</td></tr><tr><td><code>POST /birds</code></td><td>Creates a bird.</td></tr></tbody></table>

The Spring framework MVC module has excellent features for error handling. But it is left to the developer to use those features to treat the exceptions and return meaningful responses to the API client.

Let’s look at an example of the default Spring Boot answer when we issue an HTTP POST to the `/birds` endpoint with the following JSON object that has the string “aaa” on the field “mass,” which should be expecting an integer:

```json
{
 "scientificName": "Common blackbird",
 "specie": "Turdus merula",
 "mass": "aaa",
 "length": 4
}
```

The Spring Boot default answer, without proper error handling, looks like this:

```java
{
 "timestamp": 1658551020,
 "status": 400,
 "error": "Bad Request",
 "exception": "org.springframework.http.converter.HttpMessageNotReadableException",
 "message": "JSON parse error: Unrecognized token 'three': was expecting ('true', 'false' or 'null'); nested exception is com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'aaa': was expecting ('true', 'false' or 'null')\n at [Source: java.io.PushbackInputStream@cba7ebc; line: 4, column: 17]",
 "path": "/birds"
}
```

The Spring Boot `DefaultErrorAttributes`\-generated response has some good fields, but it is too focused on the exception. The `timestamp` field is an integer that doesn’t carry information about its measurement unit. The `exception` field is only valuable to Java developers, and the message leaves the API consumer lost in implementation details that are irrelevant to them. What if there were more details we could extract from the exception? Let’s learn how to handle exceptions in Spring Boot properly and wrap them into a better JSON representation to make life easier for our API clients.

As we’ll be using Java date and time classes, we first need to add a Maven dependency for the Jackson JSR310 converters. They convert Java date and time classes to JSON representation using the `@JsonFormat` annotation:

```xml
<dependency>
   <groupId>com.fasterxml.jackson.datatype</groupId>
   <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

Next, let’s define a class for representing API errors. We’ll create a class called `ApiError` with enough fields to hold relevant information about errors during REST calls:

```java
class ApiError {

   private HttpStatus status;
   @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
   private LocalDateTime timestamp;
   private String message;
   private String debugMessage;
   private List<ApiSubError> subErrors;

   private ApiError() {
       timestamp = LocalDateTime.now();
   }

   ApiError(HttpStatus status) {
       this();
       this.status = status;
   }

   ApiError(HttpStatus status, Throwable ex) {
       this();
       this.status = status;
       this.message = "Unexpected error";
       this.debugMessage = ex.getLocalizedMessage();
   }

   ApiError(HttpStatus status, String message, Throwable ex) {
       this();
       this.status = status;
       this.message = message;
       this.debugMessage = ex.getLocalizedMessage();
   }
}
```

- The `status` property holds the operation call status, which will be anything from 4xx to signal client errors or 5xx to signal server errors. A typical scenario is an HTTP code 400: BAD\_REQUEST when the client, for example, sends an improperly formatted field, like an invalid email address.

- The `timestamp` property holds the date-time instance when the error happened.

- The `message` property holds a user-friendly message about the error.

- The `debugMessage` property holds a system message describing the error in detail.

- The `subErrors` property holds an array of suberrors when there are multiple errors in a single call. An example would be numerous validation errors in which multiple fields have failed. The `ApiSubError` class encapsulates this information:

```java
abstract class ApiSubError {

}

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
class ApiValidationError extends ApiSubError {
   private String object;
   private String field;
   private Object rejectedValue;
   private String message;

   ApiValidationError(String object, String message) {
       this.object = object;
       this.message = message;
   }
}
```

The `ApiValidationError` is a class that extends `ApiSubError` and expresses validation problems encountered during the REST call.

Below, you’ll see examples of JSON responses generated after implementing these improvements.

Here is a JSON example returned for a missing entity while calling endpoint `GET /birds/2`:

```java
{
 "apierror": {
   "status": "NOT_FOUND",
   "timestamp": "22-07-2022 06:20:19",
   "message": "Bird was not found for parameters {id=2}"
 }
}
```

Here is another example of JSON returned when issuing a `POST /birds` call with an invalid value for the bird’s mass:

```java
{
 "apierror": {
   "status": "BAD_REQUEST",
   "timestamp": "22-07-2022 06:49:25",
   "message": "Validation errors",
   "subErrors": [
     {
       "object": "bird",
       "field": "mass",
       "rejectedValue": 999999,
       "message": "must be less or equal to 104000"
     }
   ]
 }
}
```

## Spring Boot Error Handler

Let’s explore some Spring annotations used to handle exceptions.

[`RestController`](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/bind/annotation/RestController.html) is the base annotation for classes that handle REST operations.

[`ExceptionHandler`](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/bind/annotation/ExceptionHandler.html) is a Spring annotation that provides a mechanism to treat exceptions thrown during execution of handlers (controller operations). This annotation, if used on methods of controller classes, will serve as the entry point for handling exceptions thrown within this controller only.

Altogether, the most common implementation is to use `@ExceptionHandler` on methods of `@ControllerAdvice` classes so that the Spring Boot exception handling will be applied globally or to a subset of controllers.

[`ControllerAdvice`](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/bind/annotation/ControllerAdvice.html) is an annotation in Spring and, as the name suggests, is “advice” for multiple controllers. It enables the application of a single `ExceptionHandler` to multiple controllers. With this annotation, we can define how to treat such an exception in a single place, and the system will call this handler for thrown exceptions on classes covered by this `ControllerAdvice`.

The subset of controllers affected can be defined by using the following selectors on `@ControllerAdvice`: `annotations()`, `basePackageClasses()`, and `basePackages()`. `ControllerAdvice` is applied globally to all controllers if no selectors are provided

By using `@ExceptionHandler` and `@ControllerAdvice`, we’ll be able to define a central point for treating exceptions and wrapping them in an `ApiError` object with better organization than is possible with the default Spring Boot error-handling mechanism.

## Handling Exceptions

![Representation of what happens with a successful and failed REST client call](https://assets.toptal.io/images?url=https%3A%2F%2Fbs-uploads.toptal.io%2Fblackfish-uploads%2Fpublic-files%2Fspring_boot_exception_handling-7be9e31168192062d06e975f837a2860.webp)

Next, we’ll create the class that will handle the exceptions. For simplicity, we call it `RestExceptionHandler`, which must extend from Spring Boot’s `ResponseEntityExceptionHandler`. We’ll be extending `ResponseEntityExceptionHandler`, as it already provides some basic handling of Spring MVC exceptions. We’ll add handlers for new exceptions while improving the existing ones.

## Overriding Exceptions Handled in ResponseEntityExceptionHandler

If you take a look at the source code of `ResponseEntityExceptionHandler`, you’ll see a lot of methods called `handle******()`, like `handleHttpMessageNotReadable()` or `handleHttpMessageNotWritable()`. Let’s see how can we extend `handleHttpMessageNotReadable()` to handle `HttpMessageNotReadableException` exceptions. We just have to override the method `handleHttpMessageNotReadable()` in our `RestExceptionHandler` class:

```java
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

   @Override
   protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
       String error = "Malformed JSON request";
       return buildResponseEntity(new ApiError(HttpStatus.BAD_REQUEST, error, ex));
   }

   private ResponseEntity<Object> buildResponseEntity(ApiError apiError) {
       return new ResponseEntity<>(apiError, apiError.getStatus());
   }

   //other exception handlers below

}
```

We have declared that in case of a thrown`HttpMessageNotReadableException`, the error message will be “Malformed JSON request” and the error will be encapsulated in the `ApiError` object. Below, we can see the answer of a REST call with this new method overridden:

```java
{
 "apierror": {
   "status": "BAD_REQUEST",
   "timestamp": "22-07-2022 03:53:39",
   "message": "Malformed JSON request",
   "debugMessage": "JSON parse error: Unrecognized token 'aaa': was expecting ('true', 'false' or 'null'); nested exception is com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'aaa': was expecting ('true', 'false' or 'null')\n at [Source: java.io.PushbackInputStream@7b5e8d8a; line: 4, column: 17]"
 }
}
```

### Implementing Custom Exceptions

Next, we’ll create a method that handles an exception not yet declared inside Spring Boot’s `ResponseEntityExceptionHandler`.

A common scenario for a Spring application that handles database calls is to provide a method that returns a record by its ID using a repository class. But if we look into the `CrudRepository.findOne()` method, we’ll see that it returns `null` for an unknown object. If our service calls this method and returns directly to the controller, we’ll get an HTTP code 200 (OK) even if the resource isn’t found. In fact, the proper approach is to return a HTTP code 404 (NOT FOUND) as specified in the [HTTP/1.1 spec](https://www.rfc-editor.org/rfc/rfc9110.html).

We’ll create a custom exception called `EntityNotFoundException` to handle this case. This one is different from `javax.persistence.EntityNotFoundException`, as it provides some constructors that ease the object creation, and one may choose to handle the `javax.persistence` exception differently.

![Example of a failed REST call](https://assets.toptal.io/images?url=https%3A%2F%2Fbs-uploads.toptal.io%2Fblackfish-uploads%2Fpublic-files%2Fspring_boot_exception_handling-341d794d4ba34029b76d47ca70424913.webp)

That said, let’s create an `ExceptionHandler` for this newly created `EntityNotFoundException` in our `RestExceptionHandler` class. Create a method called `handleEntityNotFound()` and annotate it with `@ExceptionHandler`, passing the class object `EntityNotFoundException.class` to it. This declaration signalizes Spring that every time `EntityNotFoundException` is thrown, Spring should call this method to handle it.

When annotating a method with `@ExceptionHandler`, a wide range of auto-injected parameters like `WebRequest`, `Locale`, and others may be specified as described [here](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/bind/annotation/ExceptionHandler.html). We’ll provide the exception `EntityNotFoundException` as a parameter for this `handleEntityNotFound` method:

```java
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
  
   //other exception handlers
  
   @ExceptionHandler(EntityNotFoundException.class)
   protected ResponseEntity<Object> handleEntityNotFound(
           EntityNotFoundException ex) {
       ApiError apiError = new ApiError(NOT_FOUND);
       apiError.setMessage(ex.getMessage());
       return buildResponseEntity(apiError);
   }
}
```

Great! In the `handleEntityNotFound()` method, we set the HTTP status code to `NOT_FOUND` and usethe new exception message. Here is what the response for the `GET /birds/2` endpoint looks like now:

```java
{
 "apierror": {
   "status": "NOT_FOUND",
   "timestamp": "22-07-2022 04:02:22",
   "message": "Bird was not found for parameters {id=2}"
 }
}
```

## The Importance of Spring Boot Exception Handling

It is important to control exception handling so we can properly map exceptions to the `ApiError` object and inform API clients appropriately. Additionally, we would need to create more handler methods (the ones with @ExceptionHandler) for thrown exceptions within the application code. The [GitHub code](https://github.com/brunocleite/spring-boot-exception-handling) provides more more examples for other common exceptions like `MethodArgumentTypeMismatchException`, `ConstraintViolationException`.

Here are some additional resources that helped in the composition of this article:

- [Error Handling for REST With Spring](https://www.baeldung.com/exception-handling-for-rest-with-spring)

- [Exception Handling in Spring MVC](https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc)

## Understanding the basics

- ### Why should the API have a uniform error format?

- ### How does Spring know which ExceptionHandler to use?

- ### What information is important to provide to API consumers?
