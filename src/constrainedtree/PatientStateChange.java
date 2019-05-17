package constrainedtree;

public class PatientStateChange {
    public String patientName;
    public String stateName;
    public double time;

    public enum Type {
        BEGIN, END
    }

    public Type type;

    public PatientStateChange(String patientName, String stateName, double time, Type type) {
        this.patientName = patientName;
        this.stateName = stateName;
        this.time = time;
        this.type = type;
    }
}
