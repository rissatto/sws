package com.rissatto.sws.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(@JsonProperty("targetWalletId") UUID targetWalletId, BigDecimal amount) {
}