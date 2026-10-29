
package com.cmsbackend.services;

import java.util.List;

import com.cmsbackend.dtos.AssignComplaintRequestDto;
import com.cmsbackend.dtos.ComplaintRequestDto;
import com.cmsbackend.dtos.ComplaintResponseDto;

public interface ComplaintService {

    ComplaintResponseDto createComplaint(
            ComplaintRequestDto requestDto,
            String email
    );
    
    List<ComplaintResponseDto> getMyComplaints(String email);
    
    ComplaintResponseDto assignComplaint(
            Integer complaintId,
            AssignComplaintRequestDto requestDto,
            String adminEmail
    );
}
