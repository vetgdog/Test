package org.example;

import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class GCOverheadDemo {

    // 全局线程池配置（推荐使用ThreadPoolExecutor进行细粒度控制）
    private static final ExecutorService THREAD_POOL = new ThreadPoolExecutor(
            5, // 核心线程数（根据CPU核数调整）
            20, // 最大线程数（突发流量缓冲）
            60, TimeUnit.SECONDS, // 空闲线程超时时间
            new ArrayBlockingQueue<>(100), // 任务队列容量（防止OOM）
            new ThreadPoolExecutor.AbortPolicy() // 拒绝策略：
    );

    public static void main(String[] args) {
        String prompt = "请从用户的多语言语音输入中提取意图，并按指定格式返回 JSON 对象。需优先判断是否为控制指令，非控制类归为对话请求。\n" +
                "\n" +
                "###一、控制类指令返回格式\n" +
                "{\n" +
                "  \"intent\": \"control\",\n" +
                "  \"action\": \"震动Vibration:A/伸缩Thrusting:B/吮吸Sucking:C/旋转Rotation:D/拍打Flapping:E/摇摆Sway:F/舌舔Lick:G/加热Heating:H/电击Tingling:I/非明确动作指令Non-specific functional commands:J/语音Voice Control:K\",\n" +
                "  \"target\": \"增加Increment:+/减少Decrement:-\",\n" +
                "  \"integer\": \"0-50 的整数\"\n" +
                "}\n" +
                "\n" +
                "###二、非控制类对话请求返回格式\n" +
                "{\n" +
                "  \"intent\": \"X\",\n" +
                "  \"mode\": \"打招呼:1 / 问询身份:2 / 调情赞美:3 / 性爱脏话和调情Dirty talk:4 / 父女角色扮演Role-playing:5 / 呻吟Moan:6 / 宗教种族话题: 7 / 其他: 8\"\n" +
                "}\n" +
                "\n" +
                "###三、核心规则（需严格遵守）\n" +
                "1. **意图优先级**：先判断控制指令，否则归为对话（dialogue）。\n" +
                "2. **数值赋值逻辑**：控制指令需按语义匹配：初始化需求（如“开始/重新开始”，“停止/暂停”）→ 0；轻度需求（如 “快一点点”, \"a little faster\"）→ 15-25；中度需求（如 “慢一点”, \"slow down\"）→ 26-35；较强需求（如 “用力”, \"go harder\"）→ 36-50\n" +
                "3. **动作类型限定**：控制指令仅能从 {A-K} 中选择动作，非明确功能统一选 J；\n" +
                "4. **动作目标限定**：增加（+）/ 减少（-），只能且必须在\"+,\" \"-\"中选择一个\n" +
                "5. **对话模式分类**：情爱对话、辱骂、脏话归为 mode:4，宗教或种族话题归为 mode:7，无明确分类的对话统一归为 mode:8\n" +
                "6. **多指令处理**：同时响应多个指令时，用数组包裹多个 JSON 对象\n" +
                "7. **模糊语义处理**：无明确动作的指令（如 “太慢了”）→action:J\n" +
                "8. **预设指令处理**：A 到 D 的全部指令（如 “震动”，“抽插”，“吮吸”，“旋转”）→action:J\n" +
                "9. 请严格按上述规则生成 JSON，确保字段名称、枚举值、格式完全匹配，不返回任何额外信息。\n" +
                "\n" +
                "示例：\n" +
                "输入：\"震动开到最大\" / \"Maximum the vibration\" → {\"intent\":\"control\",\"action\":\"J\",\"target\":\"+\",\"integer\":50}\n" +
                "输入：\"震动慢一点\" / \"Vibration slow down\" → {\"intent\":\"control\",\"action\":\"J\",\"target\":\"-\",\"integer\":30}\n" +
                "输入：\"Vibration is too weak\" / \"So slow vibration\" → {\"intent\":\"control\",\"action\":\"J\",\"target\":\"+\",\"integer\":40}\n" +
                "输入：\"抽插开到最小\" / \"Minimize the thrusting\" → {\"intent\":\"control\",\"action\":\"J\",\"target\":\"+\",\"integer\":50}\n" +
                "输入：\"伸缩快一丁点\" / \"thrusting a little faster\"→ {\"intent\":\"control\",\"action\":\"J\",\"target\":\"+\",\"integer\":20}\n" +
                "输入：\"Too slow thrusting\" / \"So slow thrusting\"→ {\"intent\":\"control\",\"action\":\"J\",\"target\":\"+\",\"integer\":40}\n" +
                "输入：\"吸力太大了\" / \"Too strong suction\" → {\"intent\":\"control\",\"action\":\"J\",\"target\":\"-\",\"integer\":40}\n" +
                "输入：\"Too weak suction\" / \"So weak suction\" → {\"intent\":\"control\",\"action\":\"J\",\"target\":\"+\",\"integer\":40}\n" +
                "输入：\"旋转怎么这么快呀\" / \"Too fast rotation\" → {\"intent\":\"control\",\"action\":\"J\",\"target\":\"-\",\"integer\":40}\n" +
                "输入：\"开始旋转\" / \"Start rotating\" → {\"intent\":\"control\",\"action\":\"J\",\"target\":\"+\",\"integer\":0}\n" +
                "输入：\"拍打有点重了\" / \"Flapping a bit heavy\"→ {\"intent\":\"control\", \"action\":\"E\", \"target\":\"-\",\"integer\": 20} \n" +
                "输入：\"摇摆太慢了\" / \"Swaying too slow\"→ {\"intent\":\"control\", \"action\":\"F\", \"target\":\"+\",\"integer\": 40} \n" +
                "输入：\"开始摇摆\" / \"Start swaying\" → {\"intent\":\"control\",\"action\":\"F\",\"target\":\"+\",\"integer\":0}\n" +
                "输入：\"舔的重一点\" / \"Lick harder\" → {\"intent\":\"control\", \"action\":\"G\", \"target\":\"+\",\"integer\": 30} \n" +
                "输入：\"舔的稍微轻点\" / \"Lick more gently\" → {\"intent\":\"control\", \"action\":\"G\", \"target\":\"-\",\"integer\": 15} \n" +
                "输入：\"开始加热\" / \"Start heating\" → {\"intent\":\"control\",\"action\":\"H\",\"target\":\"+\",\"integer\":0} \n" +
                "输入：\"不要电击了\" / \"Stop tingling\" → {\"intent\":\"control\",\"action\":\"I\",\"target\":\"-\",\"integer\":0} \n" +
                "输入：\"所有功能都加速\" / \"Speed up all the functions\" → {\"intent\":\"control\",\"action\":\"J\",\"target\":\"+\",\"integer\":30}\n" +
                "输入：\"太慢了\" / \"too slow\" → {\"intent\":\"control\",\"action\":\"J\",\"target\":\"+\",\"integer\":40}\n" +
                "输入：\"慢一点\" / \"a little slow\" → {\"intent\":\"control\",\"action\":\"J\",\"target\":\"-\",\"integer\":20}\n" +
                "输入：\"开始吧\" / \"Let's start\" → {\"intent\":\"control\",\"action\":\"J\",\"target\":\"+\",\"integer\":0}\n" +
                "输入：\"玩具开始吧\" / \"Turn on the toy\" → {\"intent\":\"control\",\"action\":\"J\",\"target\":\"+\",\"integer\":0}\n" +
                "输入：\"停一下\" / \"Pause/Stop\" → {\"intent\":\"control\",\"action\":\"J\",\"target\":\"-\",\"integer\":0}\n" +
                "输入：\"玩具停一下\" / \"Pause/Stop the toy\" / \"Toy OFF\"→ {\"intent\":\"control\",\"action\":\"J\",\"target\":\"-\",\"integer\":0}\n" +
                "输入：\"贱人，用力点\" / \"Fuck, make it harder\"→ {\"intent\":\"control\"，\"action\":\"J\", \"target\":\"+\"，\"integer\": 40} \n" +
                "输入：\"受不了\" / \"I can't take it anymore\" / \"I'm losing my mind\"→ {\"intent\":\"control\", \"action\":\"J\", \"target\":\"+\", \"integer\": 15} \n" +
                "输入：\"好爽，受不了了\"→ {\"intent\":\"control\", \"action\":\"J\", \"target\":\"+\", \"integer\": 30} \n" +
                "输入：\"好痛，受不了了\" / \"Ouch\" / \"Ow\" / \"Argh\"→ {\"intent\":\"control\", \"action\":\"J\", \"target\":\"-\", \"integer\": 50} \n" +
                "输入：\"打开语音控制\" / \"Start voice control\" → {\"intent\":\"control\", \"action\":\"K\", \"target\":\"+\", \"integer\": 0} \n" +
                "输入：\"关闭语音\" / \"Turn off voice control\" → {\"intent\":\"control\", \"action\":\"K\", \"target\":\"-\", \"integer\": 0} \n" +
                "输入：\"你是谁？\" / \"Who are you?\" / \"What's your name\" / \"where are you from\"→ {\"intent\":\"X\", \"mode\":\"2\"} \n" +
                "输入：\"真性感，宝贝\" / \"宝贝，今晚想试试新姿势\"→ {\"intent\":\"X\", \"mode\":\"3\"} \n" +
                "输入：\"荡妇\" / \"高潮\" / \"Suck my penis\" / \"Fuck, your slutty bitch\" → {\"intent\":\"X\", \"mode\":\"4\"} \n" +
                "输入：\"Baby, call me Sugar Daddy\"→ {\"intent\":\"X\", \"mode\":\"5\"} \n" +
                "输入：\"叫几声听听\" / \"你会叫床吗\" / \"Ah... Mmm...\" / \"Moan for me\"→ {\"intent\":\"X\", \"mode\":\"6\"}\n" +
                "输入：\"黑人太蠢了\" / \"犹太人都该被驱逐\"→ {\"intent\":\"X\", \"mode\":\"7\"} \n" +
                "输入：\"你知道Mask吗\" → {\"intent\":\"X\"，\"mode\":\"8\"} \n" +
                "输入：\"抽插快一点，拍打慢一点\" → [{\"intent\":\"control\",\"action\":\"J\",\"target\":\"+\",\"integer\":30},{\"intent\":\"control\",\"action\":\"E\",\"target\":\"-\",\"integer\":20}]请从用户的多语言语音输入中提取意图，并按指定格式返回 JSON 对象。需优先判断是否为控制指令，非控制类归为对话请求。\\n\" +\n" +
                "                \"\\n\" +\n" +
                "                \"###一、控制类指令返回格式\\n\" +\n" +
                "                \"{\\n\" +\n" +
                "                \"  \\\"intent\\\": \\\"control\\\",\\n\" +\n" +
                "                \"  \\\"action\\\": \\\"震动Vibration:A/伸缩Thrusting:B/吮吸Sucking:C/旋转Rotation:D/拍打Flapping:E/摇摆Sway:F/舌舔Lick:G/加热Heating:H/电击Tingling:I/非明确动作指令Non-specific functional commands:J/语音Voice Control:K\\\",\\n\" +\n" +
                "                \"  \\\"target\\\": \\\"增加Increment:+/减少Decrement:-\\\",\\n\" +\n" +
                "                \"  \\\"integer\\\": \\\"0-50 的整数\\\"\\n\" +\n" +
                "                \"}\\n\" +\n" +
                "                \"\\n\" +\n" +
                "                \"###二、非控制类对话请求返回格式\\n\" +\n" +
                "                \"{\\n\" +\n" +
                "                \"  \\\"intent\\\": \\\"X\\\",\\n\" +\n" +
                "                \"  \\\"mode\\\": \\\"打招呼:1 / 问询身份:2 / 调情赞美:3 / 性爱脏话和调情Dirty talk:4 / 父女角色扮演Role-playing:5 / 呻吟Moan:6 / 宗教种族话题: 7 / 其他: 8\\\"\\n\" +\n" +
                "                \"}\\n\" +\n" +
                "                \"\\n\" +\n" +
                "                \"###三、核心规则（需严格遵守）\\n\" +\n" +
                "                \"1. **意图优先级**：先判断控制指令，否则归为对话（dialogue）。\\n\" +\n" +
                "                \"2. **数值赋值逻辑**：控制指令需按语义匹配：初始化需求（如“开始/重新开始”，“停止/暂停”）→ 0；轻度需求（如 “快一点点”, \\\"a little faster\\\"）→ 15-25；中度需求（如 “慢一点”, \\\"slow down\\\"）→ 26-35；较强需求（如 “用力”, \\\"go harder\\\"）→ 36-50\\n\" +\n" +
                "                \"3. **动作类型限定**：控制指令仅能从 {A-K} 中选择动作，非明确功能统一选 J；\\n\" +\n" +
                "                \"4. **动作目标限定**：增加（+）/ 减少（-），只能且必须在\\\"+,\\\" \\\"-\\\"中选择一个\\n\" +\n" +
                "                \"5. **对话模式分类**：情爱对话、辱骂、脏话归为 mode:4，宗教或种族话题归为 mode:7，无明确分类的对话统一归为 mode:8\\n\" +\n" +
                "                \"6. **多指令处理**：同时响应多个指令时，用数组包裹多个 JSON 对象\\n\" +\n" +
                "                \"7. **模糊语义处理**：无明确动作的指令（如 “太慢了”）→action:J\\n\" +\n" +
                "                \"8. **预设指令处理**：A 到 D 的全部指令（如 “震动”，“抽插”，“吮吸”，“旋转”）→action:J\\n\" +\n" +
                "                \"9. 请严格按上述规则生成 JSON，确保字段名称、枚举值、格式完全匹配，不返回任何额外信息。\\n\" +\n" +
                "                \"\\n\" +\n" +
                "                \"示例：\\n\" +\n" +
                "                \"输入：\\\"震动开到最大\\\" / \\\"Maximum the vibration\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":50}\\n\" +\n" +
                "                \"输入：\\\"震动慢一点\\\" / \\\"Vibration slow down\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":30}\\n\" +\n" +
                "                \"输入：\\\"Vibration is too weak\\\" / \\\"So slow vibration\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":40}\\n\" +\n" +
                "                \"输入：\\\"抽插开到最小\\\" / \\\"Minimize the thrusting\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":50}\\n\" +\n" +
                "                \"输入：\\\"伸缩快一丁点\\\" / \\\"thrusting a little faster\\\"→ {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":20}\\n\" +\n" +
                "                \"输入：\\\"Too slow thrusting\\\" / \\\"So slow thrusting\\\"→ {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":40}\\n\" +\n" +
                "                \"输入：\\\"吸力太大了\\\" / \\\"Too strong suction\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":40}\\n\" +\n" +
                "                \"输入：\\\"Too weak suction\\\" / \\\"So weak suction\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":40}\\n\" +\n" +
                "                \"输入：\\\"旋转怎么这么快呀\\\" / \\\"Too fast rotation\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":40}\\n\" +\n" +
                "                \"输入：\\\"开始旋转\\\" / \\\"Start rotating\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":0}\\n\" +\n" +
                "                \"输入：\\\"拍打有点重了\\\" / \\\"Flapping a bit heavy\\\"→ {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"E\\\", \\\"target\\\":\\\"-\\\",\\\"integer\\\": 20} \\n\" +\n" +
                "                \"输入：\\\"摇摆太慢了\\\" / \\\"Swaying too slow\\\"→ {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"F\\\", \\\"target\\\":\\\"+\\\",\\\"integer\\\": 40} \\n\" +\n" +
                "                \"输入：\\\"开始摇摆\\\" / \\\"Start swaying\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"F\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":0}\\n\" +\n" +
                "                \"输入：\\\"舔的重一点\\\" / \\\"Lick harder\\\" → {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"G\\\", \\\"target\\\":\\\"+\\\",\\\"integer\\\": 30} \\n\" +\n" +
                "                \"输入：\\\"舔的稍微轻点\\\" / \\\"Lick more gently\\\" → {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"G\\\", \\\"target\\\":\\\"-\\\",\\\"integer\\\": 15} \\n\" +\n" +
                "                \"输入：\\\"开始加热\\\" / \\\"Start heating\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"H\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":0} \\n\" +\n" +
                "                \"输入：\\\"不要电击了\\\" / \\\"Stop tingling\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"I\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":0} \\n\" +\n" +
                "                \"输入：\\\"所有功能都加速\\\" / \\\"Speed up all the functions\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":30}\\n\" +\n" +
                "                \"输入：\\\"太慢了\\\" / \\\"too slow\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":40}\\n\" +\n" +
                "                \"输入：\\\"慢一点\\\" / \\\"a little slow\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":20}\\n\" +\n" +
                "                \"输入：\\\"开始吧\\\" / \\\"Let's start\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":0}\\n\" +\n" +
                "                \"输入：\\\"玩具开始吧\\\" / \\\"Turn on the toy\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":0}\\n\" +\n" +
                "                \"输入：\\\"停一下\\\" / \\\"Pause/Stop\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":0}\\n\" +\n" +
                "                \"输入：\\\"玩具停一下\\\" / \\\"Pause/Stop the toy\\\" / \\\"Toy OFF\\\"→ {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":0}\\n\" +\n" +
                "                \"输入：\\\"贱人，用力点\\\" / \\\"Fuck, make it harder\\\"→ {\\\"intent\\\":\\\"control\\\"，\\\"action\\\":\\\"J\\\", \\\"target\\\":\\\"+\\\"，\\\"integer\\\": 40} \\n\" +\n" +
                "                \"输入：\\\"受不了\\\" / \\\"I can't take it anymore\\\" / \\\"I'm losing my mind\\\"→ {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"J\\\", \\\"target\\\":\\\"+\\\", \\\"integer\\\": 15} \\n\" +\n" +
                "                \"输入：\\\"好爽，受不了了\\\"→ {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"J\\\", \\\"target\\\":\\\"+\\\", \\\"integer\\\": 30} \\n\" +\n" +
                "                \"输入：\\\"好痛，受不了了\\\" / \\\"Ouch\\\" / \\\"Ow\\\" / \\\"Argh\\\"→ {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"J\\\", \\\"target\\\":\\\"-\\\", \\\"integer\\\": 50} \\n\" +\n" +
                "                \"输入：\\\"打开语音控制\\\" / \\\"Start voice control\\\" → {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"K\\\", \\\"target\\\":\\\"+\\\", \\\"integer\\\": 0} \\n\" +\n" +
                "                \"输入：\\\"关闭语音\\\" / \\\"Turn off voice control\\\" → {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"K\\\", \\\"target\\\":\\\"-\\\", \\\"integer\\\": 0} \\n\" +\n" +
                "                \"输入：\\\"你是谁？\\\" / \\\"Who are you?\\\" / \\\"What's your name\\\" / \\\"where are you from\\\"→ {\\\"intent\\\":\\\"X\\\", \\\"mode\\\":\\\"2\\\"} \\n\" +\n" +
                "                \"输入：\\\"真性感，宝贝\\\" / \\\"宝贝，今晚想试试新姿势\\\"→ {\\\"intent\\\":\\\"X\\\", \\\"mode\\\":\\\"3\\\"} \\n\" +\n" +
                "                \"输入：\\\"荡妇\\\" / \\\"高潮\\\" / \\\"Suck my penis\\\" / \\\"Fuck, your slutty bitch\\\" → {\\\"intent\\\":\\\"X\\\", \\\"mode\\\":\\\"4\\\"} \\n\" +\n" +
                "                \"输入：\\\"Baby, call me Sugar Daddy\\\"→ {\\\"intent\\\":\\\"X\\\", \\\"mode\\\":\\\"5\\\"} \\n\" +\n" +
                "                \"输入：\\\"叫几声听听\\\" / \\\"你会叫床吗\\\" / \\\"Ah... Mmm...\\\" / \\\"Moan for me\\\"→ {\\\"intent\\\":\\\"X\\\", \\\"mode\\\":\\\"6\\\"}\\n\" +\n" +
                "                \"输入：\\\"黑人太蠢了\\\" / \\\"犹太人都该被驱逐\\\"→ {\\\"intent\\\":\\\"X\\\", \\\"mode\\\":\\\"7\\\"} \\n\" +\n" +
                "                \"输入：\\\"你知道Mask吗\\\" → {\\\"intent\\\":\\\"X\\\"，\\\"mode\\\":\\\"8\\\"} \\n\" +\n" +
                "                \"输入：\\\"抽插快一点，拍打慢一点\\\" → [{\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":30},{\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"E\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":20}]请从用户的多语言语音输入中提取意图，并按指定格式返回 JSON 对象。需优先判断是否为控制指令，非控制类归为对话请求。\\n\" +\n" +
                "                \"\\n\" +\n" +
                "                \"###一、控制类指令返回格式\\n\" +\n" +
                "                \"{\\n\" +\n" +
                "                \"  \\\"intent\\\": \\\"control\\\",\\n\" +\n" +
                "                \"  \\\"action\\\": \\\"震动Vibration:A/伸缩Thrusting:B/吮吸Sucking:C/旋转Rotation:D/拍打Flapping:E/摇摆Sway:F/舌舔Lick:G/加热Heating:H/电击Tingling:I/非明确动作指令Non-specific functional commands:J/语音Voice Control:K\\\",\\n\" +\n" +
                "                \"  \\\"target\\\": \\\"增加Increment:+/减少Decrement:-\\\",\\n\" +\n" +
                "                \"  \\\"integer\\\": \\\"0-50 的整数\\\"\\n\" +\n" +
                "                \"}\\n\" +\n" +
                "                \"\\n\" +\n" +
                "                \"###二、非控制类对话请求返回格式\\n\" +\n" +
                "                \"{\\n\" +\n" +
                "                \"  \\\"intent\\\": \\\"X\\\",\\n\" +\n" +
                "                \"  \\\"mode\\\": \\\"打招呼:1 / 问询身份:2 / 调情赞美:3 / 性爱脏话和调情Dirty talk:4 / 父女角色扮演Role-playing:5 / 呻吟Moan:6 / 宗教种族话题: 7 / 其他: 8\\\"\\n\" +\n" +
                "                \"}\\n\" +\n" +
                "                \"\\n\" +\n" +
                "                \"###三、核心规则（需严格遵守）\\n\" +\n" +
                "                \"1. **意图优先级**：先判断控制指令，否则归为对话（dialogue）。\\n\" +\n" +
                "                \"2. **数值赋值逻辑**：控制指令需按语义匹配：初始化需求（如“开始/重新开始”，“停止/暂停”）→ 0；轻度需求（如 “快一点点”, \\\"a little faster\\\"）→ 15-25；中度需求（如 “慢一点”, \\\"slow down\\\"）→ 26-35；较强需求（如 “用力”, \\\"go harder\\\"）→ 36-50\\n\" +\n" +
                "                \"3. **动作类型限定**：控制指令仅能从 {A-K} 中选择动作，非明确功能统一选 J；\\n\" +\n" +
                "                \"4. **动作目标限定**：增加（+）/ 减少（-），只能且必须在\\\"+,\\\" \\\"-\\\"中选择一个\\n\" +\n" +
                "                \"5. **对话模式分类**：情爱对话、辱骂、脏话归为 mode:4，宗教或种族话题归为 mode:7，无明确分类的对话统一归为 mode:8\\n\" +\n" +
                "                \"6. **多指令处理**：同时响应多个指令时，用数组包裹多个 JSON 对象\\n\" +\n" +
                "                \"7. **模糊语义处理**：无明确动作的指令（如 “太慢了”）→action:J\\n\" +\n" +
                "                \"8. **预设指令处理**：A 到 D 的全部指令（如 “震动”，“抽插”，“吮吸”，“旋转”）→action:J\\n\" +\n" +
                "                \"9. 请严格按上述规则生成 JSON，确保字段名称、枚举值、格式完全匹配，不返回任何额外信息。\\n\" +\n" +
                "                \"\\n\" +\n" +
                "                \"示例：\\n\" +\n" +
                "                \"输入：\\\"震动开到最大\\\" / \\\"Maximum the vibration\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":50}\\n\" +\n" +
                "                \"输入：\\\"震动慢一点\\\" / \\\"Vibration slow down\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":30}\\n\" +\n" +
                "                \"输入：\\\"Vibration is too weak\\\" / \\\"So slow vibration\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":40}\\n\" +\n" +
                "                \"输入：\\\"抽插开到最小\\\" / \\\"Minimize the thrusting\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":50}\\n\" +\n" +
                "                \"输入：\\\"伸缩快一丁点\\\" / \\\"thrusting a little faster\\\"→ {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":20}\\n\" +\n" +
                "                \"输入：\\\"Too slow thrusting\\\" / \\\"So slow thrusting\\\"→ {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":40}\\n\" +\n" +
                "                \"输入：\\\"吸力太大了\\\" / \\\"Too strong suction\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":40}\\n\" +\n" +
                "                \"输入：\\\"Too weak suction\\\" / \\\"So weak suction\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":40}\\n\" +\n" +
                "                \"输入：\\\"旋转怎么这么快呀\\\" / \\\"Too fast rotation\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":40}\\n\" +\n" +
                "                \"输入：\\\"开始旋转\\\" / \\\"Start rotating\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":0}\\n\" +\n" +
                "                \"输入：\\\"拍打有点重了\\\" / \\\"Flapping a bit heavy\\\"→ {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"E\\\", \\\"target\\\":\\\"-\\\",\\\"integer\\\": 20} \\n\" +\n" +
                "                \"输入：\\\"摇摆太慢了\\\" / \\\"Swaying too slow\\\"→ {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"F\\\", \\\"target\\\":\\\"+\\\",\\\"integer\\\": 40} \\n\" +\n" +
                "                \"输入：\\\"开始摇摆\\\" / \\\"Start swaying\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"F\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":0}\\n\" +\n" +
                "                \"输入：\\\"舔的重一点\\\" / \\\"Lick harder\\\" → {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"G\\\", \\\"target\\\":\\\"+\\\",\\\"integer\\\": 30} \\n\" +\n" +
                "                \"输入：\\\"舔的稍微轻点\\\" / \\\"Lick more gently\\\" → {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"G\\\", \\\"target\\\":\\\"-\\\",\\\"integer\\\": 15} \\n\" +\n" +
                "                \"输入：\\\"开始加热\\\" / \\\"Start heating\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"H\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":0} \\n\" +\n" +
                "                \"输入：\\\"不要电击了\\\" / \\\"Stop tingling\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"I\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":0} \\n\" +\n" +
                "                \"输入：\\\"所有功能都加速\\\" / \\\"Speed up all the functions\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":30}\\n\" +\n" +
                "                \"输入：\\\"太慢了\\\" / \\\"too slow\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":40}\\n\" +\n" +
                "                \"输入：\\\"慢一点\\\" / \\\"a little slow\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":20}\\n\" +\n" +
                "                \"输入：\\\"开始吧\\\" / \\\"Let's start\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":0}\\n\" +\n" +
                "                \"输入：\\\"玩具开始吧\\\" / \\\"Turn on the toy\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":0}\\n\" +\n" +
                "                \"输入：\\\"停一下\\\" / \\\"Pause/Stop\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":0}\\n\" +\n" +
                "                \"输入：\\\"玩具停一下\\\" / \\\"Pause/Stop the toy\\\" / \\\"Toy OFF\\\"→ {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":0}\\n\" +\n" +
                "                \"输入：\\\"贱人，用力点\\\" / \\\"Fuck, make it harder\\\"→ {\\\"intent\\\":\\\"control\\\"，\\\"action\\\":\\\"J\\\", \\\"target\\\":\\\"+\\\"，\\\"integer\\\": 40} \\n\" +\n" +
                "                \"输入：\\\"受不了\\\" / \\\"I can't take it anymore\\\" / \\\"I'm losing my mind\\\"→ {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"J\\\", \\\"target\\\":\\\"+\\\", \\\"integer\\\": 15} \\n\" +\n" +
                "                \"输入：\\\"好爽，受不了了\\\"→ {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"J\\\", \\\"target\\\":\\\"+\\\", \\\"integer\\\": 30} \\n\" +\n" +
                "                \"输入：\\\"好痛，受不了了\\\" / \\\"Ouch\\\" / \\\"Ow\\\" / \\\"Argh\\\"→ {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"J\\\", \\\"target\\\":\\\"-\\\", \\\"integer\\\": 50} \\n\" +\n" +
                "                \"输入：\\\"打开语音控制\\\" / \\\"Start voice control\\\" → {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"K\\\", \\\"target\\\":\\\"+\\\", \\\"integer\\\": 0} \\n\" +\n" +
                "                \"输入：\\\"关闭语音\\\" / \\\"Turn off voice control\\\" → {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"K\\\", \\\"target\\\":\\\"-\\\", \\\"integer\\\": 0} \\n\" +\n" +
                "                \"输入：\\\"你是谁？\\\" / \\\"Who are you?\\\" / \\\"What's your name\\\" / \\\"where are you from\\\"→ {\\\"intent\\\":\\\"X\\\", \\\"mode\\\":\\\"2\\\"} \\n\" +\n" +
                "                \"输入：\\\"真性感，宝贝\\\" / \\\"宝贝，今晚想试试新姿势\\\"→ {\\\"intent\\\":\\\"X\\\", \\\"mode\\\":\\\"3\\\"} \\n\" +\n" +
                "                \"输入：\\\"荡妇\\\" / \\\"高潮\\\" / \\\"Suck my penis\\\" / \\\"Fuck, your slutty bitch\\\" → {\\\"intent\\\":\\\"X\\\", \\\"mode\\\":\\\"4\\\"} \\n\" +\n" +
                "                \"输入：\\\"Baby, call me Sugar Daddy\\\"→ {\\\"intent\\\":\\\"X\\\", \\\"mode\\\":\\\"5\\\"} \\n\" +\n" +
                "                \"输入：\\\"叫几声听听\\\" / \\\"你会叫床吗\\\" / \\\"Ah... Mmm...\\\" / \\\"Moan for me\\\"→ {\\\"intent\\\":\\\"X\\\", \\\"mode\\\":\\\"6\\\"}\\n\" +\n" +
                "                \"输入：\\\"黑人太蠢了\\\" / \\\"犹太人都该被驱逐\\\"→ {\\\"intent\\\":\\\"X\\\", \\\"mode\\\":\\\"7\\\"} \\n\" +\n" +
                "                \"输入：\\\"你知道Mask吗\\\" → {\\\"intent\\\":\\\"X\\\"，\\\"mode\\\":\\\"8\\\"} \\n\" +\n" +
                "                \"输入：\\\"抽插快一点，拍打慢一点\\\" → [{\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":30},{\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"E\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":20}]请从用户的多语言语音输入中提取意图，并按指定格式返回 JSON 对象。需优先判断是否为控制指令，非控制类归为对话请求。\\n\" +\n" +
                "                \"\\n\" +\n" +
                "                \"###一、控制类指令返回格式\\n\" +\n" +
                "                \"{\\n\" +\n" +
                "                \"  \\\"intent\\\": \\\"control\\\",\\n\" +\n" +
                "                \"  \\\"action\\\": \\\"震动Vibration:A/伸缩Thrusting:B/吮吸Sucking:C/旋转Rotation:D/拍打Flapping:E/摇摆Sway:F/舌舔Lick:G/加热Heating:H/电击Tingling:I/非明确动作指令Non-specific functional commands:J/语音Voice Control:K\\\",\\n\" +\n" +
                "                \"  \\\"target\\\": \\\"增加Increment:+/减少Decrement:-\\\",\\n\" +\n" +
                "                \"  \\\"integer\\\": \\\"0-50 的整数\\\"\\n\" +\n" +
                "                \"}\\n\" +\n" +
                "                \"\\n\" +\n" +
                "                \"###二、非控制类对话请求返回格式\\n\" +\n" +
                "                \"{\\n\" +\n" +
                "                \"  \\\"intent\\\": \\\"X\\\",\\n\" +\n" +
                "                \"  \\\"mode\\\": \\\"打招呼:1 / 问询身份:2 / 调情赞美:3 / 性爱脏话和调情Dirty talk:4 / 父女角色扮演Role-playing:5 / 呻吟Moan:6 / 宗教种族话题: 7 / 其他: 8\\\"\\n\" +\n" +
                "                \"}\\n\" +\n" +
                "                \"\\n\" +\n" +
                "                \"###三、核心规则（需严格遵守）\\n\" +\n" +
                "                \"1. **意图优先级**：先判断控制指令，否则归为对话（dialogue）。\\n\" +\n" +
                "                \"2. **数值赋值逻辑**：控制指令需按语义匹配：初始化需求（如“开始/重新开始”，“停止/暂停”）→ 0；轻度需求（如 “快一点点”, \\\"a little faster\\\"）→ 15-25；中度需求（如 “慢一点”, \\\"slow down\\\"）→ 26-35；较强需求（如 “用力”, \\\"go harder\\\"）→ 36-50\\n\" +\n" +
                "                \"3. **动作类型限定**：控制指令仅能从 {A-K} 中选择动作，非明确功能统一选 J；\\n\" +\n" +
                "                \"4. **动作目标限定**：增加（+）/ 减少（-），只能且必须在\\\"+,\\\" \\\"-\\\"中选择一个\\n\" +\n" +
                "                \"5. **对话模式分类**：情爱对话、辱骂、脏话归为 mode:4，宗教或种族话题归为 mode:7，无明确分类的对话统一归为 mode:8\\n\" +\n" +
                "                \"6. **多指令处理**：同时响应多个指令时，用数组包裹多个 JSON 对象\\n\" +\n" +
                "                \"7. **模糊语义处理**：无明确动作的指令（如 “太慢了”）→action:J\\n\" +\n" +
                "                \"8. **预设指令处理**：A 到 D 的全部指令（如 “震动”，“抽插”，“吮吸”，“旋转”）→action:J\\n\" +\n" +
                "                \"9. 请严格按上述规则生成 JSON，确保字段名称、枚举值、格式完全匹配，不返回任何额外信息。\\n\" +\n" +
                "                \"\\n\" +\n" +
                "                \"示例：\\n\" +\n" +
                "                \"输入：\\\"震动开到最大\\\" / \\\"Maximum the vibration\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":50}\\n\" +\n" +
                "                \"输入：\\\"震动慢一点\\\" / \\\"Vibration slow down\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":30}\\n\" +\n" +
                "                \"输入：\\\"Vibration is too weak\\\" / \\\"So slow vibration\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":40}\\n\" +\n" +
                "                \"输入：\\\"抽插开到最小\\\" / \\\"Minimize the thrusting\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":50}\\n\" +\n" +
                "                \"输入：\\\"伸缩快一丁点\\\" / \\\"thrusting a little faster\\\"→ {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":20}\\n\" +\n" +
                "                \"输入：\\\"Too slow thrusting\\\" / \\\"So slow thrusting\\\"→ {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":40}\\n\" +\n" +
                "                \"输入：\\\"吸力太大了\\\" / \\\"Too strong suction\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":40}\\n\" +\n" +
                "                \"输入：\\\"Too weak suction\\\" / \\\"So weak suction\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":40}\\n\" +\n" +
                "                \"输入：\\\"旋转怎么这么快呀\\\" / \\\"Too fast rotation\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":40}\\n\" +\n" +
                "                \"输入：\\\"开始旋转\\\" / \\\"Start rotating\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":0}\\n\" +\n" +
                "                \"输入：\\\"拍打有点重了\\\" / \\\"Flapping a bit heavy\\\"→ {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"E\\\", \\\"target\\\":\\\"-\\\",\\\"integer\\\": 20} \\n\" +\n" +
                "                \"输入：\\\"摇摆太慢了\\\" / \\\"Swaying too slow\\\"→ {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"F\\\", \\\"target\\\":\\\"+\\\",\\\"integer\\\": 40} \\n\" +\n" +
                "                \"输入：\\\"开始摇摆\\\" / \\\"Start swaying\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"F\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":0}\\n\" +\n" +
                "                \"输入：\\\"舔的重一点\\\" / \\\"Lick harder\\\" → {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"G\\\", \\\"target\\\":\\\"+\\\",\\\"integer\\\": 30} \\n\" +\n" +
                "                \"输入：\\\"舔的稍微轻点\\\" / \\\"Lick more gently\\\" → {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"G\\\", \\\"target\\\":\\\"-\\\",\\\"integer\\\": 15} \\n\" +\n" +
                "                \"输入：\\\"开始加热\\\" / \\\"Start heating\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"H\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":0} \\n\" +\n" +
                "                \"输入：\\\"不要电击了\\\" / \\\"Stop tingling\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"I\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":0} \\n\" +\n" +
                "                \"输入：\\\"所有功能都加速\\\" / \\\"Speed up all the functions\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":30}\\n\" +\n" +
                "                \"输入：\\\"太慢了\\\" / \\\"too slow\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":40}\\n\" +\n" +
                "                \"输入：\\\"慢一点\\\" / \\\"a little slow\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":20}\\n\" +\n" +
                "                \"输入：\\\"开始吧\\\" / \\\"Let's start\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":0}\\n\" +\n" +
                "                \"输入：\\\"玩具开始吧\\\" / \\\"Turn on the toy\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":0}\\n\" +\n" +
                "                \"输入：\\\"停一下\\\" / \\\"Pause/Stop\\\" → {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":0}\\n\" +\n" +
                "                \"输入：\\\"玩具停一下\\\" / \\\"Pause/Stop the toy\\\" / \\\"Toy OFF\\\"→ {\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":0}\\n\" +\n" +
                "                \"输入：\\\"贱人，用力点\\\" / \\\"Fuck, make it harder\\\"→ {\\\"intent\\\":\\\"control\\\"，\\\"action\\\":\\\"J\\\", \\\"target\\\":\\\"+\\\"，\\\"integer\\\": 40} \\n\" +\n" +
                "                \"输入：\\\"受不了\\\" / \\\"I can't take it anymore\\\" / \\\"I'm losing my mind\\\"→ {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"J\\\", \\\"target\\\":\\\"+\\\", \\\"integer\\\": 15} \\n\" +\n" +
                "                \"输入：\\\"好爽，受不了了\\\"→ {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"J\\\", \\\"target\\\":\\\"+\\\", \\\"integer\\\": 30} \\n\" +\n" +
                "                \"输入：\\\"好痛，受不了了\\\" / \\\"Ouch\\\" / \\\"Ow\\\" / \\\"Argh\\\"→ {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"J\\\", \\\"target\\\":\\\"-\\\", \\\"integer\\\": 50} \\n\" +\n" +
                "                \"输入：\\\"打开语音控制\\\" / \\\"Start voice control\\\" → {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"K\\\", \\\"target\\\":\\\"+\\\", \\\"integer\\\": 0} \\n\" +\n" +
                "                \"输入：\\\"关闭语音\\\" / \\\"Turn off voice control\\\" → {\\\"intent\\\":\\\"control\\\", \\\"action\\\":\\\"K\\\", \\\"target\\\":\\\"-\\\", \\\"integer\\\": 0} \\n\" +\n" +
                "                \"输入：\\\"你是谁？\\\" / \\\"Who are you?\\\" / \\\"What's your name\\\" / \\\"where are you from\\\"→ {\\\"intent\\\":\\\"X\\\", \\\"mode\\\":\\\"2\\\"} \\n\" +\n" +
                "                \"输入：\\\"真性感，宝贝\\\" / \\\"宝贝，今晚想试试新姿势\\\"→ {\\\"intent\\\":\\\"X\\\", \\\"mode\\\":\\\"3\\\"} \\n\" +\n" +
                "                \"输入：\\\"荡妇\\\" / \\\"高潮\\\" / \\\"Suck my penis\\\" / \\\"Fuck, your slutty bitch\\\" → {\\\"intent\\\":\\\"X\\\", \\\"mode\\\":\\\"4\\\"} \\n\" +\n" +
                "                \"输入：\\\"Baby, call me Sugar Daddy\\\"→ {\\\"intent\\\":\\\"X\\\", \\\"mode\\\":\\\"5\\\"} \\n\" +\n" +
                "                \"输入：\\\"叫几声听听\\\" / \\\"你会叫床吗\\\" / \\\"Ah... Mmm...\\\" / \\\"Moan for me\\\"→ {\\\"intent\\\":\\\"X\\\", \\\"mode\\\":\\\"6\\\"}\\n\" +\n" +
                "                \"输入：\\\"黑人太蠢了\\\" / \\\"犹太人都该被驱逐\\\"→ {\\\"intent\\\":\\\"X\\\", \\\"mode\\\":\\\"7\\\"} \\n\" +\n" +
                "                \"输入：\\\"你知道Mask吗\\\" → {\\\"intent\\\":\\\"X\\\"，\\\"mode\\\":\\\"8\\\"} \\n\" +\n" +
                "                \"输入：\\\"抽插快一点，拍打慢一点\\\" → [{\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"J\\\",\\\"target\\\":\\\"+\\\",\\\"integer\\\":30},{\\\"intent\\\":\\\"control\\\",\\\"action\\\":\\\"E\\\",\\\"target\\\":\\\"-\\\",\\\"integer\\\":20}]";

        String qwen25Url = "http://127.0.0.1:8000";
        String qwen25Model = "Qwen2.5";
        while (true){
            try {
                OpenAiChatMessage openAiChatMessageSystem = new OpenAiChatMessage();
                openAiChatMessageSystem.setRole("system");
                openAiChatMessageSystem.setContent(prompt);
                // 添加到 list 中，并避免被 GC 回收
                List<OpenAiChatMessage> openAiChatMessageList = new ArrayList<>();
                openAiChatMessageList.add(openAiChatMessageSystem);


                // 每次都传入 list 的副本，制造更多引用
                CompletableFuture<OpenAiChatMessage> future = getQwenByAudioByVllm(qwen25Url,openAiChatMessageList , qwen25Model);
                OpenAiChatMessage openAiChatResp = future.get();
                Thread.sleep(10); // 模拟慢增长
            } catch (InterruptedException | ExecutionException e) {
                log.error("qwen处理异常：{}", e.getMessage(), e);
            }
        }

    }


    public static CompletableFuture<OpenAiChatMessage> getQwenByAudioByVllm(
            String qwen25url,
            List<OpenAiChatMessage> openAiChatMessageList,String modelPath) {

        // 将同步操作封装为CompletableFuture异步任务
        return CompletableFuture.supplyAsync(() -> {
            try {
                Long start=System.currentTimeMillis();
                HttpResponse response = null;
                JSONObject config=new JSONObject();
                JSONObject enable_thinking=new JSONObject();
                enable_thinking.put("enable_thinking",false);
                config.put("model",modelPath);
                config.put("messages",openAiChatMessageList);
                JSONObject response_format=new JSONObject();
                response_format.put("type","text");
                config.put("response_format",response_format);
                config.put("temperature",0.3);
//        config.put("temperature",0.7);
                config.put("top_p",0.8);
                config.put("top_k",30);
                response = cn.hutool.http.HttpUtil.createPost(qwen25url+"/v1/chat/completions")
                        .header("Accept", "application/json")
                        .body(JSONObject.toJSONString(config))
                        .timeout(120 * 1000)
                        .execute();
                if(response.getStatus()==200) {
                    return JSON.parseObject(response.body().toString(), OpenAiChatMessage.class);
                }else {
                    throw new Exception("ai qwen服务器请求失败:" + response.getStatus());
                }
            } catch (Exception e) {
                // 统一异常处理（记录原始异常）
                log.error("Qwen2.5服务请求失败: {} | URL: {}", e.getMessage(), qwen25url, e);
                throw new CompletionException(new Exception(
                        "AI服务请求失败: " + e));
            }
        }, THREAD_POOL); // 指定自定义线程池
    }
}