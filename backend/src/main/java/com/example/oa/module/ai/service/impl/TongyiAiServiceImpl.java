package com.example.oa.module.ai.service.impl;

import com.example.oa.common.exception.BusinessException;
import com.example.oa.common.util.SecurityUtils;
import com.example.oa.module.ai.config.AiProperties;
import com.example.oa.module.ai.dto.AiQaRequest;
import com.example.oa.module.ai.dto.AiResponse;
import com.example.oa.module.ai.dto.NewsGenerateRequest;
import com.example.oa.module.ai.dto.NewsPolishRequest;
import com.example.oa.module.ai.dto.ScheduleParseRequest;
import com.example.oa.module.ai.entity.AiLog;
import com.example.oa.module.ai.enums.AiFunctionTypeEnum;
import com.example.oa.module.ai.mapper.AiLogMapper;
import com.example.oa.module.ai.util.AiLogSanitizer;
import com.example.oa.module.ai.service.AiService;
import com.example.oa.module.approval.entity.Approval;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oa.ai.provider", havingValue = "tongyi")
public class TongyiAiServiceImpl implements AiService {

    private static final String PROVIDER = "tongyi";

    private final AiLogMapper aiLogMapper;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Override
    public AiResponse approvalSummary(Approval approval) {
        String userPrompt = """
                请根据以下 OA 审批单生成一段审批摘要，突出申请类型、申请人、时间、金额、理由和需要审批人关注的点。
                审批单：
                %s
                """.formatted(toJson(approval));
        return execute(AiFunctionTypeEnum.APPROVAL_SUMMARY.name(), "审批摘要", approval, userPrompt, content -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("summary", content);
            return AiResponse.of(AiFunctionTypeEnum.APPROVAL_SUMMARY.name(), PROVIDER, content, data, 0);
        });
    }

    @Override
    public AiResponse approvalRiskAnalyze(Approval approval) {
        String userPrompt = """
                请分析以下 OA 审批单的风险，并只返回 JSON。
                JSON 字段：riskLevel，取值 LOW/MEDIUM/HIGH；reason，风险原因；suggestion，审批建议。
                审批单：
                %s
                """.formatted(toJson(approval));
        return execute(AiFunctionTypeEnum.APPROVAL_RISK.name(), "审批风险分析", approval, userPrompt, content -> {
            Map<String, Object> data = parseJsonObject(content);
            return AiResponse.of(AiFunctionTypeEnum.APPROVAL_RISK.name(), PROVIDER, textValue(data, "reason", content), data, 0);
        });
    }

    @Override
    public AiResponse generateNews(NewsGenerateRequest request) {
        int wordCount = request.getResolvedWordCount() == null ? 600 : request.getResolvedWordCount();
        String userPrompt = """
                请为企业 OA 系统生成一篇内部新闻，并只返回 JSON。
                JSON 字段：title，summary，content，category。
                主题：%s
                关键词：%s
                语气：%s
                字数：%d
                分类：%s
                """.formatted(nullSafe(request.getTopic()), nullSafe(request.getKeywords()), nullSafe(request.getTone()),
                wordCount, nullSafe(request.getCategory()));
        return execute(AiFunctionTypeEnum.NEWS_GENERATE.name(), "新闻生成", request, userPrompt, content -> {
            Map<String, Object> data = parseJsonObject(content);
            return AiResponse.of(AiFunctionTypeEnum.NEWS_GENERATE.name(), PROVIDER, textValue(data, "content", content), data, 0);
        });
    }

    @Override
    public AiResponse polishNews(NewsPolishRequest request) {
        String userPrompt = """
                请润色以下企业内部新闻，并只返回 JSON。
                JSON 字段：title，summary，content。
                风格：%s
                原标题：%s
                原正文：
                %s
                """.formatted(nullSafe(request.getStyle()), nullSafe(request.getTitle()), nullSafe(request.getContent()));
        return execute(AiFunctionTypeEnum.NEWS_POLISH.name(), "新闻润色", request, userPrompt, content -> {
            Map<String, Object> data = parseJsonObject(content);
            return AiResponse.of(AiFunctionTypeEnum.NEWS_POLISH.name(), PROVIDER, textValue(data, "content", content), data, 0);
        });
    }

    @Override
    public AiResponse parseSchedule(ScheduleParseRequest request) {
        String userPrompt = """
                请把自然语言日程解析成 JSON。当前时间：%s。
                JSON 字段：title，content，type，startTime，endTime，location，reminderMinutes。
                type 取值 MEETING/TASK/REMINDER/OTHER。
                startTime 和 endTime 使用 yyyy-MM-dd'T'HH:mm:ss。
                原文：%s
                """.formatted(LocalDateTime.now(), nullSafe(request.getText()));
        return execute(AiFunctionTypeEnum.SCHEDULE_PARSE.name(), "日程解析", request, userPrompt, content -> {
            Map<String, Object> data = parseJsonObject(content);
            return AiResponse.of(AiFunctionTypeEnum.SCHEDULE_PARSE.name(), PROVIDER, "日程已解析", data, 0);
        });
    }

    @Override
    public AiResponse qa(AiQaRequest request) {
        String userPrompt = """
                请以企业 OA 系统助手身份回答用户问题。回答要具体、简洁，并优先围绕审批、请假、报销、日程、新闻、通知和用户管理。
                用户问题：%s
                """.formatted(nullSafe(request.getQuestion()));
        return execute(AiFunctionTypeEnum.QA.name(), "智能问答", request, userPrompt, content -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("answer", content);
            return AiResponse.of(AiFunctionTypeEnum.QA.name(), PROVIDER, content, data, 0);
        });
    }

    @Override
    public AiResponse streamQa(AiQaRequest request, Consumer<String> onDelta) {
        String userPrompt = """
                请以企业 OA 系统助手身份回答用户问题。回答要具体、简洁，并优先围绕审批、请假、报销、日程、新闻、通知和用户管理。
                用户问题：%s
                """.formatted(nullSafe(request.getQuestion()));
        long start = System.currentTimeMillis();
        AiLog log = new AiLog();
        log.setUserId(resolveUserId());
        log.setFunctionType(AiFunctionTypeEnum.QA.name());
        log.setProvider(PROVIDER);
        log.setModelName(aiProperties.getTongyi().getModel());
        log.setPrompt("智能问答-流式");
        try {
            log.setRequestContent(AiLogSanitizer.request(toJson(request)));
            String content = chatStream(userPrompt, onDelta);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("answer", content);
            AiResponse response = AiResponse.of(AiFunctionTypeEnum.QA.name(), PROVIDER, content, data,
                    System.currentTimeMillis() - start);
            log.setResponseContent(AiLogSanitizer.response(toJson(response)));
            log.setSuccess(1);
            log.setCostTimeMs(response.getCostTimeMs());
            aiLogMapper.insert(log);
            return response;
        } catch (Exception e) {
            log.setSuccess(0);
            log.setErrorMessage(AiLogSanitizer.error(e.getMessage()));
            log.setCostTimeMs(System.currentTimeMillis() - start);
            aiLogMapper.insert(log);
            if (e instanceof BusinessException businessException) throw businessException;
            throw new BusinessException(500, "AI 流式调用失败：" + e.getMessage());
        }
    }

    private String chatStream(String userPrompt, Consumer<String> onDelta) throws IOException, InterruptedException {
        boolean[] emitted = {false};
        Consumer<String> guardedDelta = delta -> {
            emitted[0] = true;
            onDelta.accept(delta);
        };
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return chatStreamOnce(userPrompt, guardedDelta);
            } catch (IOException | BusinessException e) {
                boolean retryable = e instanceof IOException
                        || e instanceof BusinessException be && (be.getCode() == 429 || be.getCode() >= 500);
                if (attempt > 0 || emitted[0] || !retryable) throw e;
                Thread.sleep(500L);
            }
        }
        throw new IOException("AI stream retry exhausted");
    }

    private String chatStreamOnce(String userPrompt, Consumer<String> onDelta) throws IOException, InterruptedException {
        AiProperties.Tongyi tongyi = aiProperties.getTongyi();
        if (!StringUtils.hasText(tongyi.getApiKey())) throw new BusinessException("未配置通义千问 API Key");
        String url = normalizeBaseUrl(tongyi.getBaseUrl()) + "/chat/completions";
        Map<String, Object> requestBody = bodyMap(tongyi, userPrompt);
        requestBody.put("stream", true);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + tongyi.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody), StandardCharsets.UTF_8))
                .build();
        HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String error = response.body().limit(5).reduce("", (left, right) -> left + right);
            int code = response.statusCode() == 429 ? 429 : 502;
            throw new BusinessException(code, "通义千问流式调用失败：" + response.statusCode() + " " + summarizeError(error));
        }
        StringBuilder content = new StringBuilder();
        try (Stream<String> lines = response.body()) {
            lines.forEach(line -> {
                if (!line.startsWith("data:")) return;
                String json = line.substring(5).trim();
                if (json.isEmpty() || "[DONE]".equals(json)) return;
                try {
                    String delta = objectMapper.readTree(json).path("choices").path(0).path("delta").path("content").asText("");
                    if (!delta.isEmpty()) {
                        content.append(delta);
                        onDelta.accept(delta);
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("解析 AI 流式响应失败", e);
                }
            });
        }
        if (content.isEmpty()) throw new BusinessException(502, "通义千问没有返回有效内容");
        return content.toString();
    }

    private AiResponse execute(String functionType, String prompt, Object request, String userPrompt, Function<String, AiResponse> parser) {
        long start = System.currentTimeMillis();
        AiLog log = new AiLog();
        log.setUserId(resolveUserId());
        log.setFunctionType(functionType);
        log.setProvider(PROVIDER);
        log.setModelName(aiProperties.getTongyi().getModel());
        log.setPrompt(prompt);

        try {
            log.setRequestContent(AiLogSanitizer.request(toJson(request)));
            String content = chat(userPrompt);
            AiResponse response = parser.apply(content);
            response.setCostTimeMs(System.currentTimeMillis() - start);
            log.setResponseContent(AiLogSanitizer.response(toJson(response)));
            log.setSuccess(1);
            log.setCostTimeMs(response.getCostTimeMs());
            aiLogMapper.insert(log);
            return response;
        } catch (Exception e) {
            log.setSuccess(0);
            log.setErrorMessage(AiLogSanitizer.error(e.getMessage()));
            log.setCostTimeMs(System.currentTimeMillis() - start);
            aiLogMapper.insert(log);
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(500, "AI 调用失败：" + e.getMessage());
        }
    }

    private String chat(String userPrompt) throws IOException, InterruptedException {
        AiProperties.Tongyi tongyi = aiProperties.getTongyi();
        if (!StringUtils.hasText(tongyi.getApiKey())) {
            throw new BusinessException("未配置通义千问 API Key");
        }

        String url = normalizeBaseUrl(tongyi.getBaseUrl()) + "/chat/completions";
        String body = objectMapper.writeValueAsString(bodyMap(tongyi, userPrompt));
        HttpResponse<String> response;
        try {
            response = postWithJavaHttpClient(url, tongyi.getApiKey(), body);
        } catch (Exception e) {
            if (!Boolean.TRUE.equals(tongyi.getCurlFallback()) || !isHandshakeFailure(e)) {
                throw e;
            }
            response = postWithCurl(url, tongyi.getApiKey(), body);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BusinessException(500, "通义千问调用失败：" + response.statusCode() + " " + summarizeError(response.body()));
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(500, "通义千问没有返回有效内容");
        }
        return content.trim();
    }

    private Map<String, Object> bodyMap(AiProperties.Tongyi tongyi, String userPrompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", tongyi.getModel());
        body.put("temperature", tongyi.getTemperature());
        body.put("max_tokens", tongyi.getMaxTokens());
        body.put("messages", List.of(
                Map.of("role", "system", "content", "你是企业 OA 管理系统中的 AI 助手，回答必须准确、可执行、适合办公场景。"),
                Map.of("role", "user", "content", userPrompt)
        ));
        return body;
    }

    private HttpResponse<String> postWithJavaHttpClient(String url, String apiKey, String body) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> postWithCurl(String url, String apiKey, String body) throws IOException, InterruptedException {
        AiProperties.Tongyi tongyi = aiProperties.getTongyi();
        Path bodyFile = Files.createTempFile("oa-ai-request-", ".json");
        try {
            Files.writeString(bodyFile, escapeNonAscii(body), StandardCharsets.UTF_8);
            String bodyPath = bodyFile.toAbsolutePath().toString();
            Process process = new ProcessBuilder(
                    StringUtils.hasText(tongyi.getCurlCommand()) ? tongyi.getCurlCommand() : "curl.exe",
                    "-sS",
                    "-w",
                    "\n__HTTP_STATUS__:%{http_code}\n",
                    "-X",
                    "POST",
                    url,
                    "-H",
                    "Content-Type: application/json",
                    "-H",
                    "Authorization: Bearer " + apiKey,
                    "--data-binary",
                    "@" + bodyPath,
                    "--max-time",
                    "70"
            ).start();

            CompletableFuture<String> stdout = CompletableFuture.supplyAsync(() -> readProcessOutput(process.getInputStream()));
            CompletableFuture<String> stderr = CompletableFuture.supplyAsync(() -> readProcessOutput(process.getErrorStream()));
            boolean finished = process.waitFor(80, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException(500, "通义千问调用超时");
            }

            String output = stdout.join();
            String error = stderr.join();
            if (process.exitValue() != 0) {
                throw new BusinessException(500, "curl 调用通义千问失败：" + error);
            }
            return parseCurlResponse(output);
        } finally {
            Files.deleteIfExists(bodyFile);
        }
    }

    private String escapeNonAscii(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch <= 0x7F) {
                builder.append(ch);
            } else {
                builder.append(String.format("\\u%04x", (int) ch));
            }
        }
        return builder.toString();
    }

    private String readProcessOutput(java.io.InputStream inputStream) {
        try (inputStream) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private HttpResponse<String> parseCurlResponse(String output) {
        String marker = "\n__HTTP_STATUS__:";
        int index = output.lastIndexOf(marker);
        if (index < 0) {
            throw new BusinessException(500, "curl 调用通义千问未返回 HTTP 状态");
        }
        String body = output.substring(0, index).trim();
        String statusText = output.substring(index + marker.length()).trim();
        int status = Integer.parseInt(statusText);
        return new SimpleHttpResponse(status, body);
    }

    private boolean isHandshakeFailure(Exception e) {
        Throwable current = e;
        while (current != null) {
            String message = current.getMessage();
            if (current instanceof javax.net.ssl.SSLHandshakeException
                    || (message != null && message.contains("Remote host terminated the handshake"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private record SimpleHttpResponse(int statusCode, String body) implements HttpResponse<String> {

        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public java.util.Optional<HttpResponse<String>> previousResponse() {
            return java.util.Optional.empty();
        }

        @Override
        public java.net.http.HttpHeaders headers() {
            return java.net.http.HttpHeaders.of(Map.of(), (key, value) -> true);
        }

        @Override
        public java.util.Optional<javax.net.ssl.SSLSession> sslSession() {
            return java.util.Optional.empty();
        }

        @Override
        public URI uri() {
            return null;
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }

    private Map<String, Object> parseJsonObject(String content) {
        String json = content.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("content", content);
            return data;
        }
    }

    private String textValue(Map<String, Object> data, String key, String fallback) {
        Object value = data.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = StringUtils.hasText(baseUrl) ? baseUrl : "https://dashscope.aliyuncs.com/compatible-mode/v1";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String summarizeError(String body) {
        if (!StringUtils.hasText(body)) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode message = root.at("/error/message");
            return message.isMissingNode() ? body.substring(0, Math.min(body.length(), 300)) : message.asText();
        } catch (Exception e) {
            return body.substring(0, Math.min(body.length(), 300));
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private Long resolveUserId() {
        try {
            Long userId = SecurityUtils.currentUserId();
            return userId == null ? 0L : userId;
        } catch (Exception e) {
            return 0L;
        }
    }

    private String nullSafe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
