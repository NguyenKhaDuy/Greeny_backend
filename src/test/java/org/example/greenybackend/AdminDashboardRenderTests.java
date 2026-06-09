package org.example.greenybackend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.example.greenybackend.modules.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
@AutoConfigureMockMvc
class AdminDashboardRenderTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Test
    void adminDashboardRenders() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/catalog"))
                .andReturn();

        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        org.assertj.core.api.Assertions.assertThat(html)
                .contains(
                        "data-inventory-filter-form",
                        "data-care-filter-form",
                        "data-article-filter-form",
                        "data-notification-filter-form",
                        "data-user-filter-form",
                        "data-review-filter-form",
                        "data-jump-user-orders",
                        "article-preview-panel"
                )
                .doesNotContain("Backend chưa có");
    }

    @Test
    void adminFilterApisAcceptCombinedFilters() throws Exception {
        String token = adminToken();
        List<String> paths = List.of(
                "/api/admin/inventory?plant=&sku=&categoryId=&status=all&minQuantity=0&maxQuantity=999999",
                "/api/admin/care-profiles?plant=&level=&light=&water=&humidity=",
                "/api/admin/articles?title=&slug=&status=stored&created=",
                "/api/admin/notifications?title=&message=&type=0&recipient=&readStatus=all&minRecipients=0",
                "/api/admin/reviews?product=&customer=&rating=&status=all&created=",
                "/api/admin/users?name=&email=&phone=&role=&status=1&created=",
                "/api/admin/plants?includeDeleted=true&title=&sku=&categoryId=&visibility=visible&variantState=all&toxicity=&petFriendly=&airPurifying=",
                "/api/admin/variants?plantId=&name=&sku=&categoryId=&stock=all&status=all&minPrice=0"
        );

        for (String path : paths) {
            mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void adminActionRoutesAreMapped() {
        String mappings = requestMappingHandlerMapping.getHandlerMethods().keySet().toString();

        org.assertj.core.api.Assertions.assertThat(mappings)
                .contains(
                        "/admin/inventory/{variantId}/visibility",
                        "/admin/care-profiles/{careId}/delete",
                        "/admin/notifications/{notificationId}/delete",
                        "/admin/reviews/{reviewId}/delete",
                        "/admin/users/{userId}/delete",
                        "/api/admin/inventory/{variantId}/visibility",
                        "/api/admin/notifications/{notificationId}",
                        "/api/admin/reviews/{reviewId}",
                        "/api/admin/articles/{articleId}"
                );
    }

    @Test
    void revenueExcelExportReturnsWorkbook() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin/revenue/export/excel")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        org.assertj.core.api.Assertions.assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .contains("bao-cao-doanh-thu.xlsx");
        org.assertj.core.api.Assertions.assertThat(content).hasSizeGreaterThan(100);
        org.assertj.core.api.Assertions.assertThat(content[0]).isEqualTo((byte) 'P');
        org.assertj.core.api.Assertions.assertThat(content[1]).isEqualTo((byte) 'K');
    }

    private String adminToken() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() != null && user.getRole() == 0)
                .filter(user -> user.getStatus() != null && user.getStatus() == 1)
                .map(user -> user.getReToken())
                .filter(value -> value != null && value.startsWith("AUTH:"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Can co admin token trong DB de render dashboard"));
    }
}
