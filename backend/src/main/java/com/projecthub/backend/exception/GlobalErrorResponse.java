package com.projecthub.backend.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class GlobalErrorResponse
{
    private int status;
    private String error;
    private String message;
    private Date timestamp;
}
