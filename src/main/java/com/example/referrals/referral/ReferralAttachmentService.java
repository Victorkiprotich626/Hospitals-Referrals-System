package com.example.referrals.referral;

import com.example.referrals.common.model.RoleName;
import com.example.referrals.common.web.CurrentUserFacade;
import com.example.referrals.hospitaladmin.service.HospitalAdminReferralService;
import com.example.referrals.notification.NotificationService;
import com.example.referrals.security.CustomUserDetails;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ReferralAttachmentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png", "doc", "docx");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "application/pdf",
        "image/jpeg",
        "image/png",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/octet-stream"
    );
    private static final Map<String, String> CONTENT_TYPES_BY_EXTENSION = Map.of(
        "pdf", "application/pdf",
        "jpg", "image/jpeg",
        "jpeg", "image/jpeg",
        "png", "image/png",
        "doc", "application/msword",
        "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final ReferralAttachmentRepository attachmentRepository;
    private final HospitalAdminReferralService referralService;
    private final CurrentUserFacade currentUserFacade;
    private final NotificationService notificationService;
    private final Path storageRoot;

    public ReferralAttachmentService(ReferralAttachmentRepository attachmentRepository,
                                     HospitalAdminReferralService referralService,
                                     CurrentUserFacade currentUserFacade,
                                     NotificationService notificationService,
                                     @Value("${app.storage.attachments-dir}") String storageDirectory) {
        this.attachmentRepository = attachmentRepository;
        this.referralService = referralService;
        this.currentUserFacade = currentUserFacade;
        this.notificationService = notificationService;
        this.storageRoot = Paths.get(storageDirectory).toAbsolutePath().normalize();
    }

    @PostConstruct
    void initializeStorage() {
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to initialize attachment storage directory.", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<ReferralAttachment> findVisibleAttachments(Long referralId) {
        resolveReferralForCurrentUser(referralId);
        return attachmentRepository.findAllByReferralIdOrderByCreatedAtDesc(referralId);
    }

    public void uploadVisibleAttachment(Long referralId,
                                        ReferralAttachmentType attachmentType,
                                        String note,
                                        MultipartFile file) {
        Referral referral = resolveReferralForCurrentUser(referralId);
        String originalFileName = sanitizeOriginalFileName(file.getOriginalFilename());
        String fileExtension = extractExtension(originalFileName);
        String contentType = validateAndResolveContentType(file, fileExtension);

        Path referralDirectory = storageRoot.resolve("referral-" + referral.getId()).normalize();
        try {
            Files.createDirectories(referralDirectory);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to prepare storage for this referral.", ex);
        }

        String storedFileName = UUID.randomUUID() + "-" + originalFileName;
        Path targetPath = referralDirectory.resolve(storedFileName).normalize();

        if (!targetPath.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Invalid attachment path.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store attachment.", ex);
        }

        CustomUserDetails currentUser = currentUserFacade.requireUser();
        ReferralAttachment attachment = new ReferralAttachment();
        attachment.setReferral(referral);
        attachment.setOriginalFileName(originalFileName);
        attachment.setStoredFileName(storageRoot.relativize(targetPath).toString().replace('\\', '/'));
        attachment.setAttachmentType(attachmentType != null ? attachmentType : ReferralAttachmentType.OTHER);
        attachment.setContentType(contentType);
        attachment.setFileSize(file.getSize());
        attachment.setNote(normalizeNote(note));
        attachment.setUploadedByName(currentUser.getFullName());
        attachment.setUploadedByRoleName(resolveRoleLabel(currentUser));
        attachmentRepository.save(attachment);

        referralService.recordEvent(referral, ReferralEventType.ATTACHMENT_ADDED,
            "Attachment uploaded: " + originalFileName + " (" + attachment.getAttachmentType().getDisplayName() + ").");
        notificationService.notifyReferralAttachmentAdded(referral, originalFileName);
    }

    public void deleteVisibleAttachment(Long referralId, Long attachmentId) {
        if (!canManageAttachments()) {
            throw new IllegalArgumentException("You do not have permission to delete attachments.");
        }

        Referral referral = resolveReferralForCurrentUser(referralId);
        ReferralAttachment attachment = attachmentRepository.findByIdWithReferral(attachmentId)
            .orElseThrow(() -> new EntityNotFoundException("Attachment not found"));
        if (attachment.getReferral() == null || !attachment.getReferral().getId().equals(referral.getId())) {
            throw new IllegalArgumentException("Attachment does not belong to this referral.");
        }

        Path filePath = storageRoot.resolve(attachment.getStoredFileName()).normalize();
        if (filePath.startsWith(storageRoot)) {
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to remove attachment file from storage.", ex);
            }
        }

        String fileName = attachment.getOriginalFileName();
        attachmentRepository.delete(attachment);
        referralService.recordEvent(referral, ReferralEventType.ATTACHMENT_REMOVED,
            "Attachment removed: " + fileName + ".");
    }

    @Transactional(readOnly = true)
    public DownloadedAttachment loadVisibleAttachment(Long attachmentId) {
        ReferralAttachment attachment = attachmentRepository.findByIdWithReferral(attachmentId)
            .orElseThrow(() -> new EntityNotFoundException("Attachment not found"));

        Referral referral = attachment.getReferral();
        if (referral == null) {
            throw new EntityNotFoundException("Attachment is not linked to a referral.");
        }

        resolveReferralForCurrentUser(referral.getId());

        Path filePath = storageRoot.resolve(attachment.getStoredFileName()).normalize();
        if (!filePath.startsWith(storageRoot) || !Files.exists(filePath)) {
            throw new EntityNotFoundException("Attachment file not found.");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
        return new DownloadedAttachment(attachment, resource);
    } catch (MalformedURLException ex) {
        throw new IllegalStateException("Invalid attachment resource path.", ex);
    }
}

    public boolean canManageAttachments() {
        return currentUserFacade.hasRole(RoleName.HOSPITAL_ADMIN)
            || currentUserFacade.hasRole(RoleName.REFERRAL_OFFICER);
    }

    private Referral resolveReferralForCurrentUser(Long referralId) {
        if (currentUserFacade.hasRole(RoleName.DOCTOR)) {
            return referralService.findAssignedReferralForCurrentDoctor(referralId);
        }
        if (currentUserFacade.hasRole(RoleName.HOSPITAL_ADMIN)
            || currentUserFacade.hasRole(RoleName.REFERRAL_OFFICER)) {
            return referralService.findVisibleById(referralId);
        }
        throw new IllegalArgumentException("You do not have access to referral attachments.");
    }

    private String validateAndResolveContentType(MultipartFile file, String fileExtension) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a file to upload.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Attachment exceeds the 10 MB size limit.");
        }
        if (!StringUtils.hasText(file.getOriginalFilename())) {
            throw new IllegalArgumentException("Attachment file name is required.");
        }
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            throw new IllegalArgumentException("Unsupported file type. Allowed types are PDF, JPG, PNG, DOC, and DOCX.");
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type. Allowed types are PDF, JPG, PNG, DOC, and DOCX.");
        }
        return CONTENT_TYPES_BY_EXTENSION.get(fileExtension);
    }

    private String sanitizeOriginalFileName(String originalFileName) {
        String sanitized = StringUtils.cleanPath(originalFileName == null ? "attachment" : originalFileName);
        sanitized = sanitized.replace("..", "");
        String fileName = Paths.get(sanitized).getFileName().toString().trim();
        return StringUtils.hasText(fileName) ? fileName : "attachment";
    }

    private String extractExtension(String fileName) {
        int separator = fileName.lastIndexOf('.');
        if (separator < 0 || separator == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType.trim().toLowerCase() : "application/octet-stream";
    }

    private String normalizeNote(String note) {
        return StringUtils.hasText(note) ? note.trim() : null;
    }

    private String resolveRoleLabel(CustomUserDetails user) {
        if (currentUserFacade.hasRole(RoleName.HOSPITAL_ADMIN)) {
            return "Hospital Admin";
        }
        if (currentUserFacade.hasRole(RoleName.REFERRAL_OFFICER)) {
            return "Referral Officer";
        }
        if (currentUserFacade.hasRole(RoleName.DOCTOR)) {
            return "Doctor";
        }
        if (currentUserFacade.hasRole(RoleName.SUPER_ADMIN)) {
            return "Super Admin";
        }
        if (currentUserFacade.hasRole(RoleName.VIEWER)) {
            return "Viewer";
        }
        return user.getAuthorities().stream().findFirst().map(Object::toString).orElse(null);
    }

    public record DownloadedAttachment(ReferralAttachment attachment, Resource resource) {
    }
}
