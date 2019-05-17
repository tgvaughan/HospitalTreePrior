package constrainedtree;

public abstract class PatientStateChange {
    public String stateName;
    public double time;

    public enum Type {
        BEGIN, END
    }

    public Type type;

    public PatientStateChange(String stateName, double time, Type type) {
        this.stateName = stateName;
        this.time = time;
        this.type = type;
    }
}
