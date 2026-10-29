package com.cmsbackend.services;

import com.cmsbackend.dtos.UserRequestDto;
import com.cmsbackend.dtos.UserResponseDto;

public interface UserService {

    UserResponseDto registerUser(UserRequestDto userRequestDto);

}
