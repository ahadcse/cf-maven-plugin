package se.hackathon.plugin;

import com.amazonaws.auth.AWSCredentials;

/**
 * Created by ahadcse on 11/05/16.
 */
public interface StackManagement {
    AWSCredentials createCredential();
    void createStack(String stackName, String inputFileName);
    void deleteStack(String stackName);
    void updateStack(String stackName);
    String validateCfTemplate(String fileName);
    boolean stackExists(String stackName);
}
