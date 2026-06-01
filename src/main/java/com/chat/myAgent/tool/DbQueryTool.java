package com.chat.myAgent.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 数据库查询工具（模拟版）
 *
 * 使用内存数据模拟数据库查询
 * 模拟一个简单的"公司员工管理系统"数据
 *
 */
@Component
public class DbQueryTool {

    // 模拟员工数据
    private final List<Map<String, String>> employees;
    // 模拟部门数据
    private final List<Map<String, String>> departments;

    public DbQueryTool() {
        this.employees = initEmployees();
        this.departments = initDepartments();
    }

    /**
     * 查询员工信息
     */
    @Tool(description = "根据条件查询员工信息。可以按姓名、部门、职位查询。不传条件则返回所有员工。")
    public String queryEmployees(
            @ToolParam(description = "查询条件：员工姓名（模糊匹配），传空字符串则不限制") String name,
            @ToolParam(description = "查询条件：部门名称（精确匹配），传空字符串则不限制") String department) {
        List<Map<String, String>> results = new ArrayList<>();

        for (Map<String, String> emp : employees) {
            boolean match = true;

            if (name != null && !name.isBlank()) {
                match = emp.get("name").contains(name);
            }
            if (match && department != null && !department.isBlank()) {
                match = emp.get("department").equals(department);
            }

            if (match) {
                results.add(emp);
            }
        }

        if (results.isEmpty()) {
            return "未找到符合条件的员工（姓名=" + name + "，部门=" + department + "）";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("查询结果（共").append(results.size()).append("条）：\n");
        sb.append(String.format("%-6s %-8s %-10s %-8s %-10s\n", "工号", "姓名", "部门", "职位", "薪资"));
        sb.append("-".repeat(50)).append("\n");

        for (Map<String, String> emp : results) {
            sb.append(String.format("%-6s %-8s %-10s %-8s %-10s\n",
                    emp.get("id"), emp.get("name"), emp.get("department"),
                    emp.get("position"), emp.get("salary")));
        }

        return sb.toString();
    }

    /**
     * 查询部门信息
     */
    @Tool(description = "查询公司部门信息，包括部门名称、负责人、人数等")
    public String queryDepartments() {
        StringBuilder sb = new StringBuilder();
        sb.append("公司部门列表：\n");
        sb.append(String.format("%-12s %-8s %-6s %-20s\n", "部门名称", "负责人", "人数", "职能描述"));
        sb.append("-".repeat(60)).append("\n");

        for (Map<String, String> dept : departments) {
            sb.append(String.format("%-12s %-8s %-6s %-20s\n",
                    dept.get("name"), dept.get("manager"),
                    dept.get("headcount"), dept.get("description")));
        }

        return sb.toString();
    }

    /**
     * 统计查询
     */
    @Tool(description = "进行员工数据统计。支持的统计类型：count(总人数)、avg_salary(平均薪资)、dept_count(各部门人数)")
    public String statistics(
            @ToolParam(description = "统计类型：count、avg_salary、dept_count") String statType) {
        return switch (statType.toLowerCase()) {
            case "count" -> "公司员工总数：" + employees.size() + " 人";
            case "avg_salary" -> {
                double avg = employees.stream()
                        .mapToDouble(e -> Double.parseDouble(e.get("salary")))
                        .average()
                        .orElse(0);
                yield String.format("公司平均薪资：%.0f 元/月", avg);
            }
            case "dept_count" -> {
                Map<String, Long> countMap = new LinkedHashMap<>();
                for (Map<String, String> emp : employees) {
                    countMap.merge(emp.get("department"), 1L, Long::sum);
                }
                StringBuilder sb = new StringBuilder("各部门人数统计：\n");
                countMap.forEach((dept, count) ->
                        sb.append("- ").append(dept).append("：").append(count).append(" 人\n"));
                yield sb.toString();
            }
            default -> "不支持的统计类型：" + statType + "。支持：count、avg_salary、dept_count";
        };
    }

    // ==================== 模拟数据初始化 ====================

    private List<Map<String, String>> initEmployees() {
        List<Map<String, String>> list = new ArrayList<>();
        list.add(createEmployee("E001", "张三", "技术部", "高级工程师", "25000"));
        list.add(createEmployee("E002", "李四", "技术部", "中级工程师", "18000"));
        list.add(createEmployee("E003", "王五", "技术部", "初级工程师", "12000"));
        list.add(createEmployee("E004", "赵六", "产品部", "产品经理", "22000"));
        list.add(createEmployee("E005", "钱七", "产品部", "产品助理", "10000"));
        list.add(createEmployee("E006", "孙八", "市场部", "市场总监", "30000"));
        list.add(createEmployee("E007", "周九", "市场部", "市场专员", "11000"));
        list.add(createEmployee("E008", "吴十", "人事部", "HR经理", "20000"));
        list.add(createEmployee("E009", "郑十一", "财务部", "财务主管", "19000"));
        list.add(createEmployee("E010", "王小明", "技术部", "架构师", "35000"));
        return list;
    }

    private List<Map<String, String>> initDepartments() {
        List<Map<String, String>> list = new ArrayList<>();
        list.add(createDepartment("技术部", "王小明", "4", "负责公司产品研发和技术架构"));
        list.add(createDepartment("产品部", "赵六", "2", "负责产品规划和需求管理"));
        list.add(createDepartment("市场部", "孙八", "2", "负责市场推广和品牌运营"));
        list.add(createDepartment("人事部", "吴十", "1", "负责招聘和员工关系管理"));
        list.add(createDepartment("财务部", "郑十一", "1", "负责公司财务和预算管理"));
        return list;
    }

    private Map<String, String> createEmployee(String id, String name, String dept, String position, String salary) {
        Map<String, String> emp = new LinkedHashMap<>();
        emp.put("id", id);
        emp.put("name", name);
        emp.put("department", dept);
        emp.put("position", position);
        emp.put("salary", salary);
        return emp;
    }

    private Map<String, String> createDepartment(String name, String manager, String headcount, String description) {
        Map<String, String> dept = new LinkedHashMap<>();
        dept.put("name", name);
        dept.put("manager", manager);
        dept.put("headcount", headcount);
        dept.put("description", description);
        return dept;
    }
}
