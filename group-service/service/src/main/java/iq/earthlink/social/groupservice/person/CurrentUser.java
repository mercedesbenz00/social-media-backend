package iq.earthlink.social.groupservice.person;

import iq.earthlink.social.groupservice.person.model.Person;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation should be applied on controller method param with type {@link Person}
 * for getting current authorized user.
 */
@AuthenticationPrincipal(expression = "@personManager.getPersonByUuid(#this)")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CurrentUser {

}
