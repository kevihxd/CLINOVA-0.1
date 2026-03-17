package com.clinova.dto;

public record StructureResponses<T>(
        String status,
        String message,
        T data
) {}