/*
 * Copyright (C) 2019. Tim Vaughan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package constrainedtree;

import beast.core.Input;
import beast.mascot.dynamics.Dynamics;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class PatientStateDynamics extends Dynamics {

    public Input<String> csvFileNameInput = new Input<>("csvFileName",
            "Name of CSV data file for input.",
            Input.Validate.REQUIRED);

    public Input<String> finalSampleDateInput = new Input<>("finalSampleDate",
            "Date of final sample, YYYY-MM-DD.",
            Input.Validate.REQUIRED);

    public Input<String> dateFormatStringInput = new Input<>("dateFormat",
            "Optional format string for dates, default is yyyy-MM-dd",
            "yyyy-MM-dd");

    public Input<String> stateVariablesInput = new Input<>("patientStateVariables",
            "Comma-delimited list of state variable names.",
            Input.Validate.REQUIRED);

    private List<String> stateVariables;
    private List<PatientStateChange> patientStateChanges;
    private int nStates;

    private Map<String, List<String[]>> patientStates;
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

        patientStateChanges = new ArrayList<>();

        try (FileReader reader = new FileReader(csvFileNameInput.get())){
            CSVParser parser = CSVParser.parse(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());

            for (CSVRecord record : parser.getRecords()) {

                PatientStateChange.Type changeType =
                        record.get("ChangeType").equals("+")
                                ? PatientStateChange.Type.ON
                                : PatientStateChange.Type.OFF;

                PatientStateChange change = new PatientStateChange(
                        record.get("PatientID"),
                        record.get("StateName"),
                        record.get("StateValue"),
                        getTimeFromDateString(record.get("Date"), changeType),
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
                    if (o1.type == PatientStateChange.Type.OFF)
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

        stateVariables = Arrays.stream(stateVariablesInput.get().split(","))
                .map(String::trim).collect(Collectors.toList());

        nStates = stateVariables.size();

        patientStates = new HashMap<>();
        patientStateChangeTimes = new HashMap<>();
        for (String patient : patients) {
            patientStates.put(patient, new ArrayList<>());
            patientStateChangeTimes.put(patient, new ArrayList<>());
        }

        // Add state changes

        for (PatientStateChange stateChange : patientStateChanges) {

            String patient = stateChange.patientName.intern();
            double time = stateChange.time;
            List<Double> thisPatientChangeTimes = patientStateChangeTimes.get(patient);
            List<String[]> thisPatientStates = patientStates.get(patient);

            int nChanges = thisPatientChangeTimes.size();

           String[] patientState;
           if (nChanges>0) {
               if (thisPatientChangeTimes.get(nChanges-1) == time)
                   patientState = thisPatientStates.get(nChanges-1);
               else {
                   patientState = thisPatientStates.get(nChanges - 1).clone();
                   thisPatientStates.add(patientState);
                   thisPatientChangeTimes.add(time);
               }
           } else {
               patientState = new String[nStates];
               thisPatientStates.add(patientState);
               thisPatientChangeTimes.add(time);
           }

           int stateIdx = stateVariables.indexOf(stateChange.state);
           if (stateChange.type == PatientStateChange.Type.ON)
               patientState[stateIdx] = stateChange.stateValue;
           else
               patientState[stateIdx] = null;
        }

        System.out.println("Made it.");

    }

    public double getTimeFromDateString(String dateString, PatientStateChange.Type type) {

        try {
            Date thisDate = dateFormat.parse(dateString);
            return (double)(finalSampleDate.getTime() - thisDate.getTime()) / (24*60*60*1000.0)
                    - (type==PatientStateChange.Type.OFF ? 1.0 : 0.0);
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


        PatientStateDynamics dynamics = new PatientStateDynamics();
        dynamics.initByName("csvFileName", "examples/PatientStateChanges.csv",
                "finalSampleDate", "2017-6-16",
                "dateFormat", "yyyy-MM-dd",
                "patientStateVariables", "Network,Hospital,Ward");
    }

    /*
     * Dynamics implementation
     */


    @Override
    public void recalculate() {

    }

    @Override
    public double getInterval(int i) {
        return 0;
    }

    @Override
    public double[] getIntervals() {
        return new double[0];
    }

    @Override
    public boolean intervalIsDirty(int i) {
        return false;
    }

    @Override
    public double[] getCoalescentRate(int i) {
        return new double[0];
    }

    @Override
    public double[] getBackwardsMigration(int i) {
        return new double[0];
    }

    @Override
    public int getEpochCount() {
        return 0;
    }
}
