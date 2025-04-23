package com.rissatto.sws.presentation.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(UUID targetWalletId, BigDecimal amount) {
}