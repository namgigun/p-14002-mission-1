package com.back.global.globalExceptionHandler

import com.back.global.exception.ServiceException
import com.back.global.rsData.RsData
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import lombok.RequiredArgsConstructor
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.stream.Collectors

@RestControllerAdvice
@RequiredArgsConstructor
class GlobalExceptionHandler {
    @ExceptionHandler(NoSuchElementException::class)
    fun handle(ex: NoSuchElementException?): ResponseEntity<RsData<Void>> {
        return ResponseEntity(
            RsData(
                "404-1",
                "해당 데이터가 존재하지 않습니다."
            ),
            HttpStatus.NOT_FOUND
        )
    }


    @ExceptionHandler(ConstraintViolationException::class)
    fun handle(ex: ConstraintViolationException): ResponseEntity<RsData<Void>> {
        val message = ex.constraintViolations
            .stream()
            .map { violation: ConstraintViolation<*> ->
                val field = violation.propertyPath.toString().split("\\.".toRegex(), limit = 2).toTypedArray()[1]
                val messageTemplateBits = violation.messageTemplate
                    .split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val code = messageTemplateBits[messageTemplateBits.size - 2]
                val _message = violation.message
                "%s-%s-%s".formatted(field, code, _message)
            }
            .sorted(Comparator.comparing { obj: String -> obj.toString() })
            .collect(Collectors.joining("\n"))

        return ResponseEntity(
            RsData(
                "400-1",
                message
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle(ex: MethodArgumentNotValidException): ResponseEntity<RsData<Void>> {
        val message = ex.bindingResult
            .allErrors
            .stream()
            .filter { error: ObjectError? -> error is FieldError }
            .map { error: ObjectError -> error as FieldError }
            .map { error: FieldError -> error.field + "-" + error.code + "-" + error.defaultMessage }
            .sorted(Comparator.comparing { obj: String -> obj.toString() })
            .collect(Collectors.joining("\n"))

        return ResponseEntity(
            RsData(
                "400-1",
                message
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handle(ex: HttpMessageNotReadableException?): ResponseEntity<RsData<Void>> {
        return ResponseEntity(
            RsData(
                "400-1",
                "요청 본문이 올바르지 않습니다."
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handle(ex: MissingRequestHeaderException): ResponseEntity<RsData<Void>> {
        return ResponseEntity(
            RsData(
                "400-1",
                "%s-%s-%s".formatted(
                    ex.headerName,
                    "NotBlank",
                    ex.localizedMessage
                )
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(ServiceException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handle(ex: ServiceException): ResponseEntity<RsData<Void?>> {
        val rsData: RsData<Void?> = ex.rsData

        return ResponseEntity(
            rsData,
            ResponseEntity
                .status(rsData.statusCode)
                .build<Any>()
                .statusCode
        )
    }
}