package com.commerceweb.controller;

import com.commerceweb.dto.SalesStatDto;
import com.commerceweb.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000") // 리액트(프론트)에서 접근 허용
public class SalesController {

    private final SalesService salesService;

    // 웹브라우저에서 이 주소로 요청하면 통계 데이터를 보내줍니다.
    @GetMapping("/stats")
    public List<SalesStatDto> getSalesStats() {
        return salesService.getSalesStats();
    }
}