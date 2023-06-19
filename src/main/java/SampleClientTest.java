import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SampleClientTest {

    private IGenericClient client;

    @Before
    public void setup() {
        // Create a FHIR client for testing
        FhirContext fhirContext = FhirContext.forR4();
        client = fhirContext.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
        client.registerInterceptor(new LoggingInterceptor(false));
    }


    private Patient createPatient(String givenName, String familyName) {
        Patient patient = new Patient();
        HumanName name = new HumanName();
        name.addGiven(givenName);
        name.setFamily(familyName);
        patient.addName(name);
        return patient;
    }
    public Bundle getCommonBundle() {
        // Create a Bundle with sample Patient resources
        Bundle bundle = new Bundle();
        Patient patient1 = createPatient("Joao", "Family 1");
        Patient patient2 = createPatient("Joao 1", "Family 2");
        Patient patient3 = createPatient("Emily", "Family 3");

        bundle.addEntry().setResource(patient1);
        bundle.addEntry().setResource(patient2);
        bundle.addEntry().setResource(patient3);

        return bundle;
    }

    @Test
    public void testPrintNamesBirth() {

        Bundle bundle = getCommonBundle();
        // Test the printNamesBirth method
        SampleClient.printNamesBirth(bundle);

        Assert.assertEquals(2, bundle.getEntry().size());

        // Assert the details of the patient with the name "Joao"
        Patient joaoPatient = (Patient) bundle.getEntry().get(0).getResource();
        Assert.assertEquals("Joao", joaoPatient.getNameFirstRep().getGivenAsSingleString());
        Assert.assertEquals("Family 1", joaoPatient.getNameFirstRep().getFamily());
    }

    private List<String> readLastNamesFromFile() {
        List<String> lastNames = new ArrayList<>();

        try {
            FileReader fileReader = new FileReader(SampleClient.getFileName());
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                lastNames.add(line);
            }

            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lastNames;
    }

    @Test
    public void testCreateTxtDistinctLasName() {
        // Create a Bundle with sample Patient resources
        Bundle bundle = getCommonBundle();

        // Invoke the createTxtDistinctLasName method
        SampleClient.createTxtDistinctLasName(bundle);

        // Read the created file and verify its content
        List<String> lastNames = readLastNamesFromFile();

        // Assertions
        Assert.assertEquals(3, lastNames.size()); // Assert the correct number of last names written to the file
        Assert.assertTrue(lastNames.contains("Family 1")); // Assert the presence of a specific last name in the file
        Assert.assertTrue(lastNames.contains("Family 2")); // Assert the presence of another last name in the file
    }

    private static List<String> readTestFile() {
        List<String> listNames = new ArrayList<>();

        try {
            FileReader fileReader = new FileReader(SampleClient.getFileName());
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                listNames.add(line);
            }

            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return listNames;
    }

    @Test
    public void testSearchPrintFromFile() {
        List<String> listNames = readTestFile();

        // Create a FHIR client
        IGenericClient client = SampleClient.getClient(null);
        client.registerInterceptor(new HapiInterceptor());

        // Search for Patient resources
        Bundle bundle = client.search()
                .forResource("Patient")
                .where(Patient.FAMILY.matches().values(listNames))
                .returnBundle(Bundle.class)
                .execute();

        // Assertions
        Assert.assertNotNull(bundle); // Assert that the bundle is not null

    }

}
