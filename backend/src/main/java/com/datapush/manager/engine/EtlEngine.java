package com.datapush.manager.engine;

import com.datapush.manager.entity.ConnectorInfo;
import com.datapush.manager.entity.FieldMapping;
import com.datapush.manager.entity.TaskConfig;

import java.util.List;
import java.util.Map;

/**
 * ETL执行引擎接口
 *
 * @author Data Push Team
 * @since 2024-11-20
 */
public interface EtlEngine {

    /**
     * 执行ETL任务
     *
     * @param taskConfig 任务配置
     * @param sourceConnector 源连接器
     * @param targetConnector 目标连接器
     * @param fieldMappings 字段映射
     * @param logId 日志ID
     * @return 执行结果
     */
    EtlResult execute(TaskConfig taskConfig, 
                      ConnectorInfo sourceConnector, 
                      ConnectorInfo targetConnector,
                      List<FieldMapping> fieldMappings,
                      Long logId);

    /**
     * ETL执行结果
     */
    class EtlResult {
        private boolean success;
        private Integer totalCount;
        private Integer successCount;
        private Integer failedCount;
        private String message;
        private String executeLog;

        public EtlResult() {
        }

        public EtlResult(boolean success, Integer totalCount, Integer successCount, 
                        Integer failedCount, String message, String executeLog) {
            this.success = success;
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.message = message;
            this.executeLog = executeLog;
        }

        public static EtlResult success(Integer totalCount, Integer successCount, String executeLog) {
            return new EtlResult(true, totalCount, successCount, 0, "执行成功", executeLog);
        }

        public static EtlResult failed(String message, String executeLog) {
            return new EtlResult(false, 0, 0, 0, message, executeLog);
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public Integer getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(Integer totalCount) {
            this.totalCount = totalCount;
        }

        public Integer getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(Integer successCount) {
            this.successCount = successCount;
        }

        public Integer getFailedCount() {
            return failedCount;
        }

        public void setFailedCount(Integer failedCount) {
            this.failedCount = failedCount;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getExecuteLog() {
            return executeLog;
        }

        public void setExecuteLog(String executeLog) {
            this.executeLog = executeLog;
        }
    }
}
