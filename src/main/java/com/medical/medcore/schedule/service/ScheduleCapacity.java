package com.medical.medcore.schedule.service;

import com.medical.medcore.branch.domain.Branch;
import com.medical.medcore.branch.repository.BranchRepository;
import com.medical.medcore.doctor.domain.Doctor;
import com.medical.medcore.doctor.domain.DoctorBranch;
import com.medical.medcore.doctor.repository.DoctorBranchRepository;
import com.medical.medcore.platform.service.TenantTerminology;
import com.medical.medcore.schedule.domain.ExceptionReason;
import com.medical.medcore.schedule.domain.ScheduleException;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleCapacity {

    public static final class Usage {
        private final Long branchId;
        private final String branchName;
        private final Long doctorId;
        private final String doctorName;
        private long templateMinutes;
        private long availableMinutes;
        private long closedMinutes;
        private long blockedMinutes;
        private long extraMinutes;
        private long slots;
        private final Map<ExceptionReason, Long> blockedByReason = new EnumMap<>(ExceptionReason.class);

        Usage(Long branchId, String branchName, Long doctorId, String doctorName) {
            this.branchId = branchId;
            this.branchName = branchName;
            this.doctorId = doctorId;
            this.doctorName = doctorName;
        }

        public Long branchId() { return branchId; }
        public String branchName() { return branchName; }
        public Long doctorId() { return doctorId; }
        public String doctorName() { return doctorName; }
        public long templateMinutes() { return templateMinutes; }
        public long availableMinutes() { return availableMinutes; }
        public long closedMinutes() { return closedMinutes; }
        public long blockedMinutes() { return blockedMinutes; }
        public long extraMinutes() { return extraMinutes; }
        public long slots() { return slots; }
        public Map<ExceptionReason, Long> blockedByReason() { return blockedByReason; }
    }

    private final BranchRepository branchRepository;
    private final DoctorBranchRepository doctorBranchRepository;
    private final WorkingCalendar workingCalendar;
    private final TenantTerminology terminology;

    public List<Usage> usage(LocalDate from, LocalDate to, List<Long> branchIds, Long onlyDoctorId) {
        List<Usage> result = new ArrayList<>();
        for (Branch branch : branches(branchIds)) {
            Map<Long, Doctor> doctors = new LinkedHashMap<>();
            for (DoctorBranch link : doctorBranchRepository.findActiveDoctorsByBranchId(branch.getId())) {
                Doctor doctor = link.getDoctor();
                if (onlyDoctorId == null || onlyDoctorId.equals(doctor.getId())) {
                    doctors.put(doctor.getId(), doctor);
                }
            }
            if (doctors.isEmpty()) {
                continue;
            }
            WorkingCalendar.Snapshot snapshot = workingCalendar.load(doctors.keySet(), branch.getId(), from, to);
            for (Doctor doctor : doctors.values()) {
                Usage usage = new Usage(branch.getId(), branch.getName(), doctor.getId(),
                        terminology.professionalName(doctor.getPerson()));
                for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
                    accumulate(usage, snapshot, doctor.getId(), date);
                }
                result.add(usage);
            }
        }
        return result;
    }

    private void accumulate(Usage usage, WorkingCalendar.Snapshot snapshot, Long doctorId, LocalDate date) {
        long template = snapshot.templateWindows(doctorId, date).stream().mapToLong(WorkWindow::minutes).sum();
        long scheduled = 0;
        for (WorkWindow window : snapshot.windows(doctorId, date)) {
            if (window.source() == WorkWindow.Source.EXTRA) {
                usage.extraMinutes += window.minutes();
            } else {
                scheduled += window.minutes();
            }
            usage.availableMinutes += window.minutes();
            usage.slots += window.minutes() / Math.max(1, window.slotMinutes());
        }
        usage.templateMinutes += template;
        if (snapshot.closure(date).isPresent()) {
            usage.closedMinutes += template;
            return;
        }
        long blocked = Math.max(0, template - scheduled);
        if (blocked == 0) {
            return;
        }
        usage.blockedMinutes += blocked;
        ExceptionReason reason = snapshot.exceptions(doctorId, date).stream()
                .filter(ScheduleException::isBlock)
                .map(ScheduleException::getReason)
                .findFirst()
                .orElse(ExceptionReason.OTHER);
        usage.blockedByReason.merge(reason, blocked, Long::sum);
    }

    private List<Branch> branches(List<Long> branchIds) {
        Long tenantId = TenantContext.requireTenantId();
        if (branchIds == null) {
            return branchRepository.findByTenantIdAndIsActiveTrue(tenantId, Pageable.unpaged()).getContent();
        }
        List<Branch> result = new ArrayList<>();
        for (Long id : branchIds) {
            branchRepository.findByIdAndTenantId(id, tenantId).ifPresent(result::add);
        }
        return result;
    }
}
