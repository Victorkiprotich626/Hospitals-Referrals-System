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
