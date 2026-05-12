package com.hopesapms.app.modules.enrollment.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * A generic DTO to return the results of an import operation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResultDTO {
    
    /**
     * The number of main items successfully created.
     * (e.g., "Course Offerings created" or "Enrollments created")
     */
    private int offeringsCreated; 
    
    /**
     * The number of related sub-items created.
     * (e.g., "Schedule Slots added")
     * We don't use this for enrollment, so we will just pass 0.
     */
    private int scheduleSlotsAdded; 

    /**
     * A list of error messages for rows that failed.
     */
    private List<String> errors;
}