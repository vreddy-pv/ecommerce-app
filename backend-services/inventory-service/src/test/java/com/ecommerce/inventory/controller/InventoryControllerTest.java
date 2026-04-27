package com.ecommerce.inventory.controller;

import com.ecommerce.common.exception.InsufficientInventoryException;
import com.ecommerce.inventory.dto.StockDto;
import com.ecommerce.inventory.dto.ReserveRequest;
import com.ecommerce.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@Import({com.ecommerce.inventory.config.SecurityConfig.class,
         com.ecommerce.common.exception.GlobalExceptionHandler.class})
class InventoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private InventoryService inventoryService;

    @Test
    void getStockReturns200WithStockInfo() throws Exception {
        when(inventoryService.getStock(100L))
            .thenReturn(new StockDto(100L, 10, 3, 7, true));

        mockMvc.perform(get("/api/inventory/100/stock"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.productId").value(100))
            .andExpect(jsonPath("$.data.available").value(7))
            .andExpect(jsonPath("$.data.reserved").value(3));
    }

    @Test
    void reserveReturns202OnSuccess() throws Exception {
        doNothing().when(inventoryService).reserve(any());

        var req = new ReserveRequest(101L, List.of(new ReserveRequest.LineItem(100L, 3)));

        mockMvc.perform(post("/api/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isAccepted());
    }

    @Test
    void reserveReturns409WhenInsufficientStock() throws Exception {
        doThrow(new InsufficientInventoryException(100L, 5, 2))
            .when(inventoryService).reserve(any());

        var req = new ReserveRequest(101L, List.of(new ReserveRequest.LineItem(100L, 5)));

        mockMvc.perform(post("/api/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_INVENTORY"));
    }

    @Test
    void releaseReturns204() throws Exception {
        doNothing().when(inventoryService).release(101L);

        mockMvc.perform(put("/api/inventory/release/101"))
            .andExpect(status().isNoContent());
    }

    @Test
    void confirmReturns204() throws Exception {
        doNothing().when(inventoryService).confirm(101L);

        mockMvc.perform(put("/api/inventory/confirm/101"))
            .andExpect(status().isNoContent());
    }

    @Test
    void reserveReturns400WhenOrderIdMissing() throws Exception {
        mockMvc.perform(post("/api/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"items":[{"productId":100,"quantity":3}]}
                    """))
            .andExpect(status().isBadRequest());
    }
}
