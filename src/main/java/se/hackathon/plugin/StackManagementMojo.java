package se.hackathon.plugin;

import com.amazonaws.AmazonClientException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Created by ahadcse on 11/05/16.
 */
@Mojo(name = "createstack", defaultPhase = LifecyclePhase.INSTALL,
        configurator = "include-project-dependencies", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class StackManagementMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.directory}/apicf.json") // Will look in this directory for the project for which the plugin is imported.
    String inputFileName;

    @Parameter(defaultValue = "${project.artifactId}") // Will use the project name for cloud formation stack name
    String stackName;

    StackManagement stackManagement = new StackManagementImpl();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Stack management started ...");
        System.out.println(stackManagement.validateCfTemplate(inputFileName));
        getLog().info(stackManagement.validateCfTemplate(inputFileName));
        try {
            if (stackManagement.stackExists(stackName)) {
                getLog().info("Stack already exists ...");
                stackManagement.deleteStack(stackName);
                stackManagement.createStack(stackName, inputFileName);
                //stackManagement.updateStack(stackName);
            } else {
                getLog().info("Stack does not exist ...");
                stackManagement.createStack(stackName, inputFileName);
            }
        } catch (Exception e) {
            getLog().error("Stack creation failed ...");
            throw new AmazonClientException("Cannot create stack ...", e);
        }
    }
}
