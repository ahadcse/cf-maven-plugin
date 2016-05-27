package se.hackathon.plugin;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Scanner;

/**
 * Created by ahadcse on 11/05/16.
 */
public class StackManagementImpl implements StackManagement {

    private AmazonCloudFormation stackBuilder = new AmazonCloudFormationClient(createCredential());
    private Region awsRegion = Region.getRegion(Regions.fromName(System.getProperty("awsRegion")));

    @Override
    public void createStack(String stackName, String inputFileName) {
        try {
            stackBuilder.setRegion(awsRegion);
            CreateStackRequest createRequest = new CreateStackRequest();
            createRequest.setStackName(stackName);
            createRequest.setTemplateBody(new Scanner(new File(inputFileName)).useDelimiter("\\Z").next());
            System.out.println("Creating a stack: " + createRequest.getStackName() + ".");
            stackBuilder.createStack(createRequest);
            System.out.println("Stack creation completed, the stack name: " + stackName + " completed with " + waitForCompletion(stackName));
        } catch (Exception e) {
            e.printStackTrace();
            throw new AmazonClientException("Cannot create stack: " + stackName, e);
        }
    }

    @Override
    public String validateCfTemplate(String fileDirectory) {
        ValidateTemplateRequest validateTemplateRequest = new ValidateTemplateRequest();
        validateTemplateRequest.setTemplateBody(readFile(fileDirectory));
        String s = stackBuilder.validateTemplate(validateTemplateRequest).getDescription();
        // System.out.println(s);
        return s;
    }

    private String readFile(String file) {
        String s = null;
        try {
            File ff = new File(file);
            FileReader fr = new FileReader(ff);
            BufferedReader br = new BufferedReader(new FileReader(file));
            while (br.read() != -1) {
                s = br.readLine();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return s;
    }

    @Override
    public void updateStack(String stackName) {
        try {
            UpdateStackRequest updateStackRequest = new UpdateStackRequest();
            updateStackRequest.setStackName(stackName);
            System.out.println("Updating the stack: " + updateStackRequest.getStackName() + ".");
            stackBuilder.updateStack(updateStackRequest);
            System.out.println("Stack updated completed, the stack name: " + stackName + " completed with " + waitForCompletion(stackName));
        } catch (Exception e) {
            e.printStackTrace();
            throw new AmazonClientException("Cannot update the stack: " + stackName, e);
        }
    }

    @Override
    public void deleteStack(String stackName) {
        try {
            DeleteStackRequest deleteRequest = new DeleteStackRequest();
            deleteRequest.setStackName(stackName);
            System.out.println("Deleting the stack: " + deleteRequest.getStackName() + ".");
            stackBuilder.deleteStack(deleteRequest);
            int timeout = 0;
            while (stackExists(stackName)) {
                Thread.sleep(timeout);
                timeout = timeout + 10000;
                if (timeout >= 60000)
                    throw new AmazonClientException("Cannot delete the stack: " + stackName + " within " + (timeout / 1000) + " seconds");
            }
            System.out.println("Stack deletion completed for the stack: " + stackName);
        } catch (Exception e) {
            throw new AmazonClientException("Cannot delete the stack: " + stackName, e);
        }
    }

    @Override
    public boolean stackExists(String stackName) {
        for (Stack stack : stackBuilder.describeStacks(new DescribeStacksRequest()).getStacks())
            if (stack.getStackName().equals(stackName)) return true;
        return false;
    }

    @Override
    public AWSCredentials createCredential() {
        AWSCredentials credentials;
        try {
            credentials = new DefaultAWSCredentialsProviderChain().getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.",
                    e);
        }
        return credentials;
    }

    // Wait for a stack to complete transitioning
    // End stack states are:
    //    CREATE_COMPLETE
    //    CREATE_FAILED
    //    DELETE_FAILED
    //    ROLLBACK_FAILED
    // OR the stack no longer exists
    private String waitForCompletion(String stackName) throws Exception {

        DescribeStacksRequest wait = new DescribeStacksRequest();
        wait.setStackName(stackName);
        Boolean completed = false;
        String stackStatus = "Unknown";
        String stackReason = "";

        System.out.print("Waiting");

        while (!completed) {
            List<Stack> stacks = stackBuilder.describeStacks(wait).getStacks();
            if (stacks.isEmpty()) {
                completed = true;
                stackStatus = "NO_SUCH_STACK";
                stackReason = "Stack has been deleted";
            } else {
                for (Stack stack : stacks) {
                    if (stack.getStackStatus().equals(StackStatus.CREATE_COMPLETE.toString()) ||
                            stack.getStackStatus().equals(StackStatus.CREATE_FAILED.toString()) ||
                            stack.getStackStatus().equals(StackStatus.ROLLBACK_FAILED.toString()) ||
                            stack.getStackStatus().equals(StackStatus.DELETE_FAILED.toString())) {
                        completed = true;
                        stackStatus = stack.getStackStatus();
                        stackReason = stack.getStackStatusReason();
                    }
                }
            }
            // Show we are waiting
            System.out.print(".");

            // Not done yet so sleep for 10 seconds.
            int timeout = 10000;
            if (!completed) {
                Thread.sleep(timeout);
                timeout = timeout + 10000;
                if (timeout >= 60000)
                    throw new AmazonClientException("Cannot create stack: " + stackName + " within " + (timeout / 1000) + " seconds");
            }
        }
        // Show we are done
        System.out.print("stack management done\n");
        return stackStatus + " (" + stackReason + ")";
    }
}
