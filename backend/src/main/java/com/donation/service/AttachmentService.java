package com.donation.service;

import com.donation.dto.response.TransactionResponse;
import com.donation.entity.TransactionAttachment;
import com.donation.entity.Transaction;
import com.donation.entity.User;
import com.donation.exception.ApiException;
import com.donation.repository.TransactionAttachmentRepository;
import com.donation.repository.TransactionRepository;
import com.donation.repository.UserRepository;
import com.donation.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "application/pdf", "image/jpeg", "image/jpg", "image/png"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    private final TransactionAttachmentRepository attachmentRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Value("${app.storage.path:./uploads}")
    private String storagePath;

    @Transactional
    public TransactionAttachment uploadAttachment(Long transactionId, MultipartFile file,
                                                  CustomUserPrincipal principal) throws IOException {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> ApiException.notFound("Transaction not found"));

        // Authorization: creator or Super Admin
        if (!transaction.getCreatedBy().getId().equals(principal.getUserId())
            && !principal.isSuperAdmin()) {
            throw ApiException.forbidden("No access to upload for this transaction");
        }

        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String storageKey = UUID.randomUUID() + "." + extension;

        Path uploadPath = Paths.get(storagePath).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        Path targetPath = uploadPath.resolve(storageKey).normalize();
        // Prevent path traversal
        if (!targetPath.startsWith(uploadPath)) {
            throw ApiException.badRequest("Invalid file path");
        }

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        User uploader = userRepository.findById(principal.getUserId()).orElseThrow();
        TransactionAttachment attachment = TransactionAttachment.builder()
            .transaction(transaction)
            .originalFileName(originalFilename)
            .storageKey(storageKey)
            .contentType(file.getContentType())
            .fileSize(file.getSize())
            .uploadedBy(uploader)
            .build();

        return attachmentRepository.save(attachment);
    }

    public Resource loadAttachment(Long attachmentId, CustomUserPrincipal principal) {
        TransactionAttachment attachment = attachmentRepository.findById(attachmentId)
            .orElseThrow(() -> ApiException.notFound("Attachment not found"));

        // Authorization check
        Transaction transaction = attachment.getTransaction();
        if (!principal.isSuperAdmin()
            && !transaction.getCreatedBy().getId().equals(principal.getUserId())) {
            throw ApiException.forbidden("No access to this attachment");
        }

        try {
            Path filePath = Paths.get(storagePath).resolve(attachment.getStorageKey()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw ApiException.notFound("File not found");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw ApiException.badRequest("Cannot serve file");
        }
    }

    public List<Map<String, Object>> getTransactionAttachments(Long transactionId, CustomUserPrincipal principal) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> ApiException.notFound("Transaction not found"));

        if (!principal.isSuperAdmin()
            && !transaction.getCreatedBy().getId().equals(principal.getUserId())) {
            throw ApiException.forbidden("No access to this transaction");
        }

        return attachmentRepository.findByTransactionId(transactionId).stream().map(a -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", a.getId());
            map.put("originalFileName", a.getOriginalFileName());
            map.put("contentType", a.getContentType());
            map.put("fileSize", a.getFileSize());
            map.put("uploadedAt", a.getUploadedAt());
            return map;
        }).toList();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) throw ApiException.badRequest("File is empty");
        if (file.getSize() > MAX_FILE_SIZE) throw ApiException.badRequest("File size exceeds 5 MB limit");

        String extension = getExtension(file.getOriginalFilename()).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw ApiException.badRequest("File type not allowed. Allowed: PDF, JPG, JPEG, PNG");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw ApiException.badRequest("Invalid file content type");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
