package com.elastic.support.diagnostics.commands;

import com.elastic.support.SystemProperties;
import com.elastic.support.diagnostics.DiagnosticContext;
import com.elastic.support.diagnostics.InputParams;

import java.io.Console;
import java.util.Scanner;
import java.util.Set;

public class InteractiveInputCmd extends AbstractDiagnosticCmd{

    public boolean execute(DiagnosticContext context) {


        InputParams inputs = context.getInputParams();

        // Nothing needed, move on...
        if( !( inputs.getHost().equals("") || inputs.isInteractive() )){
            return true;
        }

        // Otherwise prompt for the necessary info
        Scanner scanner = new Scanner(System.in);
        Console console = System.console();
        if (console == null){
            String msg = "Could not obtain a console instance to enter interactive mode - using scanner.";
            logger.error(msg);
            context.addMessage(msg);
        }

        String selectionPrompt = "Enter the number of your selection";

        System.out.println("");
        System.out.println("Entering Interactive Mode....");
        System.out.println("");
        System.out.println("You have not supplied a target host - in versions of Elasticsearch from 2.0 on this is required.  If you are running a 1.x version or have bound Elasticsearch to localhost you may bypass this step and the diagnostic will attempt to use localhost.  If no binding exists however, the diagnostic will fail.  Try with localhost and the default port 9200 or enter interactive mode?");
        System.out.println("");
        System.out.println("1 - Try with defaults localhost and port 9200");
        System.out.println("2 - Prompt for connection information");
        System.out.println("");
        System.out.println(selectionPrompt);
        int choice = getNumberInput(scanner);
        if (choice == -1 ){
            String msg = "A valid selection was not provided. Exiting program.";
            logger.error(msg);
            context.addMessage(msg);
            return false;
        }
        else if (choice == 1){
            inputs.setHost("localhost");
            return true;
        }

        System.out.println("");
        System.out.println("Please select a hostname or IP address from the list of network.");
        Set<String> inets = context.getHostIpList();
        for(String inet : inets){
            System.out.println("       " + inet);
        }
        System.out.println("Enter one of the host names or IP addresses from above.");
        String host = getStringInput(console, scanner, false);

        if(! isValidkHost(host, inets)){
            String msg = "The number of the host selected was not a valid choice from the list.  Exiting program.";
            logger.error(msg);
            context.addMessage(msg);
            return false;
        }

        inputs.setHost(host);

        System.out.println("");
        System.out.println("Select the port that the node is listening on or enter 9200 for the default:");
        choice = getNumberInput(scanner);
        if (choice == -1){
            String msg = "A valid numeric port value was not entered.  Exiting program.";
            logger.error(msg);
            context.addMessage(msg);
            return false;
        }

        inputs.setPort(choice);

        System.out.println("");
        System.out.println("1 - No authentication.");
        System.out.println("2 - Enter authentication info.");
        System.out.println(selectionPrompt);
        choice = getNumberInput(scanner);

        if(choice == -1){
            String msg = "Invalid selection for authentication.  Exiting program";
            logger.error(msg);
            context.addMessage(msg);
            return false;
        }
        else if(choice == 1){
            return true;
        }

        System.out.println("");
        System.out.println("username:");
        String username = getStringInput(console, scanner, false);
        inputs.setUsername(username);

        System.out.println("");
        System.out.println("password:");
        String password = getStringInput(console, scanner, true);
        System.out.println("");
        System.out.println("1 - No SSL.");
        System.out.println("2 - Use SSL");
        System.out.println(selectionPrompt);

        choice = getNumberInput(scanner);
        if(choice == -1){
            String msg = "Invalid selection for SSL.  Exiting program";
            logger.error(msg);
            context.addMessage(msg);
            return false;
        }
        else if(choice == 1){
            inputs.setIsSsl(false);
        }
        else if(choice == 2){
            inputs.setIsSsl(true);
        }

        return true;

    }

    private String getStringInput(Console console, Scanner scanner, boolean isPassword){

        String input;
        if (isPassword){
            if (console == null){
                input = scanner.next();
            }
            else{
                input = new String(console.readPassword());
            }

        }
        else {
            input = scanner.next();
        }

        if (input == null) { input = ""; }

        return input;
    }

    private int getNumberInput(Scanner scanner){

        int num;

        try{
            num = scanner.nextInt();
        }
        catch (Exception e){
            logger.error("Invalid numeric input", e);
            return - 1;
        }

       return num;
    }

    private boolean isValidkHost(String host, Set hosts){

        if (host.equals("")) { return false;}

        if ( hosts.contains(host) ) {
            return true;
        }
        else {
            return true;
        }
    }
}