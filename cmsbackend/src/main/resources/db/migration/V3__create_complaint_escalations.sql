-- Run manually in the CMS MySQL schema before starting this backend version.
-- NULL markers allow multiple reviewed escalations but only one PENDING row per complaint.
CREATE TABLE complaint_escalations (
    escalation_id INT NOT NULL AUTO_INCREMENT,
    complaint_id INT NOT NULL,
    citizen_id INT NOT NULL,
    reason TEXT NOT NULL,
    review_status VARCHAR(20) NOT NULL,
    pending_marker INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP NULL,
    reviewed_by INT NULL,
    remarks TEXT NULL,
    PRIMARY KEY (escalation_id),
    UNIQUE KEY uk_complaint_escalations_pending (complaint_id, pending_marker),
    KEY idx_complaint_escalations_citizen_created (citizen_id, created_at),
    KEY idx_complaint_escalations_status_created (review_status, created_at),
    CONSTRAINT fk_complaint_escalations_complaint FOREIGN KEY (complaint_id) REFERENCES complaint(complaint_id),
    CONSTRAINT fk_complaint_escalations_citizen FOREIGN KEY (citizen_id) REFERENCES citizen(user_id),
    CONSTRAINT fk_complaint_escalations_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(user_id)
);
