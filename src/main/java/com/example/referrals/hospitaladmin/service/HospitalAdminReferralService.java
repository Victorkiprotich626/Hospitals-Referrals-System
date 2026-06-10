package com.example.referrals.hospitaladmin.service;

import com.example.referrals.common.web.CurrentUserFacade;
import com.example.referrals.directory.Department;
import com.example.referrals.directory.DepartmentRepository;
import com.example.referrals.directory.Doctor;
import com.example.referrals.directory.DoctorRepository;
import com.example.referrals.hospital.Hospital;
import com.example.referrals.hospital.HospitalRepository;
import com.example.referrals.hospitaladmin.form.ReferralAssignmentForm;
import com.example.referrals.hospitaladmin.form.ReferralClosureForm;
import com.example.referrals.hospitaladmin.form.ReferralForm;
import com.example.referrals.hospitaladmin.form.ReferralNoteForm;
import com.example.referrals.hospitaladmin.form.ReferralStatusForm;
import com.example.referrals.notification.NotificationService;
import com.example.referrals.patient.Patient;
import com.example.referrals.patient.PatientRepository;
import com.example.referrals.referral.Referral;
import com.example.referrals.referral.ReferralClosureOutcome;
import com.example.referrals.referral.ReferralEvent;
import com.example.referrals.referral.ReferralEventRepository;
import com.example.referrals.referral.ReferralEventType;
import com.example.referrals.referral.ReferralPriority;
import com.example.referrals.referral.ReferralRepository;
import com.example.referrals.referral.ReferralStatus;
import com.example.referrals.security.CustomUserDetails;
import com.example.referrals.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class HospitalAdminReferralService {

    private static final Set<ReferralStatus> CLOSED_STATUSES =
        EnumSet.of(ReferralStatus.REJECTED, ReferralStatus.COMPLETED, ReferralStatus.CANCELLED);
    private static final Set<ReferralStatus> INCOMING_ATTENTION_STATUSES =
        EnumSet.of(ReferralStatus.SUBMITTED, ReferralStatus.RECEIVED, ReferralStatus.UNDER_REVIEW);
    private static final Set<ReferralClosureOutcome> COMPLETED_OUTCOMES =
        EnumSet.of(ReferralClosureOutcome.TREATED, ReferralClosureOutcome.ADMITTED);
    private static final Set<ReferralClosureOutcome> NON_TREATMENT_OUTCOMES =
        EnumSet.of(ReferralClosureOutcome.REDIRECTED, ReferralClosureOutcome.CLOSED_WITHOUT_TREATMENT);

    private final ReferralRepository referralRepository;
    private final ReferralEventRepository referralEventRepository;
    private final PatientRepository patientRepository;
    private final HospitalRepository hospitalRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final CurrentUserFacade currentUserFacade;
    private final NotificationService notificationService;

    public HospitalAdminReferralService(ReferralRepository referralRepository,
                                        ReferralEventRepository referralEventRepository,
                                        PatientRepository patientRepository,
                                        HospitalRepository hospitalRepository,
                                        DepartmentRepository departmentRepository,
                                        DoctorRepository doctorRepository,
                                        CurrentUserFacade currentUserFacade,
                                        NotificationService notificationService) {
        this.referralRepository = referralRepository;
        this.referralEventRepository = referralEventRepository;
        this.patientRepository = patientRepository;
        this.hospitalRepository = hospitalRepository;
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
        this.currentUserFacade = currentUserFacade;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<Referral> findAllVisibleToCurrentTenant() {
        return referralRepository.findAllVisibleToHospital(requireTenantId());
    }

    @Transactional(readOnly = true)
    public List<Referral> findAllVisibleToCurrentTenant(String query, ReferralStatus status, String direction) {
        Long tenantId = requireTenantId();
        String normalizedQuery = normalizeQuery(query);
        return referralRepository.findAllVisibleToHospital(tenantId).stream()
            .filter(referral -> status == null || referral.getStatus() == status)
            .filter(referral -> matchesDirection(referral, tenantId, direction))
            .filter(referral -> normalizedQuery == null || matchesReferral(referral, tenantId, normalizedQuery))
            .toList();
    }

    @Transactional(readOnly = true)
    public Referral findVisibleById(Long referralId) {
        return referralRepository.findVisibleById(referralId, requireTenantId())
            .orElseThrow(() -> new EntityNotFoundException("Referral not found"));
    }

    @Transactional(readOnly = true)
    public List<ReferralEvent> findTimeline(Long referralId) {
        return findTimeline(referralId, null);
    }

    @Transactional(readOnly = true)
    public List<ReferralEvent> findTimeline(Long referralId, ReferralEventType eventType) {
        findVisibleById(referralId);
        if (eventType == null) {
            return referralEventRepository.findAllByReferralIdOrderByCreatedAtDesc(referralId);
        }
        return referralEventRepository.findAllByReferralIdAndEventTypeOrderByCreatedAtDesc(referralId, eventType);
    }

    @Transactional(readOnly = true)
    public List<Hospital> findReferralDestinations() {
        Long tenantId = requireTenantId();
        return hospitalRepository.findAllByEnabledTrueOrderByNameAsc().stream()
            .filter(hospital -> !hospital.getId().equals(tenantId))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<Patient> findPatientsForCurrentTenant() {
        return patientRepository.findAllByHospitalIdOrderByLastNameAscFirstNameAsc(requireTenantId());
    }

    public Referral create(ReferralForm form) {
        Long tenantId = requireTenantId();
        Hospital fromHospital = hospitalRepository.findById(tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Current hospital not found"));
        Hospital toHospital = hospitalRepository.findById(form.getToHospitalId())
            .filter(Hospital::isEnabled)
            .orElseThrow(() -> new IllegalArgumentException("Receiving hospital is invalid or disabled."));
        if (tenantId.equals(toHospital.getId())) {
            throw new IllegalArgumentException("Receiving hospital must be different from your own hospital.");
        }

        Referral sourceReferral = null;
        Patient patient;
        String journeyCode;

        if (form.getSourceReferralId() != null) {
            sourceReferral = findVisibleById(form.getSourceReferralId());
            if (!sourceReferral.isIncomingFor(tenantId)) {
                throw new IllegalArgumentException("Only the receiving hospital can forward this referral onwards.");
            }
            patient = resolveForwardedPatient(sourceReferral, tenantId, fromHospital);
            journeyCode = sourceReferral.getJourneyCode();
        } else {
            if (form.getPatientId() == null) {
                throw new IllegalArgumentException("Select a patient.");
            }
            patient = patientRepository.findByIdAndHospitalId(form.getPatientId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Selected patient does not belong to your hospital."));
            journeyCode = generateJourneyCode(fromHospital);
        }

        Referral referral = new Referral();
        referral.setReferenceNumber(generateReferenceNumber(fromHospital));
        referral.setJourneyCode(journeyCode);
        referral.setParentReferral(sourceReferral);
        referral.setPatient(patient);
        referral.setFromHospital(fromHospital);
        referral.setToHospital(toHospital);
        referral.setPriority(defaultPriority(form.getPriority()));
        referral.setStatus(ReferralStatus.SUBMITTED);
        referral.setSubject(form.getSubject().trim());
        referral.setReferralReason(form.getReferralReason().trim());
        referral.setClinicalSummary(normalizeNullable(form.getClinicalSummary()));
        referral.setReceivingDepartment(normalizeNullable(form.getReceivingDepartment()));
        Referral saved = referralRepository.save(referral);

        String creationMessage = sourceReferral == null
            ? "Referral submitted to " + toHospital.getName() + " for " + patient.getFullName() + "."
            : "Referral forwarded onward to " + toHospital.getName() + " for " + patient.getFullName()
                + " within journey " + journeyCode + ".";
        createEvent(saved, ReferralEventType.CREATED, null, ReferralStatus.SUBMITTED, creationMessage);
        notificationService.notifyReferralCreated(saved);
        return saved;
    }

    public void updateStatus(Long referralId, ReferralStatusForm form) {
        Referral referral = findVisibleById(referralId);
        Long tenantId = requireTenantId();
        ReferralStatus nextStatus = form.getStatus();
        ReferralStatus currentStatus = referral.getStatus();

        validateStatusTransition(referral, tenantId, currentStatus, nextStatus, form);

        referral.setStatus(nextStatus);
        applyClosureDetails(referral, nextStatus, form);
        String details = buildStatusDetails(currentStatus, nextStatus, form);
        createEvent(referral, ReferralEventType.STATUS_CHANGED, currentStatus, nextStatus, details);
        notificationService.notifyReferralStatusChanged(referral, currentStatus, nextStatus);
    }

    public void addNote(Long referralId, ReferralNoteForm form) {
        Referral referral = findVisibleById(referralId);
        createEvent(referral, ReferralEventType.NOTE_ADDED, null, null, form.getNote().trim());
        notificationService.notifyReferralNoteAdded(referral);
    }

    public void assign(Long referralId, ReferralAssignmentForm form) {
        Referral referral = findVisibleById(referralId);
        Long tenantId = requireTenantId();
        if (!referral.isIncomingFor(tenantId)) {
            throw new IllegalArgumentException("Only the receiving hospital can assign this referral internally.");
        }
        if (CLOSED_STATUSES.contains(referral.getStatus())) {
            throw new IllegalArgumentException("Closed referrals cannot be reassigned.");
        }
        if (form.getDepartmentId() == null && form.getDoctorId() == null) {
            throw new IllegalArgumentException("Select a department, a doctor, or both.");
        }

        Department department = null;
        Doctor doctor = null;

        if (form.getDepartmentId() != null) {
            department = departmentRepository.findByIdAndHospitalId(form.getDepartmentId(), tenantId)
                .filter(Department::isEnabled)
                .orElseThrow(() -> new IllegalArgumentException("Selected department is invalid or disabled."));
        }
        if (form.getDoctorId() != null) {
            doctor = doctorRepository.findByIdAndHospitalId(form.getDoctorId(), tenantId)
                .filter(Doctor::isEnabled)
                .orElseThrow(() -> new IllegalArgumentException("Selected doctor is invalid or disabled."));
        }
        if (department != null && doctor != null && doctor.getDepartment() != null
            && !doctor.getDepartment().getId().equals(department.getId())) {
            throw new IllegalArgumentException("Selected doctor does not belong to the chosen department.");
        }

        referral.setAssignedDepartment(department);
        referral.setAssignedDoctor(doctor);

        String target = assignmentTargetLabel(department, doctor);
        createEvent(referral, ReferralEventType.ASSIGNED, null, null, "Referral routed to " + target + ".");
        notificationService.notifyReferralAssigned(referral);
    }

    @Transactional(readOnly = true)
    public List<ReferralStatus> allowedTransitions(Referral referral) {
        Long tenantId = requireTenantId();
        if (referral.isIncomingFor(tenantId)) {
            return switch (referral.getStatus()) {
                case SUBMITTED -> List.of(ReferralStatus.RECEIVED);
                case RECEIVED -> List.of(ReferralStatus.UNDER_REVIEW, ReferralStatus.REJECTED);
                case UNDER_REVIEW -> List.of(ReferralStatus.ACCEPTED, ReferralStatus.REJECTED);
                case ACCEPTED -> List.of(ReferralStatus.COMPLETED);
                default -> List.of();
            };
        }
        if (referral.isOutgoingFor(tenantId)) {
            return switch (referral.getStatus()) {
                case SUBMITTED, RECEIVED, UNDER_REVIEW -> List.of(ReferralStatus.CANCELLED);
                default -> List.of();
            };
        }
        return List.of();
    }

    @Transactional(readOnly = true)
    public long countOpenForCurrentTenant() {
        return referralRepository.countOpenVisibleToHospital(requireTenantId(), CLOSED_STATUSES);
    }

    @Transactional(readOnly = true)
    public long countIncomingAttentionForCurrentTenant() {
        return referralRepository.countIncomingByStatuses(requireTenantId(), INCOMING_ATTENTION_STATUSES);
    }

    @Transactional(readOnly = true)
    public List<Referral> findJourneyForCurrentTenant(String journeyCode) {
        return referralRepository.findJourneyVisibleToHospital(journeyCode, requireTenantId());
    }

    @Transactional(readOnly = true)
    public List<ReferralEvent> findJourneyTimelineForCurrentTenant(Long referralId, ReferralEventType eventType) {
        Referral referral = findVisibleById(referralId);
        return referralEventRepository.findJourneyTimelineVisibleToHospital(referral.getJourneyCode(), requireTenantId()).stream()
            .filter(event -> eventType == null || event.getEventType() == eventType)
            .toList();
    }

    @Transactional(readOnly = true)
    public ReferralForm buildForwardForm(Long referralId) {
        Referral sourceReferral = findVisibleById(referralId);
        Long tenantId = requireTenantId();
        if (!sourceReferral.isIncomingFor(tenantId)) {
            throw new IllegalArgumentException("Only the receiving hospital can forward this referral onwards.");
        }

        ReferralForm form = new ReferralForm();
        form.setSourceReferralId(sourceReferral.getId());
        form.setPriority(sourceReferral.getPriority());
        form.setSubject(sourceReferral.getSubject());
        form.setReferralReason(sourceReferral.getReferralReason());
        form.setClinicalSummary(sourceReferral.getClinicalSummary());
        form.setReceivingDepartment(sourceReferral.getReceivingDepartment());
        return form;
    }

    @Transactional(readOnly = true)
    public boolean canForward(Referral referral) {
        Long tenantId = requireTenantId();
        return referral.isIncomingFor(tenantId) && !CLOSED_STATUSES.contains(referral.getStatus());
    }

    @Transactional(readOnly = true)
    public List<Department> findEnabledDepartmentsForCurrentTenant() {
        return departmentRepository.findAllByHospitalIdAndEnabledTrueOrderByNameAsc(requireTenantId());
    }

    @Transactional(readOnly = true)
    public List<Doctor> findEnabledDoctorsForCurrentTenant() {
        return doctorRepository.findAllEnabledByHospitalId(requireTenantId());
    }

    @Transactional(readOnly = true)
    public List<Referral> findAssignedToCurrentDoctor() {
        Long doctorId = currentUserFacade.requireUser().getDoctorProfileId();
        if (doctorId == null) {
            throw new IllegalStateException("Doctor profile is not linked to the current user.");
        }
        return referralRepository.findAllAssignedToDoctor(doctorId);
    }

    @Transactional(readOnly = true)
    public List<Referral> findAssignedToCurrentDoctor(String query, ReferralStatus status) {
        String normalizedQuery = normalizeQuery(query);
        return findAssignedToCurrentDoctor().stream()
            .filter(referral -> status == null || referral.getStatus() == status)
            .filter(referral -> normalizedQuery == null || matchesDoctorReferral(referral, normalizedQuery))
            .toList();
    }

    @Transactional(readOnly = true)
    public Referral findAssignedReferralForCurrentDoctor(Long referralId) {
        Long doctorId = currentUserFacade.requireUser().getDoctorProfileId();
        if (doctorId == null) {
            throw new IllegalStateException("Doctor profile is not linked to the current user.");
        }
        return referralRepository.findAssignedToDoctor(referralId, doctorId)
            .orElseThrow(() -> new EntityNotFoundException("Referral not found"));
    }

    @Transactional(readOnly = true)
    public long countAssignedOpenForCurrentDoctor() {
        Long doctorId = currentUserFacade.requireUser().getDoctorProfileId();
        if (doctorId == null) {
            return 0;
        }
        return referralRepository.countOpenAssignedToDoctor(doctorId, CLOSED_STATUSES);
    }

    public void addDoctorNote(Long referralId, ReferralNoteForm form) {
        Referral referral = findAssignedReferralForCurrentDoctor(referralId);
        createEvent(referral, ReferralEventType.NOTE_ADDED, null, null, form.getNote().trim());
        notificationService.notifyReferralNoteAdded(referral);
    }

    public void recordEvent(Referral referral, ReferralEventType eventType, String details) {
        createEvent(referral, eventType, null, null, details);
    }

    @Transactional(readOnly = true)
    public ReferralClosureForm buildClosureForm(Referral referral) {
        ReferralClosureForm form = new ReferralClosureForm();
        form.setClosureOutcome(referral.getClosureOutcome());
        form.setClosureSummary(referral.getClosureSummary());
        return form;
    }

    public void updateClosureOutcome(Long referralId, ReferralClosureForm form) {
        Referral referral = findVisibleById(referralId);
        if (!referral.isClosed()) {
            throw new IllegalArgumentException("Close the referral first before updating its final outcome.");
        }

        validateClosureOutcome(referral.getStatus(), toStatusForm(form));
        referral.setClosureOutcome(form.getClosureOutcome());
        referral.setClosureSummary(normalizeNullable(form.getClosureSummary()));

        String details = "Final outcome updated to " + form.getClosureOutcome().getDisplayName() + ".";
        if (StringUtils.hasText(form.getClosureSummary())) {
            details = details + " " + form.getClosureSummary().trim();
        }
        createEvent(referral, ReferralEventType.OUTCOME_UPDATED, null, null, details);
    }

    private void validateStatusTransition(Referral referral,
                                          Long tenantId,
                                          ReferralStatus currentStatus,
                                          ReferralStatus nextStatus,
                                          ReferralStatusForm form) {
        if (!allowedTransitions(referral).contains(nextStatus)) {
            throw new IllegalArgumentException("That status change is not allowed for this referral.");
        }

        boolean incomingAction = referral.isIncomingFor(tenantId);
        boolean outgoingAction = referral.isOutgoingFor(tenantId);
        if (!incomingAction && !outgoingAction) {
            throw new IllegalArgumentException("You do not have access to update this referral.");
        }

        if (nextStatus == ReferralStatus.CANCELLED && !outgoingAction) {
            throw new IllegalArgumentException("Only the originating hospital can cancel a referral.");
        }

        if (nextStatus != ReferralStatus.CANCELLED && !incomingAction) {
            throw new IllegalArgumentException("Only the receiving hospital can apply that status.");
        }

        if (currentStatus == nextStatus) {
            throw new IllegalArgumentException("Referral is already in that status.");
        }

        validateClosureOutcome(nextStatus, form);
    }

    private void validateClosureOutcome(ReferralStatus nextStatus, ReferralStatusForm form) {
        if (!CLOSED_STATUSES.contains(nextStatus)) {
            return;
        }

        ReferralClosureOutcome outcome = resolveClosureOutcome(nextStatus, form);

        if (nextStatus == ReferralStatus.COMPLETED && !COMPLETED_OUTCOMES.contains(outcome)) {
            throw new IllegalArgumentException("Completed referrals must end with a treated or admitted outcome.");
        }

        if ((nextStatus == ReferralStatus.REJECTED || nextStatus == ReferralStatus.CANCELLED)
            && !NON_TREATMENT_OUTCOMES.contains(outcome)) {
            throw new IllegalArgumentException("Rejected or cancelled referrals must be marked redirected or closed without treatment.");
        }
    }

    private void applyClosureDetails(Referral referral, ReferralStatus nextStatus, ReferralStatusForm form) {
        if (!CLOSED_STATUSES.contains(nextStatus)) {
            return;
        }
        referral.setClosureOutcome(resolveClosureOutcome(nextStatus, form));
        referral.setClosureSummary(normalizeNullable(form.getClosureSummary()));
    }

    private String buildStatusDetails(ReferralStatus currentStatus, ReferralStatus nextStatus, ReferralStatusForm form) {
        StringBuilder details = new StringBuilder();
        if (StringUtils.hasText(form.getNote())) {
            details.append(form.getNote().trim());
        } else {
            details.append("Status changed from ")
                .append(currentStatus.getDisplayName())
                .append(" to ")
                .append(nextStatus.getDisplayName())
                .append('.');
        }

        ReferralClosureOutcome resolvedOutcome = CLOSED_STATUSES.contains(nextStatus)
            ? resolveClosureOutcome(nextStatus, form)
            : form.getClosureOutcome();
        if (resolvedOutcome != null) {
            details.append(" Final outcome: ")
                .append(resolvedOutcome.getDisplayName())
                .append('.');
        }
        if (StringUtils.hasText(form.getClosureSummary())) {
            details.append(' ').append(form.getClosureSummary().trim());
        }
        return details.toString();
    }

    private ReferralClosureOutcome resolveClosureOutcome(ReferralStatus nextStatus, ReferralStatusForm form) {
        if (!CLOSED_STATUSES.contains(nextStatus)) {
            return form.getClosureOutcome();
        }
        if (form.getClosureOutcome() != null) {
            return form.getClosureOutcome();
        }
        return nextStatus == ReferralStatus.COMPLETED
            ? ReferralClosureOutcome.TREATED
            : ReferralClosureOutcome.CLOSED_WITHOUT_TREATMENT;
    }

    private ReferralStatusForm toStatusForm(ReferralClosureForm form) {
        ReferralStatusForm statusForm = new ReferralStatusForm();
        statusForm.setClosureOutcome(form.getClosureOutcome());
        statusForm.setClosureSummary(form.getClosureSummary());
        return statusForm;
    }

    private void createEvent(Referral referral,
                             ReferralEventType eventType,
                             ReferralStatus previousStatus,
                             ReferralStatus newStatus,
                             String details) {
        CustomUserDetails user = currentUserFacade.requireUser();
        ReferralEvent event = new ReferralEvent();
        event.setReferral(referral);
        event.setEventType(eventType);
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(newStatus);
        event.setActorName(user.getFullName());
        event.setActorHospitalName(resolveActorHospitalName(referral));
        event.setActorRoleName(resolveActorRoleName(user));
        event.setDetails(details);
        referralEventRepository.save(event);
    }

    private String resolveActorHospitalName(Referral referral) {
        Long tenantId = requireTenantId();
        if (referral.getFromHospital() != null && tenantId.equals(referral.getFromHospital().getId())) {
            return referral.getFromHospital().getName();
        }
        if (referral.getToHospital() != null && tenantId.equals(referral.getToHospital().getId())) {
            return referral.getToHospital().getName();
        }
        return null;
    }

    private String resolveActorRoleName(CustomUserDetails user) {
        if (user.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            return "Super Admin";
        }
        if (user.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_HOSPITAL_ADMIN"))) {
            return "Hospital Admin";
        }
        if (user.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_REFERRAL_OFFICER"))) {
            return "Referral Officer";
        }
        if (user.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_DOCTOR"))) {
            return "Doctor";
        }
        if (user.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_VIEWER"))) {
            return "Viewer";
        }
        return null;
    }

    private ReferralPriority defaultPriority(ReferralPriority priority) {
        return priority != null ? priority : ReferralPriority.NORMAL;
    }

    private String generateJourneyCode(Hospital hospital) {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        return "JRNY-" + hospital.getCode() + "-" + stamp;
    }

    private String generateReferenceNumber(Hospital hospital) {
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        return "REF-" + hospital.getCode() + "-" + stamp;
    }

    private Patient resolveForwardedPatient(Referral sourceReferral, Long tenantId, Hospital currentHospital) {
        Patient sourcePatient = sourceReferral.getPatient();
        if (sourcePatient == null) {
            throw new IllegalArgumentException("Source referral does not have a patient record.");
        }

        if (StringUtils.hasText(sourcePatient.getNationalId())) {
            return patientRepository.findByHospitalIdAndNationalIdIgnoreCase(tenantId, sourcePatient.getNationalId())
                .orElseGet(() -> createForwardedPatientCopy(sourcePatient, currentHospital));
        }

        return patientRepository.findByHospitalIdAndFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth(
                tenantId,
                sourcePatient.getFirstName(),
                sourcePatient.getLastName(),
                sourcePatient.getDateOfBirth())
            .orElseGet(() -> createForwardedPatientCopy(sourcePatient, currentHospital));
    }

    private Patient createForwardedPatientCopy(Patient sourcePatient, Hospital currentHospital) {
        Patient patient = new Patient();
        patient.setHospital(currentHospital);
        patient.setPatientNumber(generateForwardedPatientNumber(currentHospital));
        patient.setFirstName(sourcePatient.getFirstName());
        patient.setLastName(sourcePatient.getLastName());
        patient.setGender(sourcePatient.getGender());
        patient.setDateOfBirth(sourcePatient.getDateOfBirth());
        patient.setPhoneNumber(sourcePatient.getPhoneNumber());
        patient.setNationalId(sourcePatient.getNationalId());
        return patientRepository.save(patient);
    }

    private String generateForwardedPatientNumber(Hospital hospital) {
        return "TRF-" + hospital.getCode() + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String assignmentTargetLabel(Department department, Doctor doctor) {
        if (department != null && doctor != null) {
            return department.getName() + " / " + doctor.getFullName();
        }
        if (department != null) {
            return department.getName() + " department";
        }
        return doctor.getFullName();
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context not available");
        }
        return tenantId;
    }

    private boolean matchesDirection(Referral referral, Long tenantId, String direction) {
        if (!StringUtils.hasText(direction)) {
            return true;
        }
        return switch (direction.trim().toLowerCase(Locale.ROOT)) {
            case "incoming" -> referral.isIncomingFor(tenantId);
            case "outgoing" -> referral.isOutgoingFor(tenantId);
            default -> true;
        };
    }

    private boolean matchesReferral(Referral referral, Long tenantId, String query) {
        return contains(referral.getReferenceNumber(), query)
            || contains(referral.getJourneyCode(), query)
            || contains(referral.getSubject(), query)
            || contains(referral.getReferralReason(), query)
            || contains(referral.getClinicalSummary(), query)
            || contains(referral.getReceivingDepartment(), query)
            || contains(referral.getPatient().getPatientNumber(), query)
            || contains(referral.getPatient().getFullName(), query)
            || contains(referral.getStatus().getDisplayName(), query)
            || contains(referral.getPriority().getDisplayName(), query)
            || contains(referral.isIncomingFor(tenantId) ? referral.getFromHospital().getName() : referral.getToHospital().getName(), query)
            || (referral.getAssignedDepartment() != null && contains(referral.getAssignedDepartment().getName(), query))
            || (referral.getAssignedDoctor() != null && contains(referral.getAssignedDoctor().getFullName(), query));
    }

    private boolean matchesDoctorReferral(Referral referral, String query) {
        return contains(referral.getReferenceNumber(), query)
            || contains(referral.getJourneyCode(), query)
            || contains(referral.getSubject(), query)
            || contains(referral.getPatient().getPatientNumber(), query)
            || contains(referral.getPatient().getFullName(), query)
            || contains(referral.getStatus().getDisplayName(), query)
            || contains(referral.getFromHospital().getName(), query)
            || (referral.getAssignedDepartment() != null && contains(referral.getAssignedDepartment().getName(), query));
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        return query.trim().toLowerCase(Locale.ROOT);
    }
}
