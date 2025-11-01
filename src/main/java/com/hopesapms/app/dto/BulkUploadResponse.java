package com.hopesapms.app.dto;

import lombok.Data;
import java.util.List;

@Data
public class BulkUploadResponse {
    private int successCount;
    private int failedCount;
    private List<String> errors;
}