
package com.cmsbackend.services;

import java.util.List;

import com.cmsbackend.dtos.AssignComplaintRequestDto;
import com.cmsbackend.dtos.ComplaintRequestDto;
import com.cmsbackend.dtos.ComplaintResponseDto;
import com.cmsbackend.dtos.UpdateComplaintPriorityRequestDto;
import com.cmsbackend.dtos.UpdateComplaintStatusRequestDto;
import com.cmsbackend.enums.ComplaintPriority;
import com.cmsbackend.enums.ComplaintStatus;

public interface ComplaintService {

    ComplaintResponseDto createComplaint(
            ComplaintRequestDto requestDto,
            String email);

    List<ComplaintResponseDto> getMyComplaints(
            String email);

    ComplaintResponseDto assignComplaint(
            Integer complaintId,
            AssignComplaintRequestDto requestDto,
            String adminEmail);

    List<ComplaintResponseDto> getAssignedComplaints(
            String email);

    ComplaintResponseDto updateComplaintStatus(
            Integer complaintId,
            UpdateComplaintStatusRequestDto requestDto,
            String officerEmail);

    List<ComplaintResponseDto> getAllComplaints(
            String adminEmail,
            ComplaintStatus status,
            ComplaintPriority priority);
    
    ComplaintResponseDto updateComplaintPriority(
            Integer complaintId,
            UpdateComplaintPriorityRequestDto requestDto,
            String adminEmail);
    
}
