package com.cmsbackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

import com.cmsbackend.dtos.CitizenRegistrationRequestDto;
import com.cmsbackend.dtos.UserResponseDto;
import com.cmsbackend.registration.EmailCheckRateLimiter;
import com.cmsbackend.registration.RegistrationException;
import com.cmsbackend.registration.RegistrationRules;
import com.cmsbackend.repositories.UserRepository;
import com.cmsbackend.services.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailCheckRateLimiter emailCheckRateLimiter;

    // POST - https://localhost:8080/users
    @PostMapping
    public ResponseEntity<UserResponseDto> register(
            @Valid @RequestBody CitizenRegistrationRequestDto userRequestDto) {

        UserResponseDto responseDto =
                userService.registerUser(userRequestDto);

        return new ResponseEntity<UserResponseDto>(
                responseDto, HttpStatus.CREATED);
    }

    // The response intentionally contains only a Boolean. Availability is advisory; POST /users is authoritative.
    @GetMapping("/email-availability")
    public ResponseEntity<EmailAvailability> emailAvailability(
            @RequestParam(required = false) String email, HttpServletRequest request) {
        if (!emailCheckRateLimiter.allow(request.getRemoteAddr())) {
            throw new RegistrationException(HttpStatus.TOO_MANY_REQUESTS, "email", "Too many email checks. Try again later");
        }
        if (!RegistrationRules.validEmail(email)) {
            throw new RegistrationException(HttpStatus.BAD_REQUEST, "email", "Enter a valid email address");
        }
        boolean available = !userRepository.existsByEmailIgnoreCase(RegistrationRules.normalizeEmail(email));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new EmailAvailability(available));
    }

    public record EmailAvailability(boolean available) { }
}
