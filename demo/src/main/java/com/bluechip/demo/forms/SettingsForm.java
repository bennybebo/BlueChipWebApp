package com.bluechip.demo.forms;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;

public class SettingsForm {
    @PositiveOrZero
    private Double bankrollDollars;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Double preferredKellyFraction;

    public Double getBankrollDollars() { return bankrollDollars; }
    public void setBankrollDollars(Double bankrollDollars) { this.bankrollDollars = bankrollDollars; }

    public Double getPreferredKellyFraction() { return preferredKellyFraction; }
    public void setPreferredKellyFraction(Double preferredKellyFraction) { this.preferredKellyFraction = preferredKellyFraction; }
}
