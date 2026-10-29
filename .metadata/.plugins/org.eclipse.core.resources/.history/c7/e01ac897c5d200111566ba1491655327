
package com.cmsbackend.services.impl;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cmsbackend.dtos.AssignComplaintRequestDto;
import com.cmsbackend.dtos.ComplaintRequestDto;
import com.cmsbackend.dtos.ComplaintResponseDto;
import com.cmsbackend.entities.CaseOfficer;
import com.cmsbackend.entities.Citizen;
import com.cmsbackend.entities.Complaint;
import com.cmsbackend.entities.ComplaintHistory;
import com.cmsbackend.entities.User;
import com.cmsbackend.enums.ComplaintPriority;
import com.cmsbackend.enums.ComplaintStatus;
import com.cmsbackend.enums.Role;
import com.cmsbackend.repositories.CaseOfficerRepository;
import com.cmsbackend.repositories.CitizenRepository;
import com.cmsbackend.repositories.ComplaintHistoryRepository;
import com.cmsbackend.repositories.ComplaintRepository;
import com.cmsbackend.repositories.UserRepository;
import com.cmsbackend.services.ComplaintService;

@Service
public class ComplaintServiceImpl implements ComplaintService {

	@Autowired
	private ComplaintRepository complaintRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CitizenRepository citizenRepository;

	@Autowired
	private ModelMapper modelMapper;

	@Autowired
	private CaseOfficerRepository caseOfficerRepository;

	@Autowired
	private ComplaintHistoryRepository complaintHistoryRepository;

	@Override
	@Transactional
	public ComplaintResponseDto createComplaint(ComplaintRequestDto requestDto, String email) {

		// 1. Find the logged-in user
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found"));

		// 2. Only citizens can submit complaints
		if (user.getUserRole() != Role.ROLE_CITIZEN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only citizens can submit complaints");
		}

		// 3. Find the citizen profile
		Citizen citizen = citizenRepository.findById(user.getUserId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Citizen profile not found"));

		// 4. Convert the request DTO into an entity
		Complaint complaint = modelMapper.map(requestDto, Complaint.class);

		// 5. Set the relationships and initial values
		complaint.setCitizen(citizen);
		complaint.setStatus(ComplaintStatus.SUBMITTED);

		if (requestDto.getPriority() == null) {
			complaint.setPriority(ComplaintPriority.MEDIUM);
		}

		// 6. Save the complaint
		Complaint savedComplaint = complaintRepository.save(complaint);

		// 7. Convert the saved entity into a response DTO
		ComplaintResponseDto responseDto = modelMapper.map(savedComplaint, ComplaintResponseDto.class);

		responseDto.setCitizenId(citizen.getUserId());
		responseDto.setOfficerId(null);

		return responseDto;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ComplaintResponseDto> getMyComplaints(String email) {

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found"));

		if (user.getUserRole() != Role.ROLE_CITIZEN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only citizens can view their complaints");
		}

		Citizen citizen = citizenRepository.findById(user.getUserId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Citizen profile not found"));

		List<Complaint> complaints = complaintRepository.findByCitizen_UserIdOrderByCreatedAtDesc(citizen.getUserId());

		return complaints.stream().map(complaint -> {
			ComplaintResponseDto dto = modelMapper.map(complaint, ComplaintResponseDto.class);

			dto.setCitizenId(citizen.getUserId());
			dto.setOfficerId(complaint.getOfficer() == null ? null : complaint.getOfficer().getUserId());

			return dto;
		}).toList();
	}

	@Override
	@Transactional
	public ComplaintResponseDto assignComplaint(Integer complaintId, AssignComplaintRequestDto requestDto,
			String adminEmail) {

		// 1. Find the complaint
		Complaint complaint = complaintRepository.findById(complaintId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));

		// 2. Find the selected case officer
		CaseOfficer officer = caseOfficerRepository.findById(requestDto.getOfficerId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case officer profile not found"));

		// 3. Find the logged-in admin
		User admin = userRepository.findByEmail(adminEmail)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin account not found"));

		if (admin.getUserRole() != Role.ROLE_ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can assign complaints");
		}

		// 4. Prevent assignment of completed or rejected complaints
		if (complaint.getStatus() == ComplaintStatus.RESOLVED || complaint.getStatus() == ComplaintStatus.REJECTED
				|| complaint.getStatus() == ComplaintStatus.IN_PROGRESS) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"This complaint cannot be assigned in its current status");
		}

		// 5. Record the previous status
		String previousStatus = complaint.getStatus() == null ? null : complaint.getStatus().name();

		// 6. Assign the officer and update the status
		complaint.setOfficer(officer);
		complaint.setStatus(ComplaintStatus.ASSIGNED);

		Complaint savedComplaint = complaintRepository.save(complaint);

		// 7. Record the assignment in complaint_history
		ComplaintHistory history = new ComplaintHistory();

		history.setComplaint(savedComplaint);
		history.setPreviousStatus(previousStatus);
		history.setNewStatus(ComplaintStatus.ASSIGNED.name());
		history.setUpdatedBy(admin);

		String remarks = requestDto.getRemarks();

		if (remarks == null || remarks.isBlank()) {
			remarks = "Assigned to case officer " + officer.getEmployeeNumber();
		}

		history.setRemarks(remarks.trim());

		complaintHistoryRepository.save(history);

		// 8. Return the complaint response
		ComplaintResponseDto responseDto = modelMapper.map(savedComplaint, ComplaintResponseDto.class);

		responseDto.setCitizenId(savedComplaint.getCitizen().getUserId());

		responseDto.setOfficerId(savedComplaint.getOfficer().getUserId());

		return responseDto;
	}
	
	
	@Override
	@Transactional(readOnly = true)
	public List<ComplaintResponseDto> getAssignedComplaints(String email) {

	    User user = userRepository.findByEmail(email)
	            .orElseThrow(() -> new ResponseStatusException(
	                    HttpStatus.NOT_FOUND,
	                    "User account not found"));

	    if (user.getUserRole() != Role.ROLE_OFFICER) {
	        throw new ResponseStatusException(
	                HttpStatus.FORBIDDEN,
	                "Only case officers can view assigned complaints");
	    }

	    CaseOfficer officer = caseOfficerRepository.findById(user.getUserId())
	            .orElseThrow(() -> new ResponseStatusException(
	                    HttpStatus.NOT_FOUND,
	                    "Case officer profile not found"));

	    List<Complaint> complaints =
	            complaintRepository.findByOfficer_UserIdOrderByCreatedAtDesc(
	                    officer.getUserId());

	    return complaints.stream().map(complaint -> {
	        ComplaintResponseDto dto =
	                modelMapper.map(complaint, ComplaintResponseDto.class);

	        dto.setCitizenId(complaint.getCitizen().getUserId());
	        dto.setOfficerId(officer.getUserId());

	        return dto;
	    }).toList();
	}

}
