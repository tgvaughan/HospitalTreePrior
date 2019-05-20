package constrainedtree;

import beast.core.BEASTObject;
import beast.core.Input;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class PatientStateSequence extends BEASTObject {

    public Input<String> csvFileNameInput = new Input<>("csvFileName",
            "Name of CSV data file for input.",
            Input.Validate.REQUIRED);

    public Input<String> finalSampleDateInput = new Input<>("finalSampleDate",
            "Date of final sample, YYYY-MM-DD.",
            Input.Validate.REQUIRED);

    public Input<String> dateFormatStringInput = new Input<>("dateFormat",
            "Optional format string for dates, default is yyyy-MM-dd",
            "yyyy-MM-dd");

    public Input<PatientStateModel> stateModelInput = new Input<>("stateModel",
            "Patient state model.",
            Input.Validate.REQUIRED);

    private List<PatientStateChange> patientStateChanges;

    private PatientStateModel baseStateModel;

    private Map<String, List<PatientStateModel>> patientStates;
    private Map<String, List<Double>> patientStateChangeTimes;

    private Date finalSampleDate;
    private DateFormat dateFormat;

    @Override
    public void initAndValidate() {


        dateFormat = new SimpleDateFormat(dateFormatStringInput.get());

        try {
            finalSampleDate = dateFormat.parse(finalSampleDateInput.get());
        } catch (ParseException e) {
            throw new IllegalArgumentException("Error parsing finalSampleDate as a date string.");
        }

        baseStateModel = stateModelInput.get();

        patientStateChanges = new ArrayList<>();

        try (FileReader reader = new FileReader(csvFileNameInput.get())){
            CSVParser parser = CSVParser.parse(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());

            for (CSVRecord record : parser.getRecords()) {

                PatientStateChange.Type changeType =
                        record.get("ChangeType").equals("+")
                                ? PatientStateChange.Type.BEGIN
                                : PatientStateChange.Type.END;

                PatientStateChange change = new PatientStateChange(
                        record.get("PatientID"),
                        record.get("StateName"),
                        record.get("StateValue"),
                        getTimeFromDateString(record.get("Date")),
                        record.get("Date"),
                        changeType);

                patientStateChanges.add(change);
            }

            patientStateChanges.sort((o1, o2) -> {
                if (o1.time > o2.time)
                    return -1;
                if (o1.time < o2.time)
                    return 1;

                if (o1.type != o2.type) {
                    if (o1.type == PatientStateChange.Type.BEGIN)
                        return -1;
                    else
                        return 1;
                }

                return 0;
            });

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error reading patient state change file.");
        }

        // Initialize patient list:

        Set<String> patients =
                patientStateChanges.stream()
                        .map(sc -> sc.patientName)
                        .collect(Collectors.toSet());

        patientStates = new HashMap<>();
        for (String patient : patients) {
            patientStates.put(patient, new ArrayList<>());
        }

        // Add state changes

        for (PatientStateChange stateChange : patientStateChanges) {
           String patient = stateChange.patientName;
           double time = stateChange.time;
           List<Double> thisPatientChangeTimes = patientStateChangeTimes.get(patient);
           List<PatientStateModel> thisPatientStates = patientStates.get(patient);

           PatientStateModel patientStateModel;
           if (thisPatientChangeTimes.isEmpty())
               patientStateModel = baseStateModel.copy();
           else if (thisPatientChangeTimes.get(thisPatientChangeTimes.size()-1) == time)
               patientStateModel = thisPatientStates.get(thisPatientStates.size()-1);
           else
               patientStateModel = thisPatientStates.get(thisPatientStates.size()-1).copy();


        }

        System.out.println("Made it.");

    }

    public double getTimeFromDateString(String dateString) {
        try {
            Date thisDate = dateFormat.parse(dateString);
            return (double)(finalSampleDate.getTime() - thisDate.getTime())/(24*60*60*1000.0);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Error parsing date string " + dateString + ".");
        }

    }

    public Set<String> getPatientState(double time) {
        return null;
    }

    public double getNextStateChangeTime(double currentTime) {
        return 0;
    }

    public static void main(String[] args) {

        PatientStateModel model = new PatientStateModel(
                "Hospital",
                new PatientStateModel("Ward"));

        PatientStateSequence sequence = new PatientStateSequence();
        sequence.initByName("csvFileName", "examples/PatientStateChanges.csv",
                "finalSampleDate", "16/6/17",
                "dateFormat", "dd/M/yy",
                "stateModel", model);
    }
}
