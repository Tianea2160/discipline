package org.project.discipline.controller

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class HelloController {

    @GetMapping("/hello")
    fun hello(@RequestParam(required = false) token: String?, model: Model): String {
        model.addAttribute("token", token)
        return "hello"
    }
}