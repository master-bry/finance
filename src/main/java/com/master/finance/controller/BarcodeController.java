package com.master.finance.controller;

import com.master.finance.service.BarcodeService;
import com.master.finance.service.DebtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/barcode")
public class BarcodeController {

    @Autowired
    private BarcodeService barcodeService;

    @Autowired
    private DebtService debtService;

    @GetMapping(value = "/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateQrCode(@RequestParam String content,
                                                  @RequestParam(defaultValue = "200") int size) {
        byte[] qrImage = barcodeService.generateQrCode(content, size, size);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qrImage);
    }

    @GetMapping(value = "/qr/debt/{debtId}/{paymentIndex}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateDebtQrCode(@PathVariable String debtId,
                                                      @PathVariable int paymentIndex,
                                                      @RequestParam(defaultValue = "140") int size,
                                                      HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + ":" + request.getServerPort() + request.getContextPath();
        String verificationUrl = baseUrl + "/verify/debt/" + debtId + "/" + paymentIndex;
        byte[] qrImage = barcodeService.generateQrCode(verificationUrl, size, size);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qrImage);
    }
}
