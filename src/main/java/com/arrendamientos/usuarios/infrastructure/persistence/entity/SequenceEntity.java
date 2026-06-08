package com.arrendamientos.usuarios.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Sequences")
public class SequenceEntity {

    @Id
    @Column(name = "Name", length = 50, nullable = false)
    private String name;

    @Column(name = "CurrentValue", nullable = false)
    private int currentValue;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCurrentValue() { return currentValue; }
    public void setCurrentValue(int currentValue) { this.currentValue = currentValue; }
}
