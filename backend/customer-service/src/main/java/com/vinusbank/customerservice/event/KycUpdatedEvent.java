package com.vinusbank.customerservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KycUpdatedEvent {
    private String userEmail;
    private String status;
}
