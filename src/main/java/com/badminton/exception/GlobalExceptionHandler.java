package com.badminton.exception;

import com.badminton.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j; // ✅ THÊM
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j // ✅ THÊM
@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(
                        ResourceNotFoundException ex) {
                log.error("❌ Resource not found: {}", ex.getMessage()); // ✅ THÊM
                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(ex.getMessage()));
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ApiResponse<Object>> handleBadRequestException(
                        BadRequestException ex) {
                log.error("❌ Bad request: {}", ex.getMessage()); // ✅ THÊM
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(ex.getMessage()));
        }

        @ExceptionHandler(UnauthorizedException.class)
        public ResponseEntity<ApiResponse<Object>> handleUnauthorizedException(
                        UnauthorizedException ex) {
                log.error("❌ Unauthorized: {}", ex.getMessage()); // ✅ THÊM
                return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .body(ApiResponse.error(ex.getMessage()));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
                        AccessDeniedException ex) {
                log.error("❌ Access denied: {}", ex.getMessage()); // ✅ THÊM
                return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .body(ApiResponse.error("Bạn không có quyền truy cập tài nguyên này"));
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(
                        BadCredentialsException ex) {
                log.error("❌ Bad credentials"); // ✅ THÊM
                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.error("Email/Số điện thoại hoặc mật khẩu không chính xác"));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
                        MethodArgumentNotValidException ex) {
                Map<String, String> errors = new HashMap<>();
                ex.getBindingResult().getAllErrors().forEach((error) -> {
                        String fieldName = ((FieldError) error).getField();
                        String errorMessage = error.getDefaultMessage();
                        errors.put(fieldName, errorMessage);
                });

                log.error("❌ Validation error: {}", errors); // ✅ THÊM

                ApiResponse<Map<String, String>> response = new ApiResponse<>(
                                false,
                                "Dữ liệu không hợp lệ",
                                errors,
                                java.time.LocalDateTime.now());

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(response);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Object>> handleGlobalException(Exception ex) {
                log.error("❌ Unexpected error: ", ex); // ✅ SỬA (đã có printStackTrace, thay bằng log)
                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.error("Có lỗi xảy ra: " + ex.getMessage()));
        }
}
