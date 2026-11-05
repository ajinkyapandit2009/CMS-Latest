package com.cmsbackend.repositories;

public interface OfficerWorkloadCount {
    Integer getUserId();
    String getFirstName();
    String getLastName();
    String getEmployeeNumber();
    Long getAssignedCount();
    Long getInProgressCount();
}
