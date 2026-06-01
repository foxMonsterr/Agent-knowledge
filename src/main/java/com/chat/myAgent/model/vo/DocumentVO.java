package com.chat.myAgent.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 文档信息展示对象
 */
@Data
@Builder
public class DocumentVO {

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private long fileSize;

    /**
     * 文件大小（可读格式）
     */
    private String fileSizeReadable;

    /**
     * 切片数量
     */
    private int chunkCount;

    /**
     * 入库时间
     */
    private Date indexTime;
}
