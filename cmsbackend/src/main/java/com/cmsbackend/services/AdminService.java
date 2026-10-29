
package com.cmsbackend.services;

import com.cmsbackend.dtos.UserRequestDto;
import com.cmsbackend.dtos.UserResponseDto;

public interface AdminService {

    UserResponseDto createCaseOfficerAccount(
            UserRequestDto userRequestDto);
}
