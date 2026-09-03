package com.phoenix.business.web;
import jakarta.servlet.http.HttpServletRequest;import org.springframework.dao.DataIntegrityViolationException;import org.springframework.http.*;import org.springframework.security.access.AccessDeniedException;import org.springframework.web.bind.MethodArgumentNotValidException;import org.springframework.web.bind.annotation.*;import org.springframework.web.server.ResponseStatusException;import java.time.Instant;import java.util.*;
@RestControllerAdvice public class ApiExceptionHandler {
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException x,HttpServletRequest r){Map<String,String> f=new LinkedHashMap<>();x.getBindingResult().getFieldErrors().forEach(e->f.putIfAbsent(e.getField(),e.getDefaultMessage()));return error(422,"Validation failed",r,f);}
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException x,HttpServletRequest r){return error(x.getStatusCode().value(),x.getReason()==null?"Request rejected":x.getReason(),r,null);}
 @ExceptionHandler(AccessDeniedException.class) ResponseEntity<?> forbidden(AccessDeniedException x,HttpServletRequest r){return error(403,"Access denied",r,null);}
 @ExceptionHandler({IllegalArgumentException.class,DataIntegrityViolationException.class}) ResponseEntity<?> bad(Exception x,HttpServletRequest r){return error(422,x instanceof IllegalArgumentException?x.getMessage():"The value conflicts with existing data",r,null);}
 @ExceptionHandler(NoSuchElementException.class) ResponseEntity<?> missing(Exception x,HttpServletRequest r){return error(404,"Resource not found",r,null);}
 @ExceptionHandler(Exception.class) ResponseEntity<?> unknown(Exception x,HttpServletRequest r){return error(500,"An unexpected error occurred",r,null);}
 private ResponseEntity<?> error(int status,String message,HttpServletRequest r,Object fields){Map<String,Object>b=new LinkedHashMap<>();b.put("timestamp",Instant.now());b.put("status",status);b.put("message",message);b.put("path",r.getRequestURI());if(fields!=null)b.put("fieldErrors",fields);return ResponseEntity.status(status).body(b);}
}
