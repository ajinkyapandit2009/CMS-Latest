
package com.cmsbackend.services;

import com.cmsbackend.dtos.UserRequestDto;
import com.cmsbackend.dtos.CaseOfficerResponseDto;

public interface AdminService {
    CaseOfficerResponseDto createCaseOfficerWithProfile(UserRequestDto userRequestDto);
}
