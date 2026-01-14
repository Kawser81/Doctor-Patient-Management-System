package com.example.doctor_patient_management_system.service;

public interface MessageOutboxScheduler {

     void processPendingMessages();

     void retryFailedMessages();

}
