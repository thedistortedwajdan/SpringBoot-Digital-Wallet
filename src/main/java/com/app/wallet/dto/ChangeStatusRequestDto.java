package com.app.wallet.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequestDto(@NotNull Boolean active) {
}
