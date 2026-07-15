package ru.practicum.ewm.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IpAddressValidator.class)
public @interface ValidIp {
    String message() default "Неверный формат IP-адреса (IPv4 или IPv6)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}