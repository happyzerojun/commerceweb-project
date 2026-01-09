package com.commerceweb.service;

import com.commerceweb.dto.SalesStatDto;
import com.commerceweb.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final OrderRepository orderRepository;

    // 리포지토리에서 계산한 통계 데이터를 가져와서 컨트롤러에게 전달합니다.
    public List<SalesStatDto> getSalesStats() {
        return orderRepository.getSalesStatistics();
    }
}