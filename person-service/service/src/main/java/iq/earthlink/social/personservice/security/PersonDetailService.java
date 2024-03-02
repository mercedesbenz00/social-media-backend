package iq.earthlink.social.personservice.security;

import iq.earthlink.social.personservice.person.PersonManager;
import iq.earthlink.social.personservice.person.model.Person;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The service exposes method for Spring security for getting user details
 */
public class PersonDetailService implements UserDetailsService {

    private final PersonManager personManager;

    public PersonDetailService(PersonManager personManager) {
        this.personManager = personManager;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Person person = personManager.getPersonByUsername(username);
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        person.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));

        return User.builder()
                .username(person.getUsername())
                .password(person.getPassword())
                .authorities(authorities)
                .build();
    }
}
