package com.app.wallet.dto;

import com.app.wallet.model.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequestDto(@NotNull Role role) {
}
