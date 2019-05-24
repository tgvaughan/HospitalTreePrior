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
import beast.core.parameter.RealParameter;
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

    public Input<String> stateVariableNamesInput = new Input<>("patientStateVariables",
            "Comma-delimited list of state variable names.",
            Input.Validate.REQUIRED);

    public Input<RealParameter> coalescentRateInput = new Input<>("coalescentRate",
            "Coalescent rate parameter",
            Input.Validate.REQUIRED);

    public Input<RealParameter> effectSizesInput = new Input<>("effectSizes",
            "Parameter describing effect sizes for state variable " +
                    "contribution to transmission rates.",
            Input.Validate.REQUIRED);

    public Input<RealParameter> migrationRateScalar = new Input<>("migrationRateScalar",
            "Scalar migration rate.",
            Input.Validate.REQUIRED);

    private List<String> stateVariableNames;
    private int nStateVariables;

    private String[][][] stateTable;
    private double[] stateChangeTimes;

    private boolean[][][][] indicatorMatrices;

    private double[] intervals;

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

        List<PatientStateChange> patientStateChanges = new ArrayList<>();

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

        // Use unique patients to set names if they haven't already been set:

        if (typeTraitInput.get() == null && typesInput.get() == null) {
            List<String> patientNames = patientStateChanges.stream()
                    .map(c -> c.patientName).distinct().collect(Collectors.toList());
            typesInput.setValue(String.join(" ", patientNames), this);
        }

        super.initAndValidate();

        // Initialize state variable list

        stateVariableNames = Arrays.stream(stateVariableNamesInput.get().split(","))
                .map(String::trim).collect(Collectors.toList());

        nStateVariables = stateVariableNames.size();

        // Build multi-patient state table

        TreeSet<Double> changeTimes = new TreeSet<>();
        int nChangeTimes = patientStateChanges.stream()
                .map(c -> c.time).collect(Collectors.toSet()).size();

        stateTable = new String[nChangeTimes][getNrTypes()][nStateVariables];
        stateChangeTimes = new double[nChangeTimes];

        int idx = 0;
        Double prevTime = null;
        for (PatientStateChange change : patientStateChanges) {
            if (prevTime == null || change.time != prevTime) {
                if (prevTime != null)
                    idx += 1;

                stateChangeTimes[idx] = change.time;

                if (idx>0) {
                    for (int pIdx = 0; pIdx < getNrTypes(); pIdx++)
                        if (nStateVariables >= 0)
                            System.arraycopy(stateTable[idx - 1][pIdx],
                                    0, stateTable[idx][pIdx],
                                    0, nStateVariables);
                }

                prevTime = change.time;
            }

            int patientIdx = getStateIndex(change.patientName);
            int stateVariableIdx = stateVariableNames.indexOf(change.state);

            if (change.type == PatientStateChange.Type.ON)
                stateTable[idx][patientIdx][stateVariableIdx] = change.stateValue;
            else
                stateTable[idx][patientIdx][stateVariableIdx] = null;
        }

        // Reverse table times:

        for (int i=0; i<stateChangeTimes.length/2; i++) {
            double tmpTime = stateChangeTimes[i];
            stateChangeTimes[i] = stateChangeTimes[stateChangeTimes.length-1-i];
            stateChangeTimes[stateChangeTimes.length-1-i] = tmpTime;

            String[][] tmpRecord = stateTable[i];
            stateTable[i] = stateTable[stateTable.length-1-i];
            stateTable[stateTable.length-1-i] = tmpRecord;
        }

        // Build indicator matrices:

        indicatorMatrices = new boolean[nChangeTimes][nStateVariables][getNrTypes()][getNrTypes()];

        for (int tIdx=0; tIdx<nChangeTimes; tIdx++) {
            for (int svIdx=0; svIdx<nStateVariables; svIdx++) {
                for (int i=1; i<getNrTypes(); i++) {
                    for (int j=0; j<i; j++) {
                        indicatorMatrices[tIdx][svIdx][i][j] = stateTable[tIdx][i][svIdx] != null
                                && stateTable[tIdx][j][svIdx] != null
                                && stateTable[tIdx][i][svIdx].equals(stateTable[tIdx][j][svIdx]);
                        indicatorMatrices[tIdx][svIdx][j][i] = indicatorMatrices[tIdx][svIdx][i][j];
                    }
                }
            }
        }

        // Cache interval widths:

        intervals = new double[nChangeTimes];
        intervals[0] = stateChangeTimes[0];
        for (int tIdx=1; tIdx<nChangeTimes; tIdx++)
            intervals[tIdx] = stateChangeTimes[tIdx] - stateChangeTimes[tIdx-1];


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
                "patientStateVariables", "Network,Hospital,Ward",
                "coalescentRate", new RealParameter("1.0"),
                "effectSizes", new RealParameter("0 0 0"));
    }

    /*
     * Dynamics implementation
     */


    @Override
    public void recalculate() {

    }

    @Override
    public double getInterval(int i) {
        return intervals[i];
    }

    @Override
    public double[] getIntervals() {
        return intervals;
    }

    @Override
    public boolean intervalIsDirty(int i) {
        return true;
    }

    private double[] coalescentRates;

    @Override
    public double[] getCoalescentRate(int i) {
        if (coalescentRates == null)
            coalescentRates = new double[getNrTypes()];

        Arrays.fill(coalescentRates, coalescentRateInput.get().getValue());
        return coalescentRates;
    }

    private double[] migrationRates;

    @Override
    public double[] getBackwardsMigration(int i) {
        if (migrationRates == null)
            migrationRates = new double[getNrTypes()*getNrTypes()];

        for (int patient1=1; patient1<getNrTypes(); patient1++) {
            for (int patient2=0; patient2<patient1; patient2++) {
                double logFactor = 0.0;

                for (int svIdx=0; svIdx<nStateVariables; svIdx++) {
                    if (indicatorMatrices[i][svIdx][patient1][patient2])
                        logFactor += effectSizesInput.get().getValue(svIdx);
                }

                migrationRates[patient1*getNrTypes() + patient2] =
                        migrationRateScalar.get().getValue()*Math.exp(logFactor);
            }
        }

        return migrationRates;
    }

    @Override
    public int getEpochCount() {
        return stateChangeTimes.length;
    }
}
