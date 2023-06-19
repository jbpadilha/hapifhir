import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import org.apache.commons.lang3.time.StopWatch;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;

import java.io.*;
import java.util.*;

public class SampleClient {

    private static String fileName = "lastNames.txt";

    public static String getFileName() {
        return fileName;
    }

    public static void printNamesBirth(Bundle bundle) {
        bundle.getEntry().sort(Comparator.comparing(entry -> ((Patient) entry.getResource()).getName().get(0).getGivenAsSingleString()));
        bundle.getEntry().forEach(b -> {
            Patient patient = (Patient) b.getResource();
            // Given names:
            System.out.println("Given Names:");
            for (int i = 0; i < patient.getName().size(); i++) {
                List<HumanName> humanName = patient.getName();
                System.out.println("- Family Name: "+ humanName.get(i).getFamily());
                patient.getName().get(i).getGiven().forEach(stringType -> System.out.print("- Name: "+ stringType));
                System.out.println("");
            }
            System.out.println("Birth Date: " + patient.getBirthDate());
            System.out.println("");
        });
    }

    public static void createTxtDistinctLasName(Bundle bundle) {
        Map<String, List<Patient>> lastNames = new HashMap<>();
        for (int i = 0; i < bundle.getEntry().size(); i++) {
            Patient patient = (Patient) bundle.getEntry().get(i).getResource();
            String lastName = patient.getName().get(0).getFamily();
            if (lastNames.containsKey(lastName)) {
                List patients = lastNames.get(lastName);
                patients.add(patient);
                lastNames.put(lastName, patients);
            } else {
                List<Patient> patients = new ArrayList<>();
                patients.add(patient);
                lastNames.put(lastName, patients);
            }
        }

        try {
            FileWriter fileLastName = new FileWriter(fileName);
            BufferedWriter bufferedWriter = new BufferedWriter(fileLastName);
            int count = 0;
            for (Map.Entry<String, List<Patient>> names : lastNames.entrySet()) {
                if (count == 20) {
                    break;
                }
                bufferedWriter.write(names.getKey());
                bufferedWriter.newLine(); // Add a new line after each last name
                count++;
            }
            bufferedWriter.close();
            System.out.println("File created successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void searchPrintFromFile(){
        // Read Txt File
        List<String> listNames = new ArrayList<>();
        try{
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                listNames.add(line);
            }

            bufferedReader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create a FHIR client
        IGenericClient client = getClient(null);
        client.registerInterceptor(new HapiInterceptor());

        // Search for Patient resources
        client.search()
            .forResource("Patient")
            .where(Patient.FAMILY.matches().values(listNames))
            .returnBundle(Bundle.class)
            .execute();
    }

    public static void loop3TimesCache(){
        // loop 3 times
        IGenericClient client = null;
        for (int i = 1; i<= 3; i++) {
            boolean disableCache = i == 3;

            if (disableCache) {
                client = getClient(0);
            } else {
                client = getClient(10000);
            }

            // Search for Patient resources
            List<Long> responseTimes = new ArrayList<>();
            for (int j = 0; j < 20; j++) {
                StopWatch stopwatch = StopWatch.createStarted();
                searchPrintFromFile();
                stopwatch.stop();
                responseTimes.add(stopwatch.getTime());
            }
            // Calculate average response time
            long totalResponseTime = 0;
            for (long responseTime : responseTimes) {
                totalResponseTime += responseTime;
            }
            long averageResponseTime = totalResponseTime / responseTimes.size();

            System.out.println("Average Response Time (Loop " + i + "): " + averageResponseTime + " ms");
        }

    }

    public static IGenericClient getClient (Integer socketTimeout) {
        // Create a FHIR client
        FhirContext fhirContext = FhirContext.forR4();
        IGenericClient client = fhirContext.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
        client.registerInterceptor(new LoggingInterceptor(false));
        if (socketTimeout != null) {
            fhirContext.getRestfulClientFactory().setSocketTimeout(socketTimeout);
        }
        return client;
    }

    public static void main(String[] theArgs) {

        IGenericClient client = getClient(null);
        // Search for Patient resources
        Bundle response = client
                .search()
                .forResource("Patient")
                .where(Patient.FAMILY.matches().value("SMITH"))
                .returnBundle(Bundle.class)
                .execute();


        printNamesBirth(response);
        createTxtDistinctLasName(response);
        searchPrintFromFile();
        loop3TimesCache();

    }

}
