package iq.earthlink.social.postservice.person;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation should be applied on controller method param with type {@link iq.earthlink.social.postservice.person.model.Person}
 * for getting current authorized user.
 */
@AuthenticationPrincipal(expression = "@personManager.getPersonByUuid(#this)")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CurrentUser {

}
