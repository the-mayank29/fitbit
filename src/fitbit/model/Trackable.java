package fitbit.model;

/**
 * Interface representing any trackable biometric or lifestyle event.
 * Demonstrates OOP Interface design.
 */
public interface Trackable {
    String getTimestamp();
    void setTimestamp(String timestamp);
    String getCategory();
    double getPrimaryMetricValue();
    String getSummary();
}
