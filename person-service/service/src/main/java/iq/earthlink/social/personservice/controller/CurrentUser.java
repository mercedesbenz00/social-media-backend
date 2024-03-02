package iq.earthlink.social.personservice.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation should be applied on controller method param with type {@link iq.earthlink.social.personservice.person.model.Person}
 * for getting current authorized user.
 */
@AuthenticationPrincipal(expression = "@personManager.getPersonDtoByUuid(#this)")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CurrentUser {

}
