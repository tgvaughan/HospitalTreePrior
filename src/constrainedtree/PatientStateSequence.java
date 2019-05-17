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

    PatientStateModel stateModel;

    Map<String, Map<Double, Set<String>>> patientStateMap;
    List<PatientStateChange> patientStateChanges;
    List<Double> patientStateChangeTimes;

    Date finalSampleDate;
    DateFormat dateFormat;

    @Override
    public void initAndValidate() {

        dateFormat = new SimpleDateFormat(dateFormatStringInput.get());

        try {
            finalSampleDate = dateFormat.parse(finalSampleDateInput.get());
        } catch (ParseException e) {
            throw new IllegalArgumentException("Error parsing finalSampleDate as a date string.");
        }

        stateModel = stateModelInput.get();

        patientStateChanges = new ArrayList<>();

        try (FileReader reader = new FileReader(csvFileNameInput.get())){
            CSVParser parser = CSVParser.parse(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());

            for (CSVRecord record : parser.getRecords()) {

                String typeString = record.get("Event");
                PatientStateChange.Type type = null;
                switch(record.get("Event").toLowerCase()) {
                    case "admission":
                        type = PatientStateChange.Type.BEGIN;
                        break;
                    case "discharge":
                        type = PatientStateChange.Type.END;
                        break;
                    default:
                        break;
                }

                if (type == null)
                    continue;



                PatientStateChange change = new PatientStateChange(
                        record.get("Patient_ID"),
                        record.get("Hospital") + record.get("Status_Or_Ward"),
                        getTimeFromDateString(record.get("Date")),
                        type);

                patientStateChanges.add(change);
            }

            patientStateChanges.sort((o1, o2) -> {
                if (o1.time < o2.time)
                    return -1;
                if (o1.time > o2.time)
                    return 1;

                if (o1.type != o2.type) {
                    if (o1.type == PatientStateChange.Type.BEGIN)
                        return -1;
                    else
                        return 1;
                }

                return 0;
            });

                    System.out.println("Made it.");
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        PatientStateSequence model = new PatientStateSequence();
        model.initByName("csvFileName", "examples/PatientTimeline.csv",
                "finalSampleDate", "16/6/17",
                "dateFormat", "dd/M/yy");
    }
}
