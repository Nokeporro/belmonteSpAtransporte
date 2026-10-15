package com.belmontespa.transporte.dto.response;
import java.time.Instant;
public record ApiError(Instant timestamp, int status, String error) {}
