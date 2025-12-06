package com.example.shiftv1.schedule;

import com.example.shiftv1.employee.Employee;
import com.example.shiftv1.skill.Skill;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record ScheduleGridEmployeeDto(
        Long id,
        String name,
        String role,
        List<EmployeeSkillDto> skills) {

    public static ScheduleGridEmployeeDto from(Employee employee) {
        List<EmployeeSkillDto> skillDtos = employee.getSkills() == null
                ? List.of()
                : employee.getSkills()
                .stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((Skill s) -> Objects.requireNonNullElse(s.getName(), ""))
                        .thenComparing(s -> Objects.requireNonNullElse(s.getCode(), "")))
                .map(skill -> new EmployeeSkillDto(skill.getId(), skill.getCode(), skill.getName()))
                .toList();
        return new ScheduleGridEmployeeDto(
                employee.getId(),
                employee.getName(),
                employee.getRole(),
                skillDtos
        );
    }

    public record EmployeeSkillDto(Long id, String code, String name) {
    }
}
