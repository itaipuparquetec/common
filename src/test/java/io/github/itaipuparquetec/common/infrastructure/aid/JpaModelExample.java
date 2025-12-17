package io.github.itaipuparquetec.common.infrastructure.aid;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JpaModelExample {

    @Id
    protected UUID id;

    @Size(min = 1, max = 32, message = "Name must be between 1 and 32 characters")
    private String name;

    @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters")
    private String description;

}
