package com.car.sharing.app.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;
import org.springframework.beans.BeanWrapperImpl;

public class FieldMatchValidator implements ConstraintValidator<FieldMatch, Object> {
    private String field;
    private String fieldToMatch;

    @Override
    public void initialize(FieldMatch constraintAnnotation) {
        this.field = constraintAnnotation.field();
        this.fieldToMatch = constraintAnnotation.fieldToMatch();
    }

    @Override
    public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
        Object fieldValue = new BeanWrapperImpl(o).getPropertyValue(field);
        Object fieldValueToMatch = new BeanWrapperImpl(o).getPropertyValue(fieldToMatch);

        return Objects.equals(fieldValue, fieldValueToMatch);
    }
}
