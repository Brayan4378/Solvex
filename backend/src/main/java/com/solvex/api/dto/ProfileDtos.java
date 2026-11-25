package com.solvex.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ProfileDtos {
    private ProfileDtos() { }
    public record UpdateRequest(@NotBlank @Size(max = 100) String nombre) { }
}
