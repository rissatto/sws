package com.rissatto.sws.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record CreateWalletRequest(@JsonProperty("userId") UUID userId) {
}