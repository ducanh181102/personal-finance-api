package com.example.personalfinance.controller;

import com.example.personalfinance.dto.transfer.CreateTransferRequest;
import com.example.personalfinance.dto.transfer.TransferResponse;
import com.example.personalfinance.mapper.TransferMapper;
import com.example.personalfinance.model.Transfer;
import com.example.personalfinance.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {
    private final TransferService transferService;
    private final TransferMapper transferMapper;

    public TransferController(TransferService transferService, TransferMapper transferMapper) {
        this.transferService = transferService;
        this.transferMapper = transferMapper;
    }

    @GetMapping
    public List<TransferResponse> getAllTransfers() {
        List<Transfer> transferList = transferService.getAllTransfer();
        return transferList.stream().map(transferMapper::toTransferResponse).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getTransferById(@PathVariable("id") int id) {
        Transfer transfer = transferService.getTransferById(id);
        TransferResponse transferResponse = transferMapper.toTransferResponse(transfer);
        return ResponseEntity.ok(transferResponse);
    }

    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(@Valid @RequestBody CreateTransferRequest request) {
        Transfer transfer = transferService.createTransfer(request.getFromAccountId(), request.getToAccountId(),
                request.getAmount(),
                request.getTransferDate(),
                request.getNote());
        TransferResponse transferResponse = transferMapper.toTransferResponse(transfer);
        return ResponseEntity.status(HttpStatus.CREATED).body(transferResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransferById(@PathVariable("id") int id) {
        transferService.deleteTransferById(id);
        return ResponseEntity.noContent().build();
    }
}
