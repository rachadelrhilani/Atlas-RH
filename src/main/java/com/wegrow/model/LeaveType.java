package com.wegrow.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leave_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // Paid Leave, Sick Leave...

    @Column(name = "default_days", nullable = false)
    private Integer defaultDays;
}