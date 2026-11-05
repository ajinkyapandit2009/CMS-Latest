package com.cmsbackend.services;

import com.cmsbackend.dtos.CitizenRegistrationRequestDto;
import com.cmsbackend.dtos.UserResponseDto;

public interface UserService {

    UserResponseDto registerUser(CitizenRegistrationRequestDto request);

}
