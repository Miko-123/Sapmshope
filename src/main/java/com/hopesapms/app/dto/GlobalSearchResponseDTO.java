package com.hopesapms.app.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class GlobalSearchResponseDTO {
    private List<SearchResultItem> students;
    private List<SearchResultItem> courses;
    private List<SearchResultItem> instructors;

    @Data
    @Builder
    public static class SearchResultItem {
        private Long id;
        private String title; 
        private String subtitle; 
        private String type; 
        private String url; 
    }
}
