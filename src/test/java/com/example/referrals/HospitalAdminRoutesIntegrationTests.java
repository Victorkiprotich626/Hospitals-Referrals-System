package com.example.referrals;

import com.example.referrals.directory.Department;
import com.example.referrals.directory.DepartmentRepository;
import com.example.referrals.directory.Doctor;
import com.example.referrals.directory.DoctorRepository;
import com.example.referrals.hospital.Hospital;
import com.example.referrals.hospital.HospitalRepository;
import com.example.referrals.patient.Gender;
import com.example.referrals.patient.Patient;
import com.example.referrals.patient.PatientRepository;
import com.example.referrals.referral.Referral;
import com.example.referrals.referral.ReferralAttachment;
import com.example.referrals.referral.ReferralAttachmentRepository;
import com.example.referrals.referral.ReferralAttachmentType;
import com.example.referrals.referral.ReferralClosureOutcome;
import com.example.referrals.referral.ReferralEvent;
import com.example.referrals.referral.ReferralEventRepository;
import com.example.referrals.referral.ReferralEventType;
import com.example.referrals.referral.ReferralPriority;
import com.example.referrals.referral.ReferralRepository;
import com.example.referrals.referral.ReferralStatus;
import com.example.referrals.security.CustomUserDetails;
import com.example.referrals.user.AppUser;
import com.example.referrals.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HospitalAdminRoutesIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ReferralRepository referralRepository;

    @Autowired
    private ReferralAttachmentRepository attachmentRepository;

    @Autowired
    private ReferralEventRepository referralEventRepository;

    @Autowired
    private UserRepository userRepository;

    private Hospital hospital;
    private Hospital receivingHospital;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        doctorRepository.deleteAll();
        departmentRepository.deleteAll();
        attachmentRepository.deleteAll();
        referralEventRepository.deleteAll();
        referralRepository.deleteAll();
        patientRepository.deleteAll();
        hospitalRepository.deleteAll();

        hospital = new Hospital();
        hospital.setName("Alpha Hospital");
        hospital.setCode("ALPHA");
        hospital.setEnabled(true);
        hospital = hospitalRepository.save(hospital);

        receivingHospital = new Hospital();
        receivingHospital.setName("Beta Hospital");
        receivingHospital.setCode("BETA");
        receivingHospital.setEnabled(true);
        receivingHospital = hospitalRepository.save(receivingHospital);
    }

    @Test
    void departmentsListAndFormRenderForHospitalAdmin() throws Exception {
        mockMvc.perform(get("/hospital-admin/departments").with(hospitalAdmin()))
            .andExpect(status().isOk())
            .andExpect(view().name("hospitaladmin/departments/list"));

        mockMvc.perform(get("/hospital-admin/departments/new").with(hospitalAdmin()))
            .andExpect(status().isOk())
            .andExpect(view().name("hospitaladmin/departments/form"));
    }

    @Test
    void mainWorkspaceDashboardsRender() throws Exception {
        mockMvc.perform(get("/super-admin").with(superAdmin()))
            .andExpect(status().isOk())
            .andExpect(view().name("superadmin/dashboard"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Tenancy model"))));

        mockMvc.perform(get("/hospital-admin").with(hospitalAdmin()))
            .andExpect(status().isOk())
            .andExpect(view().name("hospitaladmin/dashboard"));

        mockMvc.perform(get("/referral-officer").with(referralOfficer()))
            .andExpect(status().isOk())
            .andExpect(view().name("referralofficer/dashboard"));

        mockMvc.perform(get("/viewer").with(viewer()))
            .andExpect(status().isOk())
            .andExpect(view().name("viewer/dashboard"));
    }

    @Test
    void departmentCanBeCreatedAndRedirectsBackToList() throws Exception {
        mockMvc.perform(post("/hospital-admin/departments")
                .with(hospitalAdmin())
                .with(csrf())
                .param("name", "Cardiology")
                .param("code", "CARD")
                .param("enabled", "true"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/hospital-admin/departments"));
    }

    @Test
    void doctorsListAndFormRenderForHospitalAdmin() throws Exception {
        Department department = new Department();
        department.setHospital(hospital);
        department.setName("Cardiology");
        department.setCode("CARD");
        department.setEnabled(true);
        department = departmentRepository.save(department);

        Doctor doctor = new Doctor();
        doctor.setHospital(hospital);
        doctor.setDepartment(department);
        doctor.setFirstName("Amina");
        doctor.setLastName("Otieno");
        doctor.setEmail("amina@alpha.test");
        doctor.setEnabled(true);
        doctorRepository.save(doctor);

        mockMvc.perform(get("/hospital-admin/doctors").with(hospitalAdmin()))
            .andExpect(status().isOk())
            .andExpect(view().name("hospitaladmin/doctors/list"));

        mockMvc.perform(get("/hospital-admin/doctors/new").with(hospitalAdmin()))
            .andExpect(status().isOk())
            .andExpect(view().name("hospitaladmin/doctors/form"));
    }

    @Test
    void doctorCanBeCreatedAndRedirectsBackToList() throws Exception {
        Department department = new Department();
        department.setHospital(hospital);
        department.setName("Cardiology");
        department.setCode("CARD");
        department.setEnabled(true);
        department = departmentRepository.save(department);

        mockMvc.perform(post("/hospital-admin/doctors")
                .with(hospitalAdmin())
                .with(csrf())
                .param("firstName", "Amina")
                .param("lastName", "Otieno")
                .param("email", "amina@alpha.test")
                .param("departmentId", department.getId().toString())
                .param("phoneNumber", "+254700000001")
                .param("specialty", "Cardiology")
                .param("enabled", "true"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/hospital-admin/doctors"));
    }

    @Test
    void referralListRendersForHospitalAdminCancelTarget() throws Exception {
        Patient patient = new Patient();
        patient.setHospital(hospital);
        patient.setPatientNumber("PAT-001");
        patient.setFirstName("Jane");
        patient.setLastName("Doe");
        patient.setGender(Gender.FEMALE);
        patient.setDateOfBirth(java.time.LocalDate.of(1990, 1, 1));
        patient = patientRepository.save(patient);

        Referral referral = new Referral();
        referral.setReferenceNumber("REF-ALPHA-001");
        referral.setJourneyCode("JRNY-ALPHA-001");
        referral.setPatient(patient);
        referral.setFromHospital(hospital);
        referral.setToHospital(receivingHospital);
        referral.setStatus(ReferralStatus.SUBMITTED);
        referral.setPriority(ReferralPriority.NORMAL);
        referral.setSubject("Cardiology review");
        referral.setReferralReason("Needs specialist review");
        referralRepository.save(referral);

        mockMvc.perform(get("/hospital-admin/referrals").with(hospitalAdmin()))
            .andExpect(status().isOk())
            .andExpect(view().name("hospitaladmin/referrals/list"));
    }

    @Test
    void referralOfficerCanOpenCreateReferralForm() throws Exception {
        Patient patient = new Patient();
        patient.setHospital(hospital);
        patient.setPatientNumber("PAT-002");
        patient.setFirstName("John");
        patient.setLastName("Smith");
        patient.setGender(Gender.MALE);
        patient.setDateOfBirth(java.time.LocalDate.of(1988, 5, 12));
        patientRepository.save(patient);

        mockMvc.perform(get("/referral-officer/referrals/new").with(referralOfficer()))
            .andExpect(status().isOk())
            .andExpect(view().name("referralofficer/referrals/form"));
    }

    @Test
    void referralOfficerCanCreateReferral() throws Exception {
        Patient patient = new Patient();
        patient.setHospital(hospital);
        patient.setPatientNumber("PAT-003");
        patient.setFirstName("Alice");
        patient.setLastName("Brown");
        patient.setGender(Gender.FEMALE);
        patient.setDateOfBirth(java.time.LocalDate.of(1992, 7, 21));
        patient = patientRepository.save(patient);

        mockMvc.perform(post("/referral-officer/referrals")
                .with(referralOfficer())
                .with(csrf())
                .param("patientId", patient.getId().toString())
                .param("toHospitalId", receivingHospital.getId().toString())
                .param("priority", "NORMAL")
                .param("subject", "Neurology review")
                .param("referralReason", "Needs specialist opinion")
                .param("clinicalSummary", "Recurring headaches")
                .param("receivingDepartment", "Neurology"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    void usersListAndFormRenderForHospitalAdmin() throws Exception {
        mockMvc.perform(get("/hospital-admin/users").with(hospitalAdmin()))
            .andExpect(status().isOk())
            .andExpect(view().name("hospitaladmin/users/list"));

        mockMvc.perform(get("/hospital-admin/users/new").with(hospitalAdmin()))
            .andExpect(status().isOk())
            .andExpect(view().name("hospitaladmin/users/form"));
    }

    @Test
    void referralOfficerCanBeCreatedWithoutDepartmentOrDoctorProfile() throws Exception {
        mockMvc.perform(post("/hospital-admin/users")
                .with(hospitalAdmin())
                .with(csrf())
                .param("firstName", "Grace")
                .param("lastName", "Njeri")
                .param("email", "grace@alpha.test")
                .param("role", "REFERRAL_OFFICER")
                .param("password", "Password123!")
                .param("enabled", "true"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/hospital-admin/users"));
    }

    @Test
    void patientSearchFiltersList() throws Exception {
        Patient patientOne = new Patient();
        patientOne.setHospital(hospital);
        patientOne.setPatientNumber("PAT-ALPHA");
        patientOne.setFirstName("Alice");
        patientOne.setLastName("Kamau");
        patientOne.setGender(Gender.FEMALE);
        patientOne.setDateOfBirth(java.time.LocalDate.of(1994, 3, 10));
        patientRepository.save(patientOne);

        Patient patientTwo = new Patient();
        patientTwo.setHospital(hospital);
        patientTwo.setPatientNumber("PAT-BETA");
        patientTwo.setFirstName("Brian");
        patientTwo.setLastName("Otieno");
        patientTwo.setGender(Gender.MALE);
        patientTwo.setDateOfBirth(java.time.LocalDate.of(1991, 8, 20));
        patientRepository.save(patientTwo);

        mockMvc.perform(get("/hospital-admin/patients").with(hospitalAdmin()).param("q", "Alice"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Alice Kamau")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Brian Otieno"))));
    }

    @Test
    void patientCanBeCreatedWithoutManualPatientNumber() throws Exception {
        mockMvc.perform(post("/hospital-admin/patients")
                .with(hospitalAdmin())
                .with(csrf())
                .param("firstName", "Janet")
                .param("lastName", "Wanjiku")
                .param("gender", "FEMALE")
                .param("dateOfBirth", "1994-03-10")
                .param("phoneNumber", "+254700123456")
                .param("nationalId", "12345678"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/hospital-admin/patients"));

        Patient savedPatient = patientRepository.findAllByHospitalIdOrderByLastNameAscFirstNameAsc(hospital.getId()).stream()
            .filter(patient -> "Janet".equals(patient.getFirstName()) && "Wanjiku".equals(patient.getLastName()))
            .findFirst()
            .orElse(null);

        assertNotNull(savedPatient);
        assertNotNull(savedPatient.getPatientNumber());
        assertTrue(savedPatient.getPatientNumber().startsWith("PAT-ALPHA-"));
        assertFalse(savedPatient.getPatientNumber().isBlank());
    }

    @Test
    void referralSearchFiltersList() throws Exception {
        Patient patient = new Patient();
        patient.setHospital(hospital);
        patient.setPatientNumber("PAT-SEARCH");
        patient.setFirstName("Carol");
        patient.setLastName("Njeri");
        patient.setGender(Gender.FEMALE);
        patient.setDateOfBirth(java.time.LocalDate.of(1993, 11, 5));
        patient = patientRepository.save(patient);

        Referral match = new Referral();
        match.setReferenceNumber("REF-CARD-001");
        match.setJourneyCode("JRNY-CARD-001");
        match.setPatient(patient);
        match.setFromHospital(hospital);
        match.setToHospital(receivingHospital);
        match.setStatus(ReferralStatus.SUBMITTED);
        match.setPriority(ReferralPriority.NORMAL);
        match.setSubject("Cardiology review");
        match.setReferralReason("Heart assessment");
        referralRepository.save(match);

        Referral other = new Referral();
        other.setReferenceNumber("REF-ORTHO-001");
        other.setJourneyCode("JRNY-ORTHO-001");
        other.setPatient(patient);
        other.setFromHospital(hospital);
        other.setToHospital(receivingHospital);
        other.setStatus(ReferralStatus.RECEIVED);
        other.setPriority(ReferralPriority.HIGH);
        other.setSubject("Orthopedic consult");
        other.setReferralReason("Knee pain");
        referralRepository.save(other);

        mockMvc.perform(get("/hospital-admin/referrals").with(hospitalAdmin()).param("q", "Cardiology"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("REF-CARD-001")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("REF-ORTHO-001"))));
    }

    @Test
    void hospitalAdminReferralDetailSupportsJourneyTimelineFiltering() throws Exception {
        Referral referral = createReferral("REF-TIME-001", "JRNY-TIME-001", ReferralStatus.RECEIVED);

        ReferralEvent created = new ReferralEvent();
        created.setReferral(referral);
        created.setEventType(ReferralEventType.CREATED);
        created.setActorName("Grace Njeri");
        created.setActorHospitalName(hospital.getName());
        created.setActorRoleName("Referral Officer");
        created.setDetails("Referral submitted to Beta Hospital.");
        referralEventRepository.save(created);

        ReferralEvent statusChanged = new ReferralEvent();
        statusChanged.setReferral(referral);
        statusChanged.setEventType(ReferralEventType.STATUS_CHANGED);
        statusChanged.setPreviousStatus(ReferralStatus.SUBMITTED);
        statusChanged.setNewStatus(ReferralStatus.RECEIVED);
        statusChanged.setActorName("Brian Otieno");
        statusChanged.setActorHospitalName(receivingHospital.getName());
        statusChanged.setActorRoleName("Hospital Admin");
        statusChanged.setDetails("Referral acknowledged by receiving hospital.");
        referralEventRepository.save(statusChanged);

        mockMvc.perform(get("/hospital-admin/referrals/" + referral.getId())
                .with(hospitalAdmin())
                .param("timelineScope", "journey")
                .param("eventType", "STATUS_CHANGED"))
            .andExpect(status().isOk())
            .andExpect(view().name("hospitaladmin/referrals/detail"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Submitted -&gt; Received")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Referral acknowledged by receiving hospital.")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Referral submitted to Beta Hospital."))));
    }

    @Test
    void invalidTimelineEventTypeFallsBackGracefully() throws Exception {
        Referral referral = createReferral("REF-TIME-002", "JRNY-TIME-002", ReferralStatus.SUBMITTED);

        mockMvc.perform(get("/hospital-admin/referrals/" + referral.getId())
                .with(hospitalAdmin())
                .param("eventType", "NOT_A_REAL_TYPE"))
            .andExpect(status().isOk())
            .andExpect(view().name("hospitaladmin/referrals/detail"));
    }

    @Test
    void hospitalAdminReferralDetailShowsAttachmentMetadata() throws Exception {
        Referral referral = createReferral("REF-ATT-001", "JRNY-ATT-001", ReferralStatus.SUBMITTED);

        ReferralAttachment attachment = new ReferralAttachment();
        attachment.setReferral(referral);
        attachment.setOriginalFileName("blood-work.pdf");
        attachment.setStoredFileName("referral-1/blood-work.pdf");
        attachment.setAttachmentType(ReferralAttachmentType.LAB_RESULT);
        attachment.setContentType("application/pdf");
        attachment.setFileSize(2048);
        attachment.setNote("Baseline blood chemistry panel");
        attachment.setUploadedByName("Grace Njeri");
        attachment.setUploadedByRoleName("Referral Officer");
        attachmentRepository.save(attachment);

        mockMvc.perform(get("/hospital-admin/referrals/" + referral.getId()).with(hospitalAdmin()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("blood-work.pdf")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Lab Result")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Baseline blood chemistry panel")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Referral Officer")));
    }

    @Test
    void hospitalAdminReferralDetailShowsClosureOutcome() throws Exception {
        Referral referral = createIncomingReferral("REF-CLOSE-001", "JRNY-CLOSE-001", ReferralStatus.COMPLETED);
        referral.setClosureOutcome(ReferralClosureOutcome.ADMITTED);
        referral.setClosureSummary("Patient admitted for observation and continued treatment.");
        referralRepository.save(referral);

        mockMvc.perform(get("/hospital-admin/referrals/" + referral.getId()).with(hospitalAdmin()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Admitted")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Patient admitted for observation and continued treatment.")));
    }

    @Test
    void closingReferralRequiresFinalOutcome() throws Exception {
        Referral referral = createIncomingReferral("REF-CLOSE-002", "JRNY-CLOSE-002", ReferralStatus.ACCEPTED);

        mockMvc.perform(post("/hospital-admin/referrals/" + referral.getId() + "/status")
                .with(hospitalAdmin())
                .with(csrf())
                .param("status", "COMPLETED")
                .param("note", "Clinical work finished"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/hospital-admin/referrals/" + referral.getId()));

        Referral reloaded = referralRepository.findById(referral.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ReferralStatus.COMPLETED, reloaded.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(ReferralClosureOutcome.TREATED, reloaded.getClosureOutcome());
    }

    @Test
    void closingReferralPersistsAllowedFinalOutcome() throws Exception {
        Referral referral = createIncomingReferral("REF-CLOSE-003", "JRNY-CLOSE-003", ReferralStatus.ACCEPTED);

        mockMvc.perform(post("/hospital-admin/referrals/" + referral.getId() + "/status")
                .with(hospitalAdmin())
                .with(csrf())
                .param("status", "COMPLETED")
                .param("closureOutcome", "ADMITTED")
                .param("closureSummary", "Admitted under internal medicine for further care.")
                .param("note", "Accepted case has now been admitted"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/hospital-admin/referrals/" + referral.getId()));

        Referral reloaded = referralRepository.findById(referral.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ReferralStatus.COMPLETED, reloaded.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(ReferralClosureOutcome.ADMITTED, reloaded.getClosureOutcome());
    }

    @Test
    void closedReferralOutcomeCanBeUpdatedAndAppearsInTimeline() throws Exception {
        Referral referral = createIncomingReferral("REF-CLOSE-004", "JRNY-CLOSE-004", ReferralStatus.COMPLETED);
        referral.setClosureOutcome(ReferralClosureOutcome.TREATED);
        referral.setClosureSummary("Patient initially discharged after treatment.");
        referral = referralRepository.save(referral);

        mockMvc.perform(post("/hospital-admin/referrals/" + referral.getId() + "/closure")
                .with(hospitalAdmin())
                .with(csrf())
                .param("closureOutcome", "ADMITTED")
                .param("closureSummary", "Patient admitted for continued inpatient care."))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/hospital-admin/referrals/" + referral.getId()));

        Referral reloaded = referralRepository.findById(referral.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ReferralClosureOutcome.ADMITTED, reloaded.getClosureOutcome());

        mockMvc.perform(get("/hospital-admin/referrals/" + referral.getId()).with(hospitalAdmin()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Outcome Updated")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Patient admitted for continued inpatient care.")));
    }

    @Test
    void outgoingReferralQueueShowsAwaitingResponseInsteadOfOverdue() throws Exception {
        Patient patient = new Patient();
        patient.setHospital(hospital);
        patient.setPatientNumber("PAT-AWAITING-001");
        patient.setFirstName("Awaiting");
        patient.setLastName("Response");
        patient.setGender(Gender.FEMALE);
        patient.setDateOfBirth(java.time.LocalDate.of(1990, 1, 1));
        patient = patientRepository.save(patient);

        Referral referral = new Referral();
        referral.setReferenceNumber("REF-AWAITING-001");
        referral.setJourneyCode("JRNY-AWAITING-001");
        referral.setPatient(patient);
        referral.setFromHospital(hospital);
        referral.setToHospital(receivingHospital);
        referral.setStatus(ReferralStatus.SUBMITTED);
        referral.setPriority(ReferralPriority.URGENT);
        referral.setSubject("Awaiting response test");
        referral.setReferralReason("Response tracking");
        referralRepository.save(referral);

        mockMvc.perform(get("/hospital-admin/referrals").with(hospitalAdmin()))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Awaiting response")));
    }

    @Test
    void hospitalAdminCanDeleteAttachment() throws Exception {
        Referral referral = createReferral("REF-ATT-002", "JRNY-ATT-002", ReferralStatus.SUBMITTED);

        ReferralAttachment attachment = new ReferralAttachment();
        attachment.setReferral(referral);
        attachment.setOriginalFileName("scan.pdf");
        attachment.setStoredFileName("referral-" + referral.getId() + "/scan.pdf");
        attachment.setAttachmentType(ReferralAttachmentType.IMAGING);
        attachment.setContentType("application/pdf");
        attachment.setFileSize(1024);
        attachment.setUploadedByName("Grace Njeri");
        attachment.setUploadedByRoleName("Referral Officer");
        attachmentRepository.save(attachment);

        mockMvc.perform(post("/hospital-admin/referrals/" + referral.getId() + "/attachments/" + attachment.getId() + "/delete")
                .with(hospitalAdmin())
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/hospital-admin/referrals/" + referral.getId()));
    }

    @Test
    void notificationsPageShowsOnlyHospitalAdminNavigation() throws Exception {
        mockMvc.perform(get("/notifications").with(hospitalAdmin()))
            .andExpect(status().isOk())
            .andExpect(view().name("notifications/list"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Hospital admin workspace")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Multi-tenant platform"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Referral officer workspace"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Doctor workspace"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Viewer workspace"))));
    }

    @Test
    void userSearchFiltersList() throws Exception {
        AppUser referralOfficer = new AppUser();
        referralOfficer.setHospital(hospital);
        referralOfficer.setFirstName("Grace");
        referralOfficer.setLastName("Njeri");
        referralOfficer.setEmail("grace@alpha.test");
        referralOfficer.setPasswordHash("hash");
        referralOfficer.setEnabled(true);
        referralOfficer.setRoles(java.util.Set.of(com.example.referrals.common.model.RoleName.REFERRAL_OFFICER));
        userRepository.save(referralOfficer);

        AppUser viewer = new AppUser();
        viewer.setHospital(hospital);
        viewer.setFirstName("Victor");
        viewer.setLastName("Mwangi");
        viewer.setEmail("victor@alpha.test");
        viewer.setPasswordHash("hash");
        viewer.setEnabled(true);
        viewer.setRoles(java.util.Set.of(com.example.referrals.common.model.RoleName.VIEWER));
        userRepository.save(viewer);

        mockMvc.perform(get("/hospital-admin/users").with(hospitalAdmin()).param("q", "Grace"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("grace@alpha.test")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("victor@alpha.test"))));
    }

    private RequestPostProcessor hospitalAdmin() {
        CustomUserDetails principal = new CustomUserDetails(
            100L,
            hospital.getId(),
            null,
            null,
            "Hospital Admin",
            "admin@alpha.test",
            "password",
            true,
            true,
            AuthorityUtils.createAuthorityList("ROLE_HOSPITAL_ADMIN")
        );
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());
        return authentication(authentication);
    }

    private RequestPostProcessor superAdmin() {
        CustomUserDetails principal = new CustomUserDetails(
            99L,
            null,
            null,
            null,
            "Super Admin",
            "superadmin@referrals.local",
            "password",
            true,
            true,
            AuthorityUtils.createAuthorityList("ROLE_SUPER_ADMIN")
        );
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());
        return authentication(authentication);
    }

    private RequestPostProcessor referralOfficer() {
        CustomUserDetails principal = new CustomUserDetails(
            101L,
            hospital.getId(),
            null,
            null,
            "Referral Officer",
            "officer@alpha.test",
            "password",
            true,
            true,
            AuthorityUtils.createAuthorityList("ROLE_REFERRAL_OFFICER")
        );
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());
        return authentication(authentication);
    }

    private RequestPostProcessor viewer() {
        CustomUserDetails principal = new CustomUserDetails(
            102L,
            hospital.getId(),
            null,
            null,
            "Viewer User",
            "viewer@alpha.test",
            "password",
            true,
            true,
            AuthorityUtils.createAuthorityList("ROLE_VIEWER")
        );
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());
        return authentication(authentication);
    }

    private Referral createReferral(String referenceNumber, String journeyCode, ReferralStatus status) {
        Patient patient = new Patient();
        patient.setHospital(hospital);
        patient.setPatientNumber("PAT-" + referenceNumber);
        patient.setFirstName("Timeline");
        patient.setLastName("Patient");
        patient.setGender(Gender.FEMALE);
        patient.setDateOfBirth(java.time.LocalDate.of(1995, 6, 14));
        patient = patientRepository.save(patient);

        Referral referral = new Referral();
        referral.setReferenceNumber(referenceNumber);
        referral.setJourneyCode(journeyCode);
        referral.setPatient(patient);
        referral.setFromHospital(hospital);
        referral.setToHospital(receivingHospital);
        referral.setStatus(status);
        referral.setPriority(ReferralPriority.NORMAL);
        referral.setSubject("Timeline referral");
        referral.setReferralReason("Timeline testing");
        return referralRepository.save(referral);
    }

    private Referral createIncomingReferral(String referenceNumber, String journeyCode, ReferralStatus status) {
        Patient patient = new Patient();
        patient.setHospital(hospital);
        patient.setPatientNumber("PAT-" + referenceNumber);
        patient.setFirstName("Incoming");
        patient.setLastName("Patient");
        patient.setGender(Gender.FEMALE);
        patient.setDateOfBirth(java.time.LocalDate.of(1992, 4, 18));
        patient = patientRepository.save(patient);

        Referral referral = new Referral();
        referral.setReferenceNumber(referenceNumber);
        referral.setJourneyCode(journeyCode);
        referral.setPatient(patient);
        referral.setFromHospital(receivingHospital);
        referral.setToHospital(hospital);
        referral.setStatus(status);
        referral.setPriority(ReferralPriority.HIGH);
        referral.setSubject("Incoming referral");
        referral.setReferralReason("Incoming referral testing");
        return referralRepository.save(referral);
    }
}
