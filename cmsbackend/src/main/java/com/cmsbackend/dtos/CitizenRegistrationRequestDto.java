package com.cmsbackend.dtos;

import com.cmsbackend.registration.RegistrationRules;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CitizenRegistrationRequestDto {
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must be 100 characters or fewer")
    @Pattern(regexp = "^[\\p{L}\\p{M}][\\p{L}\\p{M}\\p{Zs}'’.-]*$",
            message = "First name must contain letters and common name punctuation")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must be 100 characters or fewer")
    @Pattern(regexp = "^[\\p{L}\\p{M}][\\p{L}\\p{M}\\p{Zs}'’.-]*$",
            message = "Last name must contain letters and common name punctuation")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Size(max = 254, message = "Email must be 254 characters or fewer")
    @Pattern(regexp = RegistrationRules.EMAIL_PATTERN, message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = RegistrationRules.PASSWORD_PATTERN,
            message = "Use 8 to 72 characters without spaces, including uppercase, lowercase, and a number")
    private String password;

    @NotBlank(message = "Confirm password is required")
    @Size(max = 72, message = "Confirm password must be 72 characters or fewer")
    private String confirmPassword;

    @Size(max = 15, message = "Phone must be 15 characters or fewer")
    @Pattern(regexp = "^\\s*$|" + RegistrationRules.PHONE_PATTERN,
            message = "Phone must contain a digit and use only phone characters")
    private String phone;

    @Size(max = 65535, message = "Address is too long")
    private String address;

    // Explicitly reject role claims instead of silently ignoring them.
    private String role;
    private String userRole;

    public void setEmail(String email) {
        this.email = RegistrationRules.normalizeEmail(email);
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName == null ? null : firstName.trim();
    }

    public void setLastName(String lastName) {
        this.lastName = lastName == null ? null : lastName.trim();
    }
}
