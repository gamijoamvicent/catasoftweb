package devforge.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CombosReportController {
    @GetMapping("/reportes/combos")
    public String dashboardCombos() {
        return "reportes/combos";
    }
}
