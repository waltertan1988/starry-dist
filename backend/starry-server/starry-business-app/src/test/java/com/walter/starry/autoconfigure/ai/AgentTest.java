package com.walter.starry.autoconfigure.ai;

import com.walter.starry.business.app.BusinessApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 智能体测试
 * 参考：https://blog.csdn.net/2302_79380280/article/details/149840367
 */
@Slf4j
@SpringBootTest(classes = BusinessApplication.class)
public class AgentTest {
    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Nested
    class EvaluatorOptimizerTest{

        static class SimpleEvaluatorOptimizer {

            private final ChatClient chatClient;

            //中文生成器提示词
            private static final String GENERATOR_PROMPT = """
                你是一个Java代码生成助手。请根据任务要求生成高质量的Java代码。
                重要提醒：
                - 第一次生成时，创建一个基础但完整的实现
                - 如果收到反馈，请仔细分析每一条建议并逐一改进
                - 每次迭代都要在前一版本基础上显著提升代码质量
                - 不要一次性实现所有功能，而是逐步完善
            
                必须以JSON格式回复：
                {"thoughts":"详细说明本轮的改进思路","response":"改进后的Java代码"}
            """;

            //中文评估器提示词
            private static final String EVALUATOR_PROMPT = """
                    你是一个非常严格的面试官。请从以下维度严格评估代码：
                    1. 代码是否高效：从底层分析每一个类型以满足最佳性能!
                    2. 满足不重复扩容影响的性能
                    
                    评估标准：
                    - 只有当代码满足要求达到优秀水平时才返回PASS
                    - 如果任何一个维度有改进空间，必须返回NEEDS_IMPROVEMENT
                    - 提供具体、详细的改进建议
                    
                    必须以JSON格式回复：
                    {"evaluation":"PASS或NEEDS_IMPROVEMENT或FAIL","feedback"："详细的分维度反馈"}
                    
                    记住：宁可严格也不要放松标准！
                    """;

            // 迭代计数器
            int iteration = 0;
            // 最大迭代次数
            final int MAX_ITERATIONS = 3;
            // 上下文信息，用于在迭代间传递信息
            String context = "";

            public SimpleEvaluatorOptimizer(ChatClient chatClient) {
                this.chatClient = chatClient;
            }

            public RefinedResponse loop(String task) {
                System.out.println("=== 第" + (iteration + 1) + "轮迭代 ===");

                // 1. 生成代码阶段
                Generation generation = this.generate(task, context);

                // 2. 评估代码阶段
                EvaluationResponse evaluation = this.evaluate(generation.response(), task);
                System.out.println("生成结果: " + generation.response());
                System.out.println("评估结果: " + evaluation.evaluation());
                System.out.println("反馈: " + evaluation.feedback());

                // 3. 检查是否通过评估
                if (evaluation.evaluation() == EvaluationResponse.Evaluation.PASS) {
                    System.out.println("代码通过评估！");
                    return new RefinedResponse(generation.response());
                }else if(iteration >= MAX_ITERATIONS){
                    System.out.println("\n\n\n已超过最大迭代次数，评估结束！");
                    return new RefinedResponse(generation.response());
                }else{
                    // 准备下一轮的上下文（包含前一轮的代码和反馈）
                    context = String.format("之前的尝试:\n%s\n\n评估反馈:\n%s\n\n请根据反馈改进代码。",
                            generation.response(), evaluation.feedback());
                    iteration++;
                    // 递归调用继续迭代
                    return this.loop(task);
                }
            }

            /**
             * 生成代码方法
             * @param task 任务描述
             * @param context 上下文信息（前一轮的代码和反馈）
             * @return 生成的代码和思考过程
             */
            private Generation generate(String task, String context) {
                return chatClient.prompt()
                        .user(u -> u.text("{prompt}\n{context}\n任务: {task}")
                                .param("prompt", GENERATOR_PROMPT)  // 生成器提示词
                                .param("context", context)  // 上下文信息
                                .param("task", task))  // 任务描述
                        .call()
                        .entity(Generation.class);  // 映射为Generation对象
            }

            /**
             * 评估代码方法
             * @param content 需要评估的代码内容
             * @param task 原始任务描述
             * @return 评估结果和反馈
             */
            private EvaluationResponse evaluate(String content, String task) {
                return chatClient.prompt()
                        .user(u -> u.text("{prompt}\n\n任务: {task}\n\n代码:\n{content}")
                                .param("prompt", EVALUATOR_PROMPT)  // 评估器提示词
                                .param("task", task)  // 任务描述
                                .param("content", content))  // 需要评估的代码
                        .call()
                        .entity(EvaluationResponse.class);  // 映射为EvaluationResponse对象
            }

            /**
             * 代码生成结果记录类
             * @param thoughts 生成过程中的思考过程
             * @param response 生成的代码
             */
            public record Generation(String thoughts, String response) {}

            /**
             * 评估结果记录类
             * @param evaluation 评估结果枚举
             * @param feedback 详细的反馈信息
             */
            public record EvaluationResponse(Evaluation evaluation, String feedback) {
                /**
                 * 评估结果枚举
                 * PASS: 通过评估
                 * NEEDS_IMPROVEMENT: 需要改进
                 * FAIL: 失败
                 */
                public enum Evaluation { PASS, NEEDS_IMPROVEMENT, FAIL }
            }

            /**
             * 最终优化结果记录类
             * @param solution 优化后的解决方案
             */
            public record RefinedResponse(String solution) {}
        }

        @Test
        void test(){
            ChatClient chatClient = ChatClient.builder(openAiChatModel).build();

//            String task = """
//                    <user input>
//                        面试被问：怎么高效的将10000行list<User>数据，转化成map<id，user>，不是用stream.
//                    </user input>
//                    """;

            String task = """
                    <user input>
                        手工编写一个排序算法，为以下数组进行排序：
                        [2,9,6,7,5,2,0,4,4,6]
                    </user input>
                    """;

            System.out.printf(">>>最终的代码如下：\n%s%n", new SimpleEvaluatorOptimizer(chatClient).loop(task).solution());
        }
    }

    @Nested
    class SimpleOrchestratorWorkersTest{

        static class SimpleOrchestratorWorkers{

            // 聊天客户端，用于与AI模型交互
            private final ChatClient chatClient;

            /**
             * 编排器提示词模板（中文）
             * 用于指导AI如何分解复杂任务
             * 使用文本块语法（Java 15+特性）保持格式
             */
            private static final String ORCHESTRATOR_PROMPT = """
                你是一个项目管理专家，需要将复杂任务分解为可并行执行的专业子任务。
                    任务: {task}
                    请分析任务的复杂性和专业领域需求，将其分解为2-4个需要不同专业技能的子任务。
                    每个子任务应该：
                    1. 有明确的专业领域（如：前端开发、后端API、数据库设计、测试等）
                    2. 可以独立执行
                    3. 有具体的交付物

                    请以JSON格式回复：
                    {
                        "analysis": "任务复杂度分析和分解策略",
                        "tasks": [
                            {
                                "type": "后端API开发",
                                "description": "设计并实现RESTful API接口，包括数据验证和错误处理"
                            },
                            {
                                "type": "前端界面开发",
                                "description": "创建响应式用户界面，实现与后端API的交互"
                            },
                            {
                                "type": "数据库设计",
                                "description": "设计数据表结构，编写SQL脚本和索引优化"
                            }
                        ]
                    }
            """;

            /**
             * 工作者提示词模板（中文）
             * 用于指导AI如何完成特定专业领域的子任务
             */
            private static final String WORKER_PROMPT = """
            你是一个{task_type}领域的资深专家，请完成以下专业任务：
              项目背景: {original_task}
              专业领域: {task_type}
              具体任务: {task_description}

              请按照行业最佳实践完成任务，包括：
              1. 技术选型和架构考虑
              2. 具体实现方案
              3. 潜在风险和解决方案
              4. 质量保证措施

              请提供专业、详细的解决方案。
            """;

            /**
             * 构造函数
             * @param chatClient Spring AI的聊天客户端，用于与AI模型交互
             */
            public SimpleOrchestratorWorkers(ChatClient chatClient) {
                this.chatClient = chatClient;
            }

            /**
             * 处理任务的主方法
             * 1. 先使用编排器分解任务
             * 2. 然后使用工作者处理各个子任务
             * @param taskDescription 原始任务描述
             */
            public void process(String taskDescription) {
                System.out.println("=== 开始处理任务 ===");

                // 步骤1: 使用编排器分析并分解任务
                // 通过ChatClient发送提示词并获取响应
                OrchestratorResponse orchestratorResponse = chatClient.prompt()
                        .system(p -> p.param("task", taskDescription))  // 设置系统参数
                        .user(ORCHESTRATOR_PROMPT)  // 设置用户提示词
                        .call()  // 执行调用
                        .entity(OrchestratorResponse.class);  // 将响应映射为OrchestratorResponse对象

                // 打印编排器分析结果
                System.out.println("编排器分析: " + orchestratorResponse.analysis());
                System.out.println("子任务列表: " + orchestratorResponse.tasks());

                // 步骤2: 并行处理各个子任务
                orchestratorResponse.tasks().stream()
                        .map(task -> {
                            // 打印当前处理的子任务信息
                            System.out.println("-----------------------------------处理子任务: " + task.type()+"--------------------------------");
                            // 调用工作者处理子任务
                            String content = chatClient.prompt()
                                    .user(u -> u.text(WORKER_PROMPT)
                                            .param("original_task", taskDescription)  // 原始任务
                                            .param("task_type", task.type())  // 子任务类型
                                            .param("task_description", task.description()))  // 子任务描述
                                    .call()
                                    .content();  // 获取响应内容
                            System.out.println(content);
                            return task;
                        }).toList();  // 收集结果（虽然这里没有使用结果）

                System.out.println("=== 所有工作者完成任务 ===");
            }

            // 以下是数据记录类（Java 14+ record特性）

            /**
             * 子任务记录类
             * @param type 任务类型（如"后端API开发"）
             * @param description 任务描述
             */
            public record Task(String type, String description) {}

            /**
             * 编排器响应记录类
             * @param analysis 任务分析结果
             * @param tasks 分解后的子任务列表
             */
            public record OrchestratorResponse(String analysis, List<Task> tasks) {}

            /**
             * 最终响应记录类（当前未使用）
             * @param analysis 分析结果
             * @param workerResponses 工作者响应列表
             */
            public record FinalResponse(String analysis, List<String> workerResponses) {}
        }

        @Test
        void test(){
            ChatClient chatClient = ChatClient.builder(openAiChatModel).build();

            // 启动任务编排处理器，处理企业考勤系统设计需求
            new SimpleOrchestratorWorkers(chatClient)
                    .process("设计一个企业级的员工考勤系统，支持多种打卡方式和报表生成");
        }
    }
}
