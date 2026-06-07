package com.chat.myAgent.learn.react;

import com.chat.myAgent.react.core.ReActRunRequest;
import com.chat.myAgent.react.core.ReActTool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LearnReActToolRegistry {

    @Getter
    private final SearchNotesReActTool searchNotesTool;

    @Getter
    private final LearningProgressReActTool learningProgressTool;

    @Getter
    private final QuizReActTool quizTool;

    @Getter
    private final SearchWebReActTool searchWebTool;

    public List<ReActTool> selectTools(ReActRunRequest request) {
        return List.of(searchNotesTool, learningProgressTool, quizTool, searchWebTool);
    }
}
