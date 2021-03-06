package com.gluteen.web;

import com.gluteen.domain.Person;
import com.gluteen.model.ChangePassword;
import com.gluteen.model.PersonView;
import com.gluteen.model.ProfileInformation;
import com.gluteen.model.SignUp;
import com.gluteen.security.CurrentProfile;
import com.gluteen.service.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;

import static com.gluteen.configuration.Constants.*;

/**
 * Created by yusufaslan on 3.06.2017.
 */
@RestController
@RequestMapping(value = URI_API_PREFIX)
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    @Autowired
    private PersonService personService;

    @GetMapping("/login")
    public ResponseEntity<PersonView> login(@CurrentProfile Person profile)
    {
        log.debug("REST request to get current profile: {}", profile);

        if (null == profile) {
            log.warn("Attempt getting unauthorised profile information failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(new PersonView(profile));
    }

    @PostMapping("/signUp")
    public ResponseEntity<String> signUp(@Valid @RequestBody SignUp person) throws URISyntaxException {
        log.debug("REST request to sign up a new profile: {}", person);

        final Person result = personService.findByEmail(person.getUserName());
        if (null != result) {
            log.debug("Attempt sign up email: {} failed! E-mail is already used by another contact: {}",
                    person.getUserName(), result);

            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(ERROR_SIGN_UP_EMAIL);
        }

        final Person profile = personService.create(
                person.getFirstName(),
                person.getLastName(),
                person.getUserName(),
                person.getPassword());

        return ResponseEntity.created(new URI(URI_API_PREFIX + "/person/" + profile.getId())).build();
    }

    @PutMapping("/updateContact")
    public ResponseEntity<String> updatePerson(
            @CurrentProfile Person profile,
            @Valid @RequestBody ProfileInformation contact) {
        log.debug("REST request to update current profile: {} contact information", profile);

        if (!profile.getId().equals(contact.getId())) {
            log.error("Updating profile: {} doesn't match the current one: {}", contact, profile);

            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(ERROR_UPDATE_PROFILE);
        }

        final String oldEmail = profile.getEmail();
        final String newEmail = contact.getEmail();
        if (!oldEmail.equals(newEmail)) {
            final Person result = personService.findByEmail(newEmail);
            if (null != result) {
                log.debug("Attempt to change email value from: {} to  {} failed! " +
                        "E-mail is already used by another contact : {}", result);

                return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(ERROR_UPDATE_EMAIL);
            }
        }

        profile.setFirstName(contact.getFirstName());
        profile.setLastName(contact.getLastName());
        profile.setEmail(contact.getEmail());
        profile.setPhone(contact.getPhone());
        profile.setBirthDate(contact.getBirthDate());
        profile.setGender(contact.getGender());
        personService.update(profile);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/changePassword")
    public ResponseEntity<?> changePassword(
            @CurrentProfile Person profile,
            @Valid @RequestBody ChangePassword pwd) throws URISyntaxException {
        log.debug("REST request to change pwd: {}", pwd);

        if (null == profile) {
            log.warn("Attempt to change unauthorised profile password failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        final String currentPwd = pwd.getCurrentPassword();
        final String newPwd = pwd.getPassword();
        if(!personService.hasValidPassword(profile, currentPwd)) {
            log.warn("Current password: {} doesn't match profile's one: {}", currentPwd, profile);
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(ERROR_PASSWORD_CONFIRMATION);
        }

        personService.changePassword(profile, newPwd);

        return ResponseEntity.ok().build();
    }





}
