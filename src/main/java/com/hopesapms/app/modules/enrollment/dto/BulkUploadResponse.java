package com.hopesapms.app.modules.enrollment.dto;

import lombok.Data;
import java.util.List;

@Data
public class BulkUploadResponse {
    private int successCount;
    private int failedCount;
    private List<String> errors;
}