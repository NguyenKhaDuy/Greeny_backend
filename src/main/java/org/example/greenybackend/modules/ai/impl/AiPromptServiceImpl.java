package org.example.greenybackend.modules.ai.impl;

import org.example.greenybackend.modules.ai.AiPromptService;
import org.example.greenybackend.modules.ai.AiContextResult;
import org.springframework.stereotype.Service;

@Service
public class AiPromptServiceImpl implements AiPromptService {

	public static final String EXTERNAL_SOURCE_NOTE = "Lưu ý: Thông tin này không thuộc dữ liệu chính thức của website Greeny.";

	@Override
	public String buildSystemPrompt(String customPrompt) {
	    String basePrompt = String.join("\n",
	            "Bạn là trợ lý AI của hệ thống bán cây Greeny.",
	            "DATABASE_CONTEXT là dữ liệu chính thức của website Greeny về sản phẩm, giá, biến thể, tồn kho, khuyến mãi và đơn hàng.",
	            "Ưu tiên DATABASE_CONTEXT khi câu hỏi liên quan trực tiếp đến website Greeny.",
	            "Được dùng kiến thức chung hoặc thông tin bên ngoài để trả lời khi database không có dữ liệu phù hợp.",
	            "Mọi thông tin không nằm trong DATABASE_CONTEXT phải được đánh dấu rõ bằng câu: \"" + EXTERNAL_SOURCE_NOTE + "\"",
	            "Không được tự bịa sản phẩm, giá, biến thể, tồn kho, khuyến mãi, chính sách hoặc thông tin đơn hàng của Greeny.",
	            "Trả lời thân thiện, ngắn gọn, dễ hiểu, ưu tiên tiếng Việt.",
	            "Với đơn hàng hoặc thông tin cá nhân, chỉ dùng dữ liệu đơn hàng của user hiện tại nếu DATABASE_CONTEXT có cung cấp."
	    );

        if (customPrompt == null || customPrompt.isBlank()) {
            return basePrompt;
        }
        return basePrompt + "\n\nCUSTOM_AI_SETTING:\n" + customPrompt.trim();
    }

    @Override
    public String buildUserPrompt(AiContextResult context, String question) {
        String databaseContext = context.hasDatabaseData()
                ? context.databaseContext()
                : "No matching database records were found for this question.";
        return "DATABASE_CONTEXT_STATUS: "
                + (context.hasDatabaseData() ? "MATCHED" : "NO_MATCH")
                + "\nDATABASE_CONTEXT:\n"
                + databaseContext
                + "\n\nUSER_QUESTION:\n\""
                + safe(question)
                + "\"";
    }

    @Override
    public String ensureExternalSourceNote(String content, AiContextResult context) {
        if (content == null || content.isBlank() || context.hasDatabaseData()) {
            return content;
        }
        if (content.contains(EXTERNAL_SOURCE_NOTE)) {
            return content;
        }
        return EXTERNAL_SOURCE_NOTE + "\n\n" + content.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }
}
