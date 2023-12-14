package com.fastcampuspay.banking.application.port.in;


import com.fastcampuspay.banking.domain.RegisteredBankAccount;

public interface RegisterBankAccountUseCase {

    // BankAccount 등록하는 Use Case
    RegisteredBankAccount registerBankAccount(RegisterBankAccountCommand command);
}
