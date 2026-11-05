
package com.cmsbackend.services;

import com.cmsbackend.dtos.CaseOfficerRequestDto;
import com.cmsbackend.dtos.CaseOfficerResponseDto;
import com.cmsbackend.dtos.CaseOfficerWorkloadDto;

import java.util.List;

public interface CaseOfficerService {

    CaseOfficerResponseDto createCaseOfficer(
            CaseOfficerRequestDto caseOfficerRequestDto);

    List<CaseOfficerResponseDto> getAllCaseOfficers();

    List<CaseOfficerWorkloadDto> getOfficerWorkloads();

}
