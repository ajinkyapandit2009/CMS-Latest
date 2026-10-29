
package com.cmsbackend.dtos;

import lombok.Data;

@Data
public class CaseOfficerResponseDto {

    private Integer userId;

    private String firstName;

    private String lastName;

    private String email;

    private String employeeNumber;
}
