package com.chat.myAgent.model.platform;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型选择结果
 *
 * 用于描述一次请求最终选择了哪个模型，以及是否走了兜底。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelSelection {

    /**
     * 主模型名
     */
    private String primaryModel;

    /**
     * 兜底模型名
     */
    private String fallbackModel;

    /**
     * 最终使用的模型名
     */
    private String selectedModel;

    /**
     * 是否触发了兜底
     */
    private boolean fallbackUsed;

    /**
     * 是否启用思考模式
     */
    private boolean thinkingMode;
}
