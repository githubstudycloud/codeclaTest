package com.mycompany.dynamicquery.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Entity
@Table(name = "data_record")
public class DataRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", length = 100)
    private String name;
    
    @Column(name = "age")
    private Integer age;
    
    @Column(name = "status", length = 50)
    private String status;
    
    @Column(name = "score")
    private Double score;
    
    @Column(name = "department", length = 100)
    private String department;
    
    @Column(name = "email", length = 200)
    private String email;
    
    @Column(name = "phone", length = 20)
    private String phone;
    
    @Column(name = "salary")
    private Double salary;
    
    @Column(name = "hire_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime hireDate;
    
    @Column(name = "created_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
    
    @Column(name = "updated_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
    
    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
        updatedTime = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedTime = LocalDateTime.now();
    }
}