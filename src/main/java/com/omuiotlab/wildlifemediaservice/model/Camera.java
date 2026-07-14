package com.omuiotlab.wildlifemediaservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cameras")
public class Camera {

    @Id
    private String id;

    @Indexed(unique = true)
    private String cameraCode;

    private String name;
    private String location;
    private String jitsiRoomName;

    @Builder.Default
    private CameraStatus status = CameraStatus.OFFLINE;

    private String customerId;
    private String description;

    @Builder.Default
    private boolean active = true;

    private Instant lastSeenAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
