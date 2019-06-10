package net.jk.app.commons.boot.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import net.jk.app.commons.boot.validation.PhoneNumber.PhoneNumberValidator;
import org.apache.commons.lang3.StringUtils;

@Retention(RUNTIME)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Constraint(validatedBy = PhoneNumberValidator.class)
public @interface PhoneNumber {

  String message() default "Invalid Phone / Fax number";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  public static class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {
    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
      if (StringUtils.isNotBlank(value)) {
        try {
          return phoneUtil.isValidNumber(phoneUtil.parse(value, "US"));
        } catch (NumberParseException e) {
          return false;
        }
      }
      return true;
    }
  }
}
