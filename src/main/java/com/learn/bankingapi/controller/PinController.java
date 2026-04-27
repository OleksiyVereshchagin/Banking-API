package com.learn.bankingapi.controller;
import com.learn.bankingapi.dto.request.pin.ChangePinRequest;
import com.learn.bankingapi.dto.request.pin.SetPinRequest;
import com.learn.bankingapi.service.PinService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PinController {
    private final PinService pinService;

    public PinController(PinService pinService) {
        this.pinService = pinService;
    }

    @PostMapping("/cards/{id}/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPin(@Valid @RequestBody SetPinRequest request, @PathVariable long id){
        pinService.setPinForCard(request, id);
    }

    @PatchMapping("/cards/{id}/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePin(@Valid @RequestBody ChangePinRequest request, @PathVariable long id){
        pinService.changePinForCard(request, id);
    }
}
