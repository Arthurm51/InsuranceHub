package br.com.insurehub.common;

import java.util.List;

/** Erro de validação detectado no serviço (campos condicionais), respondido como 400 VALIDATION_ERROR. */
public class ValidationException extends RuntimeException {

    private final List<ApiError.FieldViolation> fields;

    public ValidationException(List<ApiError.FieldViolation> fields) {
        super("Dados inválidos");
        this.fields = fields;
    }

    public static ValidationException of(String field, String message) {
        return new ValidationException(List.of(new ApiError.FieldViolation(field, message)));
    }

    public List<ApiError.FieldViolation> getFields() {
        return fields;
    }
}
