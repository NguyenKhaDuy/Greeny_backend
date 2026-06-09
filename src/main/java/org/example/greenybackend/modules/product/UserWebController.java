package org.example.greenybackend.modules.product;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserWebController {

    @GetMapping("/shop")
    public String shop() {
        return "user/shop";
    }
}
