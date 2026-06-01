package com.chat.myAgent.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class CreateSessionRequest {

    @JsonAlias({"sessionTitle", "name"})
    private String title;
}
