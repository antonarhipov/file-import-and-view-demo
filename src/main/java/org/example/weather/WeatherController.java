package org.example.weather;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WeatherController {

    private static final int PAGE_SIZE = 50;
    private final WeatherReadingRepository repository;

    public WeatherController(WeatherReadingRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/")
    public String temperatures(@RequestParam(defaultValue = "1") int page, Model model) {
        long total = repository.count();
        long pageCount = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int currentPage = (int) Math.clamp(page, 1, pageCount);
        List<WeatherReading> readings = repository.findPage(PAGE_SIZE, (long) (currentPage - 1) * PAGE_SIZE);

        model.addAttribute("readings", readings);
        model.addAttribute("total", total);
        model.addAttribute("page", currentPage);
        model.addAttribute("pageCount", pageCount);
        return "temperatures";
    }
}
