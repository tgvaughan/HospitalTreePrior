package constrainedtree;

public class PatientStateChange {
    public String patientName;
    public String state;
    public String stateValue;
    public double time;
    public String dateString;

    public enum Type {
        BEGIN, END
    }

    public Type type;

    public PatientStateChange(String patientName, String state, String stateValue,
                              double time, String dateString,
                              Type type) {
        this.patientName = patientName;
        this.state = state;
        this.stateValue = stateValue;
        this.time = time;
        this.dateString = dateString;
        this.type = type;
    }

    @Override
    public String toString() {
        return "Patient:" + patientName + " State:" + state
                + " Value:" + stateValue + " (" + type + " " + dateString + ") ";
    }
}
